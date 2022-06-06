package com.nowcoder.community;

import com.nowcoder.community.dao.*;
import com.nowcoder.community.entity.*;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void updateUser() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);

        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
        System.out.println(rows);

        rows = userMapper.updatePassword(150, "hello");
        System.out.println(rows);
    }

   //修改密码
    @Test
    public void updateUserPassword(){
        User user=userMapper.selectById(166);
        String password=CommunityUtil.md5("111" + user.getSalt());
        userMapper.updatePassword(166,password);
    }

    @Test
    public void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149, 0, 10);
        for(DiscussPost post : list) {
            System.out.println(post);
        }
        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

    @Test
    public void  testInsetLoginTicket(){
        LoginTicket lt=new LoginTicket();
        lt.setUserId(101);
        lt.setTicket("abc");
        lt.setStatus(0);
        lt.setExpired(new Date(System.currentTimeMillis()+1000*60*10));

        loginTicketMapper.insertLoginTicket(lt);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("abc",1);
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectDiscussPostById(){
        DiscussPost post = discussPostMapper.selectDiscussPostById(166);
        System.out.println(post);
    }

    @Test
    public void testSelectLetters(){
        List<Message> conversations = messageMapper.selectConversations(111, 0, 20);
        for (Message message:conversations) {
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        List<Message> messages = messageMapper.selectLetters("111_112", 0, 10);
        for (Message message:messages){
            System.out.println(message);
        }

        int i = messageMapper.selectLetterCount("111_112");
        System.out.println(i);

        int i1 = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println(i1);

        System.out.println(messageMapper.selectLetterUnreadCount(131,null));
    }


}
