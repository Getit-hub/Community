package com.nowcoder.community;

import com.nowcoder.community.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
        String redisKey = "test:count";

        redisTemplate.opsForValue().set(redisKey,1);

        System.out.println(redisTemplate.opsForValue().get(redisKey));

        System.out.println(redisTemplate.opsForValue().increment(redisKey));
    }

    @Test
    public void testHashes(){
        String redisKey = "test:user";
        redisTemplate.opsForHash().put(redisKey,"id",1);
        redisTemplate.opsForHash().put(redisKey,"username","zhangsan");
        System.out.println(redisTemplate.opsForHash().get(redisKey,"id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey,"username"));
    }

    @Test
    public void testLists(){
        String redisKey = "test:ids";
        redisTemplate.opsForList().leftPush("test:ids", 101);
        redisTemplate.opsForList().leftPush("test:ids", 102);
        redisTemplate.opsForList().leftPush("test:ids", 103);

        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey,0));

        System.out.println(redisTemplate.opsForList().rightPop(redisKey));
    }

    @Test
    public void testSets(){
        String redisKey = "test:teacher";

        redisTemplate.opsForSet().add(redisKey, "zhangsan", "lisi", "wangwu");

        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public void testSortedSet(){
        String redisKey = "test:students";


        redisTemplate.opsForZSet().add(redisKey, "唐僧",80);
        redisTemplate.opsForZSet().add(redisKey, "悟空",90);
        redisTemplate.opsForZSet().add(redisKey, "八戒",60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"唐僧"));
        System.out.println(redisTemplate.opsForZSet().rank(redisKey,"悟空"));
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2));
    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    //编程式事务
    @Test
    public void testTransactional() {
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = "test:tx";

                redisOperations.multi();//启用事务

                redisOperations.opsForSet().add(redisKey, "zhangsan");
                redisOperations.opsForSet().add(redisKey, "lisi");
                redisOperations.opsForSet().add(redisKey, "wangwu");

                //此时查询不到数据 因为还未执行事务
                System.out.println(redisOperations.opsForSet().members(redisKey));

                return redisOperations.exec();//执行事务
            }
        });
        System.out.println(obj);

        //下面的lambda表达式为什么会报错

        /*{
        Object obj1 = redisTemplate.execute(
                (RedisOperations operations) -> {
                    String redisKey = "test:tx";

                    operations.multi();//启用事务

                    operations.opsForSet().add(redisKey, "zhangsan");
                    operations.opsForSet().add(redisKey, "lisi");
                    operations.opsForSet().add(redisKey, "wangwu");

                    //此时查询不到数据 因为还未执行事务
                    System.out.println(operations.opsForSet().members(redisKey));

                    return operations.exec();//执行事务
                }
        )  ;
        System.out.println(obj1);
        }*/
    }




}
