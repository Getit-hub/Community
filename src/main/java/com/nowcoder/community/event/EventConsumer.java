package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageServce;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.Topic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageServce messageServce;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleCommmentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("通知消息内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("通知消息格式错误！");
            return;
        }

        //需要注意 下面的步骤相当于从事件队列中取出事件，但是队列中的是事件消息，需要转换成通知
        //从消息队列中取出事件形成站内通知消息
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());//事件触发者
        //entityType和entityId共同标识了 事件被触发者,即接收通知的人
        content.put("entityType",event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()){
            for (Map.Entry<String,Object> entry:event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        //注意这里添加的是通知 而不是私信
        messageServce.addMessage(message);
    }

    //发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record==null || record.value()==null){
            logger.error("消息的内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误！");
            return;
        }

        //保存到
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    //删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record==null || record.value()==null){
            logger.error("消息的内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误！");
            return;
        }
        //这里只从elasticsearch服务器中删除帖子 却没有从数据库中删除 为什么？
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }


}
