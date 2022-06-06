package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private  String contextPath;

    @RequestMapping(path = "/register",method = RequestMethod.GET )
    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET )
    public String getLoginPage(){
        return "site/login";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public  String register(Model model,User user){
        Map<String, Object> map = userService.register(user);
        if (map==null || map.isEmpty()){
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMas"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    //点击邮件中的激活链接
    @RequestMapping(path="/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code")String code){
        int result = userService.activation(userId, code);
        if(result==ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功！");
            model.addAttribute("target", "/login");
        }else  if(result==ACTIVATION_REPEAT){
            model.addAttribute("msg", "请勿重复激活！");
            model.addAttribute("target", "/index");
        }else {
            //激活失败是否应该将插入user表中的用户删除掉？我刚开始觉得要删除，
            //但其实不应该删除,未激活的用户登录时可以提醒激活，在登陆时做验证就可以了。
            //userService.deleteById(userId);
            model.addAttribute("msg", "激活失败，验证码不正确！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public  void getKaptcha(HttpServletResponse response, HttpSession session){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //将验证码存入session
        //session.setAttribute("kaptcha",text);

        //验证码的归属 验证码的id 用来将cookie中的验证码和redis中的验证码一一对应
        // {kaptchaOwner:text}    cookie{kaptchaOwner:kaptcha}
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        //将图片输出给浏览器  图片中含有验证码
        response.setContentType("image/png");
        try {
            OutputStream outputStream = response.getOutputStream();
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username,String password,String code,boolean rememberme,
                        Model model,HttpServletResponse response,@CookieValue("kaptchaOwner") String kaptchaOwner){

        String kaptcha = null;
        //如果cookie中的含有对应的验证码标识符
        if (StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            //根据key（即验证码的ID）获取redis中的value（即验证码）
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg", "验证码不正确");
            return "site/login";
        }
        //检查账号,密码
        int expiredSeconds=rememberme ? REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        //验证成功后才会生成登录凭证
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);//设置该路径下的请求都会携带cookie
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket")String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
