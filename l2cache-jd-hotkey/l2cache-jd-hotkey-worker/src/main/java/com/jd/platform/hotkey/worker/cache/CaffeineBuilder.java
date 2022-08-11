package com.jd.platform.hotkey.worker.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jd.platform.hotkey.worker.tool.InitConstant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
public class CaffeineBuilder {
    private static ExecutorService executorService = Executors.newFixedThreadPool(4);
    /**
     * 构建所有来的要缓存的key cache
     */
    public static Cache<String, Object> buildAllKeyCache() {
        //老版本jdk1.8.0_20之前，caffeine默认的forkJoinPool在及其密集的淘汰过期时，会有forkJoinPool报错。建议用新版jdk
        return Caffeine.newBuilder()
                .initialCapacity(8192)//初始大小
                .maximumSize(5000000)//最大数量。这个数值我设置的很大，按30万每秒，每分钟是1800万，实际可以调小
                .expireAfterWrite(InitConstant.caffeineMaxMinutes, TimeUnit.MINUTES)//过期时间，默认1分钟
                .executor(executorService)
                .softValues()
                .build();
    }

    /**
     * 刚生成的热key，先放这里放几秒后，应该所有客户端都收到了热key并本地缓存了。这几秒内，不再处理同样的key了
     */
    public static Cache<String, Object> buildRecentHotKeyCache() {
        return Caffeine.newBuilder()
                .initialCapacity(256)//初始大小
                .maximumSize(50000)//最大数量
                .expireAfterWrite(5, TimeUnit.SECONDS)//过期时间
                .executor(executorService)
                .softValues()
                .build();
    }

}
