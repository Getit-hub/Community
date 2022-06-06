package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolders;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolders hostHolders;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    //个人主页 点击头像进入个人主页
    @RequestMapping(path="/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId")int userId,Model model){
        User user = userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        //用户
        model.addAttribute("user", user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeUserCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeUserCount", followeeUserCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //是否被当前登录用户已关注
        boolean hasFollowed = false;
        if (hostHolders.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolders.getUser().getId(), ENTITY_TYPE_USER, userId);
        }

        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    @LoginRequired
    @RequestMapping(path = "/posts/{userId}",method = RequestMethod.GET)
    public String getPosts(@PathVariable("userId")int userId, Model model, Page page){

        User user = userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("请求参数错误！");
        }
        model.addAttribute("user", user);

        page.setPath("/user/posts/"+userId);
        page.setLimit(5);
        int postsCount = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postsCount", postsCount);
        page.setRows(postsCount);

        //帖子
        List<DiscussPost> posts = discussPostService.findDiscussPosts(userId,page.getOffset(),page.getLimit());

        //帖子视图
        List<Map<String,Object>> postsVo= new ArrayList<>();
        if (posts!=null){
            for (DiscussPost post:posts) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                int likeCount = (int) likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                postsVo.add(map);
            }
        }
        model.addAttribute("postsVo", postsVo);
        return "site/my-post";
    }


    @LoginRequired
    @RequestMapping(path = "/comments/{userId}",method = RequestMethod.GET)
    public String getComments(@PathVariable("userId")int userId, Model model, Page page){

        User user = userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("请求参数错误！");
        }

        model.addAttribute("user", user);
        page.setPath("/user/comments/"+userId);
        page.setLimit(10);
        //查询某个用户的评论数(包含回复)
        int commentCount = commentService.findCommentCountByUserId(userId);
        model.addAttribute("commentCount",commentCount );
        page.setRows(commentCount);

        //回复集合
        List<Comment> comments = commentService.findCommentsByUserId(userId,page.getOffset(),page.getLimit());
        //回复集合视图
        List<Map<String,Object>> commentsVo= new ArrayList<>();
        if (comments!=null){
            for (Comment comment:comments) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                if (comment.getEntityType()==ENTITY_TYPE_POST){
                    DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                    map.put("post", post);
                }
                if (comment.getEntityType()==ENTITY_TYPE_COMMENT){
                    Comment comment1 = commentService.findCommentById(comment.getEntityId());
                    map.put("comment1", comment1);
                }
                commentsVo.add(map);
            }
            model.addAttribute("commentsVo", commentsVo);
        }
        return "site/my-reply";
    }

    @LoginRequired
    @RequestMapping(path="/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping("/updatePassword")
    public String updatePassword(Model model,
            @Param("oldPassword") String oldPassword,
            @Param("newPassword") String newPassword,
            @Param("confirmPassword") String confirmPassword){
        //输入框的密码不能为空
        if(StringUtils.isBlank(oldPassword) ){
            model.addAttribute("oldPasswordMsg", "旧密码不能为空！请重新输入！");
            return  "/site/setting";
        }

        if(StringUtils.isBlank(newPassword) ){
            model.addAttribute("newPasswordMsg", "新密码不能为空！请重新输入！");
            return  "/site/setting";
        }

        if(StringUtils.isBlank(confirmPassword) ){
            model.addAttribute("confirmPasswordMsg", "确认密码不能为空！请重新输入！");
            return  "/site/setting";
        }

        //确认密码要和新密码相同
        if(!newPassword.equals(confirmPassword)){
            model.addAttribute("confirmPasswordMsg", "确认密码和新密码不一致！请重新输入");
            //将用户以及输入的错误密码也送到页面表单上,使用户可以直接改，不用重复输入。
            model.addAttribute("oldPassword",oldPassword);
            model.addAttribute("newPassword",newPassword);
            model.addAttribute("confirmPassword", confirmPassword);
            return "/site/setting";
        }

        //验证用户输入的旧密码是否正确
        User user = hostHolders.getUser();


        if(!user.getPassword().equals(CommunityUtil.md5(oldPassword+user.getSalt())) ){
            model.addAttribute("oldPasswordMsg", "旧密码错误！请重新输入");
            //将用户以及输入的错误密码也送到页面表单上,使用户可以直接改，不用重复输入。
            model.addAttribute("oldPassword",oldPassword);
            model.addAttribute("newPassword",newPassword);
            model.addAttribute("confirmPassword", confirmPassword);
            return "/site/setting";
        }
        userService.updatePassword(user.getId(), CommunityUtil.md5(newPassword+user.getSalt()));
        return "redirect:/index";
    }

    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public  String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件格式不正确");
            return "/site/setting";
        }

        //生成随机的文件名
        filename = CommunityUtil.generateUUID() + suffix;
        //确定文件存放的路径
        File dest = new File(uploadPath + "/" + filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败："+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        //更新当前用户的头像的路径(web访问路径)
        //http://localhost:80/community/user/header/xxx.png
        User user=hostHolders.getUser();
        String headerUrl = domain + contextPath + "user/header/" + filename;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //服务器存放路径
        filename = uploadPath + "/" + filename;
        //文件后缀
        String suffix = filename.substring(filename.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        try (
                //从本地服务器中获取图片文件，以字节流的形式传入当前程序
                FileInputStream fis =new FileInputStream(filename);
                //获取输出流，将文件传到浏览器
                OutputStream os = response.getOutputStream();
        ) {
             byte[] buffer = new byte[1024];
             int b=0;
             while((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
             }
        } catch (IOException e) {
            logger.error("读取头像失败："+e.getMessage());
        }
    }

}
