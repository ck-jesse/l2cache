package com.github.jesse.l2cache.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ExpireTimeUtil {

    /**
     * 计算key的过期时间字符串
     *
     * @param expireTime 过期时间（毫秒）
     */
    public static String toStr(long expireTime) {
        long currentTime = System.currentTimeMillis();
        // 转换为指定格式，方便排查问题
        return DateUtil.format(new Date(currentTime + expireTime), DatePattern.NORM_DATETIME_MS_PATTERN);
    }

    /**
     * 计算key的过期时间字符串
     *
     * @param currentTime 当前时间（毫秒）
     * @param expireTime  过期时间（毫秒）
     */
    public static String toStr(long currentTime, long expireTime) {
        // 转换为指定格式，方便排查问题
        return DateUtil.format(new Date(currentTime + expireTime), DatePattern.NORM_DATETIME_MS_PATTERN);
    }

    public static void main(String[] args) {
        long curr = System.currentTimeMillis(); // 当前时间作为基准
        System.out.println("当前时间（毫秒）" + DateUtil.format(new Date(curr), DatePattern.NORM_DATETIME_MS_PATTERN));
        long curr2Nanos = TimeUnit.MILLISECONDS.toNanos(curr);
        long curr2NanosMillis = TimeUnit.NANOSECONDS.toMillis(curr2Nanos);
        System.out.println("当前时间（毫秒 -> 纳秒 -> 毫秒）" + DateUtil.format(new Date(curr2NanosMillis), DatePattern.NORM_DATETIME_MS_PATTERN));


        // System.nanoTime() 返回相对的纳秒值，只能用于计算时间间隔，不能直接转换为日期
        // 错误用法
        long currNano = System.nanoTime();
        long currNanoMillis = TimeUnit.NANOSECONDS.toMillis(currNano);
        System.out.println("错误用法：当前时间（纳秒 -> 毫秒）" + DateUtil.format(new Date(curr + currNanoMillis), DatePattern.NORM_DATETIME_MS_PATTERN));

        // 正确用法：System.nanoTime() 只能用于计算时间间隔
        long startNano = System.nanoTime();
        System.out.println("startNano:" + startNano);
        try {
            Thread.sleep(10); // 模拟一些操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long endNano = System.nanoTime();
        System.out.println("endNano:" + endNano);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endNano - startNano);
        System.out.println("正确用法：时间间隔: " + durationMillis + " 毫秒");


        long ttl = 60000;
        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(ttl);
        System.out.println(ttlNanos);
    }
}
