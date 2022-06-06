package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTest {

    @Autowired
    private MailClient mailClient;

    @Autowired
    TemplateEngine templateEngine;

    @Test
    public void testMail(){
        mailClient.sendMail("swwzhongbao@163.com","TEST","Welcome to Email");
    }

    @Test
    public void testHtmlMail(){
        //这里的Context是thymeleaf的Context
        Context context = new Context();
        context.setVariable("username","sww");

        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);

        mailClient.sendMail("swwzhongbao@163.com","TEST_HTML",content);

    }

}
