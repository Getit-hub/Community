package com.nowcoder.community.util;


public class RedisKeyUtil {

    private static final String SPLIT = ":";
    //某个实体的赞
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    //某个用户的赞
    private static final String PREFIX_USER_LIKE = "like:user";

    //关注的人
    private static final String PREFIX_FOLLOWER = "follower";
    //粉丝
    private static final String PREFIX_FOLLOWEE = "followee";

    //验证码
    private static final String PREFIX_KAPTCHA = "kaptcha";

    //登录凭证
    private static final String PREFIX_TICKET = "ticket";

    //用户信息
    private static final String PREFIX_USER = "user";


    //某个实体的赞
    //like:entity:entityType:entityID ->set(userId)  key是具体实体（帖子实体或评论实体）value是set集合 里面包含所有点赞用户的id
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    //某个用户的赞
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    //某个用户关注的实体(可以是用户 也可以是帖子、题目的等) 比如关注了多少用户 关注了多少个帖子 关注了多少主题
    //followee:userID:entityType -> zset(entityId,now)
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    //某个实体（用户、帖子、题目）拥有的粉丝
    //follower:entityType:entityId->zset(userId,now)
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    //登录使用的验证码
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录的凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    //用户信息
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

}
