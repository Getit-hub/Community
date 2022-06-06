package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolders hostHolders;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    //置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id, 1);
        //帖子
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolders.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id, 1);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolders.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //删除
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id, 2);
        //触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolders.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user = hostHolders.getUser();
        if (user==null){
            return CommunityUtil.getJSONString(403,"你还没有登录哦!");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());

        eventProducer.fireEvent(event);

        //报错的情况，将来统一处理。
        return CommunityUtil.getJSONString(0,"发布成功");

    }

    //帖子详情页
    @RequestMapping(path = "detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子信息
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);

        //作者信息
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        //点赞信息 数量和状态
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("likeCount", likeCount);

        //判断当前用户的点赞状态
        int likeStatus = hostHolders.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolders.getUser().getId(),ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());
        //获取评论列表(包括帖子评论和评论回复))
        List<Comment> commentList = commentService.findCommentByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //对每一个评论，映射成 用户和评论 的形式
        List<Map<String, Object>> commentVolist = new ArrayList<>();

        if(commentVolist!=null){
            for (Comment comment:commentList) {
                //评论视图
                Map<String, Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment", comment);
                //作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());

                commentVo.put("likeCount", likeCount);
                //点赞状态
                likeStatus = hostHolders.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolders.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                //评论回复列表
                List<Comment> replyList = commentService.findCommentByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);

                List<Map<String, Object>> replyVoList = new ArrayList<>();

                if(replyList!=null){
                    // 回复列表视图
                    for (Comment reply: replyList) {
                        Map<String, Object> replyOv = new HashMap<>();
                        //回复
                        replyOv.put("reply", reply);
                        //作者
                        replyOv.put("user", userService.findUserById(reply.getUserId()));
                        //回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyOv.put("target", target);

                        //回复点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyOv.put("likeCount", likeCount);

                        //回复点赞状态
                        likeStatus = hostHolders.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolders.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyOv.put("likeStatus", likeStatus);

                        replyVoList.add(replyOv);
                    }
                }

                //回复列表放到评论列表中
                commentVo.put("replys", replyVoList);

                //回复的数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);

                //评论vo添加到评论集合中
                commentVolist.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVolist);
        return "/site/discuss-detail";
    }
}
