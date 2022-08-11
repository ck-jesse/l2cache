package com.jd.platform.hotkey.dashboard.netty;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.dashboard.cache.CaffeineBuilder;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.model.KeyTimely;
import com.jd.platform.hotkey.dashboard.util.RuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 此处存储所有发来的热key，统一处理入库
 *
 * @author wuweifeng
 * @version 1.0
 * @date 2020-08-31
 */
public class HotKeyReceiver {

    /**
     * 发来的热key集中营
     */
    private static LinkedBlockingQueue<HotKeyModel> hotKeyStoreQueue = new LinkedBlockingQueue<>();
    /**
     * 存储实时hotkey，供界面查询实时热key
     */
    private static Map<Integer, Cache<String, Object>> aliveKeyStore = new ConcurrentHashMap<>();

    private static Logger logger = LoggerFactory.getLogger("HotKeyReceiver");

    /**
     * netty收到的先存这里
     */
    public static void push(HotKeyModel model) {
        hotKeyStoreQueue.offer(model);
    }

    public static HotKeyModel take() {
        try {
            return hotKeyStoreQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将热key存入本地缓存，设置过期时间
     */
    public static void put(HotKeyModel hotKeyModel) {
        String appNameKey = hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
        KeyRule keyRule = RuleUtil.findByKey(appNameKey);
        if (keyRule == null) {
            logger.error("rule is null, hotkeyModel " + hotKeyModel.getAppName() + "-" + hotKeyModel.getKey());
            return;
        }
        Cache<String, Object> cache = aliveKeyStore.computeIfAbsent(keyRule.getDuration(), s -> CaffeineBuilder.cache(keyRule.getDuration()));
        cache.put(appNameKey, hotKeyModel);
    }

    /**
     * 展示当前所有的实时热key
     */
    public static List<KeyTimely> list(SearchReq searchReq) {
        List<KeyTimely> timelyList = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (Integer duration : aliveKeyStore.keySet()) {
            Cache<String, Object> cache = aliveKeyStore.get(duration);
            ConcurrentMap<String, Object> concurrentHashMap = cache.asMap();
            for (Map.Entry<String, Object> entry : concurrentHashMap.entrySet()) {
                KeyTimely keyTimely = parse((HotKeyModel) entry.getValue(), now);
                if (keyTimely == null) {
                    continue;
                }
                timelyList.add(keyTimely);
            }
        }

        if (searchReq != null) {
            if (StrUtil.isNotEmpty(searchReq.getApp())) {
                timelyList = timelyList.parallelStream().filter(keyTimely -> searchReq.getApp().equals(keyTimely.getAppName())).collect(Collectors.toList());
            }
            if (StrUtil.isNotEmpty(searchReq.getKey())) {
                timelyList = timelyList.parallelStream().filter(keyTimely -> keyTimely.getKey().startsWith(searchReq.getKey())).collect(Collectors.toList());
            }

        }

        timelyList.sort(Comparator.comparing(KeyTimely::getCreateTime).reversed());

        return timelyList;
    }

    /**
     * 将hotkeyModel变成前端需要的对象
     */
    private static KeyTimely parse(HotKeyModel hotKeyModel, long now) {
        String appNameKey = hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
        KeyRule keyRule = RuleUtil.findByKey(appNameKey);
        if (keyRule == null) {
            return null;
        }
        long remainTime = keyRule.getDuration() * 1000 - (now - hotKeyModel.getCreateTime());
        return KeyTimely.aKeyTimely()
                .key(hotKeyModel.getKey())
                .val(UUID.randomUUID().toString())
                .appName(hotKeyModel.getAppName())
                .duration(remainTime / 1000)
                .ruleDesc(RuleUtil.ruleDesc(appNameKey))
                .createTime(new Date(hotKeyModel.getCreateTime())).build();
    }

//    public static void main(String[] args) {
//        List<KeyTimely> keyTimelyList = new ArrayList<>();
//        for (int i = 0; i < 24; i++) {
//            KeyTimely keyTimely = new KeyTimely();
//            keyTimely.setKey(i + "");
//            keyTimely.setCreateTime(new Date());
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            keyTimelyList.add(keyTimely);
//        }
//        keyTimelyList.sort(Comparator.comparing(KeyTimely::getCreateTime).reversed());
//
//        Page<KeyTimely> page =PageUtil.pagination(keyTimelyList, 10, 3);
//        System.out.println(page.getTotal());
//        System.out.println(page.getPage());
//        System.out.println(page.getRows());
//    }





    /**
     * 删除实时热key
     */
    public static boolean delete(String appNameKey) {
        KeyRule keyRule = RuleUtil.findByKey(appNameKey);
        if (keyRule == null) {
            return false;
        }
        Cache<String, Object> cache = aliveKeyStore.get(keyRule.getDuration());
        if (cache == null) {
            return false;
        }
        cache.invalidate(appNameKey);
        return true;
    }

    /**
     * 定时清理caffeine
     */
    public static void cleanUpCaffeine() {
        for (Integer duration : aliveKeyStore.keySet()) {
            Cache<String, Object> cache = aliveKeyStore.get(duration);
            cache.cleanUp();
        }
    }

}
