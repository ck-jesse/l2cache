package com.jd.platform.hotkey.client.core.key;

import cn.hutool.core.util.StrUtil;
import com.jd.platform.hotkey.client.core.rule.KeyRuleHolder;
import com.jd.platform.hotkey.common.model.KeyCountModel;
import com.jd.platform.hotkey.common.tool.Constant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 热点数量统计
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-24
 */
public class TurnCountCollector implements IKeyCollector<KeyHotModel, KeyCountModel> {
    /**
     * 存储格式为：
     * pin_20200624091024 -> 10
     * pin_20200624091025 -> 20
     * <p>
     * sku_20200624091024 -> 123
     * sku_20200624091025 -> 142
     */
    private ConcurrentHashMap<String, HitCount> HIT_MAP_0 = new ConcurrentHashMap<>(512);
    private ConcurrentHashMap<String, HitCount> HIT_MAP_1 = new ConcurrentHashMap<>(512);

    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss";

    private AtomicLong atomicLong = new AtomicLong(0);

    @Override
    public List<KeyCountModel> lockAndGetResult() {
        //自增后，对应的map就会停止被写入，等待被读取
        atomicLong.addAndGet(1);

        List<KeyCountModel> list;
        if (atomicLong.get() % 2 == 0) {
            list = get(HIT_MAP_1);
            HIT_MAP_1.clear();
        } else {
            list = get(HIT_MAP_0);
            HIT_MAP_0.clear();
        }
        return list;
    }

    /**
     * 每10秒上报一次最近10秒的数据，并且删掉相应的key
     */
    private List<KeyCountModel> get(ConcurrentHashMap<String, HitCount> map) {
        List<KeyCountModel> list = new ArrayList<>();
        for (Map.Entry<String, HitCount> entry : map.entrySet()) {
            String key = entry.getKey();
            HitCount hitCount = entry.getValue();
            KeyCountModel keyCountModel = new KeyCountModel();
            keyCountModel.setTotalHitCount(hitCount.totalHitCount.get());
            keyCountModel.setRuleKey(key);
            keyCountModel.setHotHitCount(hitCount.hotHitCount.get());
            list.add(keyCountModel);
        }
        return list;
    }

    @Override
    public void collect(KeyHotModel keyHotModel) {
        if (atomicLong.get() % 2 == 0) {
            put(keyHotModel.getKey(), keyHotModel.isHot(), HIT_MAP_0);
        } else {
            put(keyHotModel.getKey(), keyHotModel.isHot(), HIT_MAP_1);
        }
    }

    @Override
    public void finishOnce() {

    }

    public void put(String key, boolean isHot, ConcurrentHashMap<String, HitCount> map) {
        //如key是pin_的前缀，则存储pin_
        String rule = KeyRuleHolder.rule(key);
        //不在规则内的不处理
        if (StrUtil.isEmpty(rule)) {
            return;
        }
        String nowTime = nowTime();

        //rule + 分隔符 + 2020-10-23 21:11:22
        String mapKey = rule + Constant.COUNT_DELIMITER + nowTime;
        //该方法线程安全
        HitCount hitCount = map.computeIfAbsent(mapKey, v -> new HitCount());
        if (isHot) {
            hitCount.hotHitCount.incrementAndGet();
        }
        hitCount.totalHitCount.incrementAndGet();
    }

    private String nowTime() {
        Date nowTime = new Date(System.currentTimeMillis());
        SimpleDateFormat sdFormatter = new SimpleDateFormat(FORMAT);
        return sdFormatter.format(nowTime);
    }

    private class HitCount {
        private AtomicInteger hotHitCount = new AtomicInteger();
        private AtomicInteger totalHitCount = new AtomicInteger();
    }
}
