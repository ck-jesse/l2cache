package com.jd.platform.hotkey.worker.keylistener;

import cn.hutool.core.date.SystemClock;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.worker.cache.CaffeineCacheHolder;
import com.jd.platform.hotkey.worker.netty.pusher.IPusher;
import com.jd.platform.hotkey.worker.rule.KeyRuleHolder;
import com.jd.platform.hotkey.worker.tool.SlidingWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * key的新增、删除处理
 *
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
@Component
public class KeyListener implements IKeyListener {
    @Resource(name = "hotKeyCache")
    private Cache<String, Object> hotCache;
    @Resource
    private List<IPusher> iPushers;

    private static final String SPLITER = "-";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String NEW_KEY_EVENT = "new key created event, key : ";
    private static final String DELETE_KEY_EVENT = "key delete event key : ";

    @Override
    public void newKey(HotKeyModel hotKeyModel, KeyEventOriginal original) {
        //cache里的key
        String key = buildKey(hotKeyModel);
        //判断是不是刚热不久
        Object o = hotCache.getIfPresent(key);
        if (o != null) {
            return;
        }

        //********** watch here ************//
        //该方法会被InitConstant.threadCount个线程同时调用，存在多线程问题
        //下面的那句addCount是加了锁的，代表给Key累加数量时是原子性的，不会发生多加、少加的情况，到了设定的阈值一定会hot
        //譬如阈值是2，如果多个线程累加，在没hot前，hot的状态肯定是对的，譬如thread1 加1，thread2加1，那么thread2会hot返回true，开启推送
        //但是极端情况下，譬如阈值是10，当前是9，thread1走到这里时，加1，返回true，thread2也走到这里，加1，此时是11，返回true，问题来了
        //该key会走下面的else两次，也就是2次推送。
        //所以出现问题的原因是hotCache.getIfPresent(key)这一句在并发情况下，没return掉，放了两个key+1到addCount这一步时，会有问题
        //测试代码在TestBlockQueue类，直接运行可以看到会同时hot

        //那么该问题用解决吗，NO，不需要解决，1 首先要发生的条件极其苛刻，很难触发，以京东这样高的并发量，线上我也没见过触发连续2次推送同一个key的
        //2 即便触发了，后果也是可以接受的，2次推送而已，毫无影响，客户端无感知。但是如果非要解决，就要对slidingWindow实例加锁了，必然有一些开销

        //所以只要保证key数量不多计算就可以，少计算了没事。因为热key必然频率高，漏计几次没事。但非热key，多计算了，被干成了热key就不对了
        SlidingWindow slidingWindow = checkWindow(hotKeyModel, key);
        //看看hot没
        boolean hot = slidingWindow.addCount(hotKeyModel.getCount());

        if (!hot) {
            //如果没hot，重新put，cache会自动刷新过期时间
            CaffeineCacheHolder.getCache(hotKeyModel.getAppName()).put(key, slidingWindow);
        } else {
            hotCache.put(key, 1);

            //删掉该key
            CaffeineCacheHolder.getCache(hotKeyModel.getAppName()).invalidate(key);

            //开启推送
            hotKeyModel.setCreateTime(SystemClock.now());
            logger.info(NEW_KEY_EVENT + hotKeyModel.getKey());
            //分别推送到各client和etcd
            for (IPusher pusher : iPushers) {
                pusher.push(hotKeyModel);
            }

        }

    }

    @Override
    public void removeKey(HotKeyModel hotKeyModel, KeyEventOriginal original) {
        //cache里的key
        String key = buildKey(hotKeyModel);

        hotCache.invalidate(key);
        CaffeineCacheHolder.getCache(hotKeyModel.getAppName()).invalidate(key);

        //推送所有client删除
        hotKeyModel.setCreateTime(SystemClock.now());
        logger.info(DELETE_KEY_EVENT + hotKeyModel.getKey());

        for (IPusher pusher : iPushers) {
            pusher.remove(hotKeyModel);
        }

    }

    /**
     * 生成或返回该key的滑窗
     */
    private SlidingWindow checkWindow(HotKeyModel hotKeyModel, String key) {
        //取该key的滑窗
        return (SlidingWindow) CaffeineCacheHolder.getCache(hotKeyModel.getAppName()).get(key, (Function<String, SlidingWindow>) s -> {
            //是个新key，获取它的规则
            KeyRule keyRule = KeyRuleHolder.getRuleByAppAndKey(hotKeyModel);
            return new SlidingWindow(keyRule.getInterval(), keyRule.getThreshold());
        });
    }

    private String buildKey(HotKeyModel hotKeyModel) {
        return Joiner.on(SPLITER).join(hotKeyModel.getAppName(), hotKeyModel.getKeyType(), hotKeyModel.getKey());
    }

}
