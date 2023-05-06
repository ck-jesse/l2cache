package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.pool.ThreadPoolSupport;
import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author chenck
 * @date 2023/5/6 11:06
 */
public class ThreadPoolSupportTest {

    /**
     * 线程池队列溢出的验证
     */
    @Test
    public void test() {
        ThreadPoolExecutor pool = ThreadPoolSupport.getPool("testPool", 2, 2, 30, 10
                , new ThreadPoolSupport.MyAbortPolicy("testPool"));

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("run" + finalI);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        while (true) {

        }
    }
}
