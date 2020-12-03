package com.coy.l2cache.test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenck
 * @date 2020/12/3 18:47
 */
public class Test1 {

    public static void main(String[] args) {
        AtomicInteger waitRefreshNum = new AtomicInteger();
        System.out.println("获取：" + waitRefreshNum.get());
        System.out.println("加1：" + waitRefreshNum.getAndIncrement());
        System.out.println("获取：" + waitRefreshNum.get());
        System.out.println("加1：" + waitRefreshNum.getAndIncrement());

        System.out.println("获取：" + waitRefreshNum.get());
        System.out.println(waitRefreshNum.getAndSet(0));
        System.out.println("获取：" + waitRefreshNum.get());
    }
}
