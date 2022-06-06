package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    // @Autowired
    // private LoginTicketMapper loginTicketMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.domain}")
    private String domain;

    public User findUserById(int id) {
       // return userMapper.selectById(id);
        User user = getCache(id);
        if(user==null){
            user = initCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if(user==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("mailMsg", "邮箱不能为空！");
            return map;
        }
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u!=null){
            map.put("usernameMsg", "该用户名已存在！");
            return map;
        }

        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u!=null){
            map.put("emailMsg", "该邮箱已被注册");
        }
        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());//随机生成的激活码
        user.setHeaderUrl(String.format("https://images/nowcoder.com/head/%dr.png",new Random().nextInt(1000)) );
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        //thymeleaf中的context 将context中的信息放入mail/activation页面去
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        //String url= domain +"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }


        //点击邮件中的链接激活账号
    public int activation(int usedId,String code){
        User user = userMapper.selectById(usedId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(usedId, 1);
            clearCache(usedId);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }


    public Map<String,Object> login(String username,String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        if(StringUtils.isBlank(password)){
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }
        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);//0代表登录状态有效 1代表无效
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds*1000));
      //  loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //将loginTicket序列化为json格式的字符串
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return  map;
    }

    //退出登录
    public void logout(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    //根据凭证号 获取凭证
    public LoginTicket findLoginTicket(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        return loginTicket;
    }

    //更新头像
    public boolean updateHeader(int userId,String headerUrl){
        //return userMapper.updateHeader(userId, headerUrl);
        boolean  res = userMapper.updateHeader(userId, headerUrl)>0;
        clearCache(userId);
        return res;
    }

    //更新密码
    public boolean updatePassword(int userId,String password){
        return userMapper.updatePassword(userId, password)>0;
    }

    //根据name查询用户
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    // 1 优先从缓存中取值
    public User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2 取不到时初始化缓存数据
    public User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    // 3 当数据变更时清除用户信息缓存
    public void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();

        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case (1):
                        return AUTHORITY_USER;
                    case (2):
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_ADMIN;
                }
            }
        });
        return list;
    }


}
