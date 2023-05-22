package com.github.jesse.l2cache.example;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chenck
 * @date 2023/5/17 15:25
 */
public class Test {

    public static void main(String[] args) {
        String text = null;
        System.out.println(!StringUtils.hasText(text));
        System.out.println(StrUtil.isBlank(text));

        System.out.println(StringUtils.isEmpty(text));
        System.out.println(StrUtil.isEmpty(text));
    }

    @org.junit.Test
    public void collectionTest(){
        Map<Object, Object> notHitCacheKeyMap = new HashMap<>();
        System.out.println(CollectionUtils.isEmpty(notHitCacheKeyMap));
        System.out.println(CollectionUtil.isEmpty(notHitCacheKeyMap));
    }

    @org.junit.Test
    public void idUtilTest(){

        System.out.println(IdUtil.fastSimpleUUID());
        System.out.println(IdUtil.nanoId());
        System.out.println(IdUtil.getSnowflakeNextIdStr());
    }
}
