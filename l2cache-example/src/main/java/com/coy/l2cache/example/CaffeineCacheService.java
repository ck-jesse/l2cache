package com.coy.l2cache.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenck
 * @date 2020/4/26 19:37
 */
@Service
public class CaffeineCacheService {

    private final Logger logger = LoggerFactory.getLogger(CaffeineCacheService.class);

    /**
     * 加载缓存项
     */
    @Cacheable(value = "userCache", key = "#userId")
    public User queryUser(String userId) {
        User user = new User(userId, "addr");
        try {
            Thread.sleep(2000);// 模拟加载数据的耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("加载数据:{}", user);
        return user;
    }

    /**
     * sync=true 则表示并发场景下同步加载缓存项，
     * 注：因底层是基于caffeine来实现一级缓存，所以利用的caffeine本身的同步机制来实现
     * <p>
     * 建议：设置@Cacheable的sync=true，可以利用Caffeine的特性，防止缓存击穿（方式同一个key和不同key）
     */
    @Cacheable(value = "userCacheSync", key = "#userId", sync = true)
    public List<User> queryUserSync(String userId) {
        List<User> list = new ArrayList();
        list.add(new User(userId, "addr1"));
        list.add(new User(userId, "addr2"));
        list.add(new User(userId, /*"addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +
                "-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +
                "-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +
                "-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +
                "-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +
                "-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr-addr" +*/
                "-addr-addr-addr-addr-addr-addr"));
        try {
            Thread.sleep(2000);// 模拟加载数据的耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("加载数据:{}", list);
        return list;
    }

    /**
     * 淘汰缓存
     */
    @CacheEvict(value = "userCacheSync", key = "#userId")
    public String evictUserSync(String userId) {
        return userId;
    }
}
