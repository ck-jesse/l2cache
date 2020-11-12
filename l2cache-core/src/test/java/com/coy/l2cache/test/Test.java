package com.coy.l2cache.test;

import org.redisson.Redisson;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/9/23 9:41
 */
public class Test {

    public static void main(String[] args) {

        System.out.println(TimeUnit.SECONDS.toMillis(30));

        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue(2);
        queue.add(1);
        System.out.println(queue);
        queue.add(2);
        System.out.println(queue);


        RedissonClient redissonClient = Redisson.create();

        RBatch batch = redissonClient.createBatch();
        for (int i = 0; i < 50; i++) {
            String tempKey = "key" + i;
            String value = "value" + i;
            batch.getBucket(tempKey).setAsync(value);
            System.out.println("key=" + tempKey + ",value=" + value);
            if (i != 0 && i % 10 == 0) {
                BatchResult result = batch.execute();
                System.out.println("i=" + i + ",SyncedSlaves=" + result.getSyncedSlaves() + ",Responses=" + result.getResponses());
            }
        }
        BatchResult result = batch.execute();
        System.out.println("SyncedSlaves=" + result.getSyncedSlaves() + ",Responses=" + result.getResponses());
    }
}
