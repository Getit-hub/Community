package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageServce;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolders;
import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import javax.print.DocFlavor;
import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageServce messageServce;

    @Autowired
    private HostHolders hostHolders;

    @Autowired
    private UserService userService;

    //获取当前未读私信的id集合 (以便将未读变成已读)
    private List<Integer> getLetterIds(List<Message> letters){
        List<Integer> ids = new ArrayList<>();
        if(letters!=null){
            for (Message message:letters) {
                //如果发给自己的私信未读 就把此私信的id加入到ids集合中
                if(hostHolders.getUser().getId() == message.getToId() && message.getStatus() == 0 ){
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    //发送私信
    @RequestMapping(path = "/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content){
        User target = userService.findUserByName(toName);
        if(target == null){
            return CommunityUtil.getJSONString(1, "目标用户不存在！");
        }

        Message message = new Message();
        message.setFromId(hostHolders.getUser().getId());
        message.setToId(target.getId());
        //比如发送者用户A的id是12，接收者用户B的id是35 那么12_35就是会话id
        //当发送者用户B的id是35，接收者A的id是12，那么会话id依然是12_35 而不是35_12
        //这样可以保证两个人只能有一个会话，也只能有一个会话id 不管是A发给B 还是B发给A 消息所属的会话都是同一个
        if(message.getFromId()<message.getToId()){
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        } else {
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }

        message.setContent(content);
        message.setCreateTime(new Date());
        messageServce.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }


    //私信列表
    @RequestMapping(path = "/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){

        User user = hostHolders.getUser();

        //分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageServce.findConversationCount(user.getId()));

        //会话列表
        List<Message> conversations = messageServce.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversationsVo = new ArrayList<>();

        if(conversations!=null){
            for (Message message:conversations) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                //获取每个会话的消息数量
                map.put("letterCount", messageServce.findLetterCount(message.getConversationId()));
                //获取每个会话未读的消息数量
                map.put("unreadCount", messageServce.findLetterUnreadCount(user.getId(), message.getConversationId()));
                //显示对方的头像 需要先获取对方用户
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getToId();
                map.put("target", userService.findUserById(targetId));

                conversationsVo.add(map);
            }
        }
        model.addAttribute("conversationsVo", conversationsVo);
        //查询用户的未读消息数量
        int letterUnreadCount = messageServce.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        //查询未读通知的总数量
        int noticeUnreadCount = messageServce.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    //获取某一个会话的私信列表
    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model){
        //分页信息
        page.setLimit(5);
        page.setRows(messageServce.findLetterCount(conversationId));
        page.setPath("/letter/detail/"+conversationId);

        //私信列表
        List<Message> letters = messageServce.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> lettersOv = new ArrayList<>();
        if(letters!=null){
            for(Message message:letters){
                Map<String, Object> map = new HashMap<>();
                map.put("letter",message);
                //获取每条消息的发送者  因为页面上显示的是发起者的头像昵称以及消息内容 发送者可以是自己 也可以对方
                map.put("fromUser", userService.findUserById(message.getFromId()));
                lettersOv.add(map);
            }
        }
        model.addAttribute("lettersOv", lettersOv);

        //私信会话的目标对象
        model.addAttribute("target", getLetterTarget(conversationId));

        //将未读的私信设为已读
        //1 先获取未读私信的id集合
        List<Integer> ids = getLetterIds(letters);
        if(!ids.isEmpty()){
            // 2设为已读
            messageServce.readMessage(ids);
        }
        return "/site/letter-detail";

    }

    //获取私信对象
    private User getLetterTarget(String conversationId){
        //conversationId="111_112"代表用户111发起的与112的会话
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);//发起者的id
        int id1 = Integer.parseInt(ids[1]);//接收者的id

        if (hostHolders.getUser().getId()==id0){//如果自身用户是发起者，则返回接收者用户
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }

    //通知列表
    @RequestMapping(path = "/notice/list",method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolders.getUser();
        //查询评论类通知,通知页只显示一条最新的评论
        Message message = messageServce.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageVo = new HashMap<>();
        if(message!=null){
            messageVo.put("message", message);
            //下面的操作是使得到的content没有转义字符了 正常显示
            // （因为插入数据库中的内容都是经过转义的，从数据库中查找后直接显示在页面中的）
            //我们现在需要得到的是没有转义的内容
            String content = HtmlUtils.htmlUnescape(message.getContent());
            //将content转化成map类型的对象
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            Integer userId = Integer.parseInt(String.valueOf(data.get("userId"))) ;
            messageVo.put("user", userService.findUserById(userId ));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));

            int count = messageServce.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count", count);

            int unread = messageServce.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unread", unread);
        }
            model.addAttribute("commentNotice",messageVo);

        //查询点赞类通知
        message = messageServce.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVo = new HashMap<>();
        if(message!=null){
            messageVo.put("message", message);
            //下面的操作是使得到的content没有转义字符了 正常显示（因为原本的内容中经过了转义，以便从数据库中查找后直接显示在页面中的）
            //我们现在需要得到的是没有转义的内容
            String content = HtmlUtils.htmlUnescape(message.getContent());
            //将content转化成map类型的对象
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            /**
             *  如下 为啥从content中得到下面的数据??????
             *  想了好久才明白 消息是从事件队列中取出来的 在 EventConsumer设置的获取事件的逻辑是
             *  先取出事件后，然后事从件中取出触发事件的userId、事件的被触发者的实体类型和id,以及其他所有的key-value存放到
             *  通知消息字段content中，而postId是从评论事件的触发者 添加到事件中的，消费者取出事件时也会取出该属性。
             *  通知的内容实际上就包含这些属性。
             */
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));
            int count = messageServce.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("count", count);
            int unread = messageServce.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unread", unread);
        }
            model.addAttribute("likeNotice",messageVo);
        //查询关注类通知
        message = messageServce.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVo = new HashMap<>();
        if(message != null){
            messageVo.put("message", message);
            //下面的操作是使得到的content没有转义字符了 正常显示（因为原本的内容中经过了转义，以便从数据库中查找后直接显示在页面中的）
            //我们现在需要得到的是没有转义的通知内容
            String content = HtmlUtils.htmlUnescape(message.getContent());
            //将content转化成map类型的对象
            //这样做可能是为了方便携带数据到前端？不然这些信息怎么传到前端？
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));
            int count = messageServce.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count", count);
            int unread = messageServce.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unread", unread);
        }
            model.addAttribute("followNotice",messageVo);

        //查询未读私信的总数量（当前页面不仅需要显示未读通知，也需要显示未读私信的数量）
        int letterUnreadCount = messageServce.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        //查询未读通知的总数量
        int noticeUnreadCount = messageServce.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "/site/notice";
    }

    //某个主题下的所有通知
    @RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model){
        User user = hostHolders.getUser();

        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageServce.findNoticeCount(user.getId(),topic));

        List<Message> noticeList = messageServce.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if(noticeList!=null){
            for (Message notice:noticeList){
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);
        //model.addAttribute("topic", topic);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()){
            messageServce.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
