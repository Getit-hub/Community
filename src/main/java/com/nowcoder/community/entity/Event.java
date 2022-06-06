package com.nowcoder.community.entity;


import java.util.HashMap;
import java.util.Map;

public class Event {

    private String topic;

    //发起事件行为的用户，比如说用户A给 用户B的帖子点赞了 A就是userId, B就是entityUserId。
    private int userId;

    private int entityType;

    private int entityId;

    //事件中的宾语用户
    private int entityUserId;

    private Map<String, Object> data = new HashMap<>();


    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
