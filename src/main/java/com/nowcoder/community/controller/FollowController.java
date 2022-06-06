package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant{

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolders hostHolders;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;



    @RequestMapping(path = "/follow",method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId){

        User user = hostHolders.getUser();

        followService.follow(user.getId(),entityType,entityId);


        //触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注！");
    }

    @RequestMapping(path = "/unfollow",method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId){

        User user = hostHolders.getUser();

        followService.unfollow(user.getId(),entityType,entityId);

        return CommunityUtil.getJSONString(0, "已取消关注！");
    }

    //访问关注列表
    @RequestMapping(path = "/followees/{userId}",method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Model model, Page page){
        User user = userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/"+userId);
        //总条数
        page.setRows((int) followService.findFolloweeCount(userId,ENTITY_TYPE_USER));

        //得到关注列表
        List<Map<String, Object>> userList = followService.findFollowees(userId,page.getOffset(),page.getLimit());

        //如果关注列表不为空
        if (userList!=null){
            for (Map<String,Object> map : userList){
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/followee";
    }

    //访问粉丝列表
    @RequestMapping(path = "/followers/{userId}",method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Model model, Page page){
        User user = userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER,userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId,page.getOffset(),page.getLimit());

        if (userList!=null){
            for (Map<String,Object> map : userList){
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/follower";
    }





    //判断当前登陆用户是否关注了该用户
    private boolean hasFollowed(int userId){
        if(hostHolders.getUser()==null){
            return false;
        }
        return followService.hasFollowed(hostHolders.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
