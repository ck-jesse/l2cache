package com.coy.l2cache.jd.hotkey.sample;

import com.coy.l2cache.jd.hotkey.client.callback.JdHotKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
@Component
public class Cache {
    @Resource
    private RedisTemplate<String, String> redisTemplate;


    public String getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    //最佳实践：
    //
    //1 判断用户是否是刷子
    //
    //        if (JdHotKeyStore.isHotKey(“pin__” + thePin)) {
    //            //限流他，do your job
    //        }
    //2 判断商品id是否是热点
    //
    //
    //
    //     Object skuInfo = JdHotKeyStore.getValue("skuId__" + skuId);
    //           if(skuInfo == null) {
    //
    //         JdHotKeyStore.smartSet("skuId__" + skuId, theSkuInfo);
    //           } else {
    //
    //                  //使用缓存好的value即可
    //
    //            }
    //
    //   或者这样：
    //
    //
    //
    //        if (JdHotKeyStore.isHotKey(key)) {
    //                           //注意是get，不是getValue。getValue会获取并上报，get是纯粹的本地获取
    //
    //            Object skuInfo = JdHotKeyStore.get("skuId__" + skuId);
    //                          if(skuInfo == null) {
    //
    //                JdHotKeyStore.smartSet("skuId__" + skuId, theSkuInfo);
    //                          } else {
    //
    //                                  //使用缓存好的value即可
    //
    //                          }
    //
    //        }

    public String get(String key) {
        Object object = JdHotKeyStore.getValue(key);
        //如果已经缓存过了
        if (object != null) {
            System.out.println("is hot key");
            return object.toString();
        } else {
            String value = getFromRedis(key);
            JdHotKeyStore.smartSet(key, value);
            return value;
        }
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void remove(String key) {
        JdHotKeyStore.remove(key);
        //do your job
    }


    private Logger logger = LoggerFactory.getLogger(getClass());


//    @PostConstruct
    public void test() {

        CompletableFuture.runAsync(() -> {
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                logger.info("beat");
//                Object object = JdHotKeyStore.getValue("a");
//                if (object != null) {
//                    System.err.println("is hot key " + object);
//                } else {
//                    System.err.println("set value");
//                    JdHotKeyStore.smartSet("a", "a");
//                }
                if (JdHotKeyStore.isHotKey("a")) {
                    logger.error("isHot");
                }
            }
        });

    }

}
