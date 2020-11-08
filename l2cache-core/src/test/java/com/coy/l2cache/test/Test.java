package com.coy.l2cache.test;

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
        queue.add(3);
        System.out.println(queue);
    }
}
