package com.coy.l2cache.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chenck
 * @date 2020/4/26 19:37
 */
@Service
public class CaffeineCacheService {

    private final Logger logger = LoggerFactory.getLogger(CaffeineCacheService.class);

    /**
     * 用于模拟db
     */
    private static Map<String, User> userMap = new HashMap<>();

    {
        userMap.put("user01", new User("user01", "addr"));
        userMap.put("user02", new User("user03", "addr"));
        userMap.put("user03", new User("user03", "addr"));
    }

    /**
     * 获取或加载缓存项
     * <p>
     * 注：sync=false，CaffeineCache在定时刷新过期缓存时，是通过get(Object key)来获取缓存项，由于没有valueLoader（加载缓存项的具体逻辑），所以定时刷新缓存时，缓存项过期则会被淘汰。
     */
    @Cacheable(value = "userCache", key = "#userId")
    public User queryUser(String userId) {
        User user = userMap.get(userId);
        try {
            Thread.sleep(1000);// 模拟加载数据的耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("加载数据:{}", user);
        return user;
    }

    @Cacheable(value = "queryUserSync", key = "#userId", sync = true)
    public User queryUserSync(String userId) {
        User user = userMap.get(userId);
        logger.info("加载数据:{}", user);
        return user;
    }

    /**
     * 获取或加载缓存项
     * <p>
     * 注：因底层是基于caffeine来实现一级缓存，所以利用的caffeine本身的同步机制来实现
     * sync=true 则表示并发场景下同步加载缓存项，
     * sync=true，是通过get(Object key, Callable<T> valueLoader)来获取或加载缓存项，此时valueLoader（加载缓存项的具体逻辑）会被缓存起来，所以CaffeineCache在定时刷新过期缓存时，缓存项过期则会重新加载。
     * sync=false，是通过get(Object key)来获取缓存项，由于没有valueLoader（加载缓存项的具体逻辑），所以CaffeineCache在定时刷新过期缓存时，缓存项过期则会被淘汰。
     * <p>
     * 建议：设置@Cacheable的sync=true，可以利用Caffeine的特性，防止缓存击穿（方式同一个key和不同key）
     */
    @Cacheable(value = "queryUserSyncList", key = "#userId", sync = true)
    public List<User> queryUserSyncList(String userId) {
        User user = userMap.get(userId);
        List<User> list = new ArrayList();
        list.add(user);
        logger.info("加载数据:{}", list);
        return list;
    }

    /**
     * 设置缓存
     * 注：通过 @CachePut 标注的方法添加的缓存项，在CaffeineCache的定时刷新过期缓存任务执行时，缓存项过期则会被淘汰。
     * 如果先执行了 @Cacheable(sync = true) 标注的方法，再执行 @CachePut 标注的方法，那么在CaffeineCache的定时刷新过期缓存任务执行时，缓存项过期则会重新加载。
     */
    @CachePut(value = "userCacheSync", key = "#userId")
    public User putUser(String userId, User user) {
        return user;
    }

    /**
     * 淘汰缓存
     */
    @CacheEvict(value = "userCacheSync", key = "#userId")
    public String evictUserSync(String userId) {
        return userId;
    }
}
