package com.jd.platform.hotkey.client.core.key;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.jd.platform.hotkey.common.model.HotKeyModel;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮流提供读写、暂存key的操作。
 * 上报时譬如采用定时器，每隔0.5秒调度一次push方法。在上报过程中，
 * 不应阻塞写入操作。所以计划采用2个HashMap加一个atomicLong，如奇数时写入map0，为1写入map1，上传后会清空该map。
 *
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class TurnKeyCollector implements IKeyCollector<HotKeyModel, HotKeyModel> {
    private ConcurrentHashMap<String, HotKeyModel> map0 = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HotKeyModel> map1 = new ConcurrentHashMap<>();

    private AtomicLong atomicLong = new AtomicLong(0);

    @Override
    public List<HotKeyModel> lockAndGetResult() {
        //自增后，对应的map就会停止被写入，等待被读取
        atomicLong.addAndGet(1);

        List<HotKeyModel> list;
        if (atomicLong.get() % 2 == 0) {
            list = get(map1);
            map1.clear();
        } else {
            list = get(map0);
            map0.clear();
        }
        return list;
    }

    private List<HotKeyModel> get(ConcurrentHashMap<String, HotKeyModel> map) {
        return CollectionUtil.list(false, map.values());
    }

    @Override
    public void collect(HotKeyModel hotKeyModel) {
        String key = hotKeyModel.getKey();
        if (StrUtil.isEmpty(key)) {
            return;
        }
        if (atomicLong.get() % 2 == 0) {
            //不存在时返回null并将key-value放入，已有相同key时，返回该key对应的value，并且不覆盖
            HotKeyModel model = map0.putIfAbsent(key, hotKeyModel);
            if (model != null) {
                model.setCount(model.getCount() + hotKeyModel.getCount());
            }
        } else {
            HotKeyModel model = map1.putIfAbsent(key, hotKeyModel);
            if (model != null) {
                model.setCount(model.getCount() + hotKeyModel.getCount());
            }
        }

    }

    @Override
    public void finishOnce() {

    }

}
