package com.jd.platform.hotkey.worker.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-04
 */
@Component
public class Test {
    @Resource(name = "hotKeyCache")
    private Cache<String, Object> hotCache;

    public static void main(String[] args) {
        Map map = new HashMap();
//        map.put("a", "b");
//        System.out.println(map);
                Cache<String, Object> cache = Caffeine.newBuilder()
                .initialCapacity(1024)//初始大小
                .maximumSize(5000000)//最大数量
                .expireAfterWrite(5, TimeUnit.MINUTES)//过期时间
                .softValues()
                .build();

                cache.put("yuan2012555", 1);
//        System.out.println(cache.getIfPresent("yuan2012555"));


        System.out.println("lwymail163".hashCode() %4);
        System.out.println("272551766_m".hashCode() %4);
        System.out.println("hanxu123".hashCode() %4);
        System.out.println("abc123ab".hashCode() %4);

        System.out.println("hanxu123hanxu".hashCode() %4);
        System.out.println(Math.abs("lwy163mail".hashCode() %4));

    }

//    @PostConstruct
//    public void aa() throws InterruptedException {
//        Executor executor = Executors.newCachedThreadPool();
//        Cache<String, Object> cache = Caffeine.newBuilder()
//                .executor(executor)
//                .initialCapacity(1024)//初始大小
//                .maximumSize(5000000)//最大数量
//                .expireAfterWrite(5, TimeUnit.MINUTES)//过期时间
//                .softValues()
//                .build();
//        long i = 0;
//        while (true) {
//            cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
////            hotCache.put("i" + i, i);
//            i++;
//        }
//    }
}
