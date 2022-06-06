package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    //关注
    public void follow(int userId,int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                //关注的实体
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                //粉丝
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);

                //开启事务
                redisOperations.multi();

                //1将被关注的实体添加到被关注zset中
                //2将点击关注的当前用户添加到粉丝zset中
                redisOperations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                redisOperations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());

                return redisOperations.exec();


            }
        });
    }

    //取消关注
    public void unfollow(int userId,int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                //关注的实体
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                //粉丝
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);
                //开启事务
                redisOperations.multi();

                redisOperations.opsForZSet().remove(followeeKey,entityId);
                redisOperations.opsForZSet().remove(followerKey,userId);

                return redisOperations.exec();

            }
        });
    }

    //查询关注的实体的数量
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        long followeeCount = redisTemplate.opsForZSet().zCard(followeeKey);
        return followeeCount;
    }


    //查询实体的粉丝的数量
    public long findFollowerCount(int entityType,int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        long followerCount = redisTemplate.opsForZSet().zCard(followerKey);

        return followerCount;
    }

    //查询当前用户是否已关注该实体 1先查询当前用户关注的实体列表 然后判断该实体是否在关注列表里
    public boolean hasFollowed(int userId,int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    //查询某用户关注的人
    public List<Map<String,Object>> findFollowees(int userId, int offset, int limit){
        String followeesKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeesKey, offset, offset + limit - 1);

        if (targetIds==null){
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId:targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                redisTemplate.opsForZSet().score(followeesKey, targetId);
                Double score = redisTemplate.opsForZSet().score(followeesKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
        }
        return list;
    }


    //查询某用户的粉丝
    public List<Map<String,Object>> findFollowers(int userId, int offset, int limit){
        String followersKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followersKey, offset, offset + limit);

        if (targetIds == null){
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId:targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            redisTemplate.opsForZSet().score(followersKey, targetId);
            Double score = redisTemplate.opsForZSet().score(followersKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
