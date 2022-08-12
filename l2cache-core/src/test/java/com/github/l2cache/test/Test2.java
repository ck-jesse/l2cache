package com.github.l2cache.test;

import com.github.l2cache.util.pool.RunnableMdcWarpper;
import com.github.l2cache.util.pool.ThreadPoolSupport;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author chenck
 * @date 2020/12/3 23:45
 */
public class Test2 {

    private static final ThreadPoolExecutor poolExecutor = ThreadPoolSupport.getPool("publish_redis_msg");

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            poolExecutor.execute(new RunnableMdcWarpper(() -> {
                System.out.println("publish message  " + finalI);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "publish message  " + finalI));
        }
        while (true) {

        }
    }
}
