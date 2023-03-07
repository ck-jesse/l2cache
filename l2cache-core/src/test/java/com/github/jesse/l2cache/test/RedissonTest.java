package com.github.jesse.l2cache.test;

import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RSetCache;
import org.redisson.api.RSortedSet;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2023/3/7 11:22
 */
public class RedissonTest {

    RedissonClient redissonClient;

    @Before
    public void before() {
        redissonClient = Redisson.create();
    }

    /**
     * Redisson 分布式集合
     * https://github.com/redisson/redisson/wiki/7.-%E5%88%86%E5%B8%83%E5%BC%8F%E9%9B%86%E5%90%88#71-%E6%98%A0%E5%B0%84map
     */
    @Test
    public void test(){
        String key = "data";
        String value = "value";

        // Redis list结构
        RList rList = redissonClient.getList(key);
        rList.expire(60, TimeUnit.SECONDS);// 为list集合设置过期时间

        // Redis set结构(元素唯一)
        RSet rSet = redissonClient.getSet(key);
        rSet.add(value);

        // Redis set结构
        // 在RSet的基础上提供淘汰机制，Redis自身并不支持Set当中的元素淘汰
        RSetCache rSetCache = redissonClient.getSetCache(key);
        rSetCache.expire(60, TimeUnit.SECONDS);// 为set集合设置过期时间
        rSetCache.add(value, 60, TimeUnit.SECONDS);// 为set集合中的元素设置过期时间

        // Redis SortedSet，在保证元素唯一性的前提下，通过比较器（Comparator）接口实现了对元素的排序。
        RSortedSet rSortedSet = redissonClient.getSortedSet(key);
        rSortedSet.add(value);

        // Redis zset结构，按插入时指定的元素评分排序的集合
        RScoredSortedSet rScoredSortedSet = redissonClient.getScoredSortedSet(key);
        rScoredSortedSet.expire(60, TimeUnit.SECONDS);// 为zset集合设置过期时间
        rScoredSortedSet.add(1, value);

        // Redis hash结构
        RMapCache rMapCache = redissonClient.getMapCache(key);
        rMapCache.put(key, value, 60, TimeUnit.SECONDS);

    }
}
