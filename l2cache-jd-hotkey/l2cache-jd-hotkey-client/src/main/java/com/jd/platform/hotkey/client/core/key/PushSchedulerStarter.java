package com.jd.platform.hotkey.client.core.key;

import com.jd.platform.hotkey.client.Context;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.KeyCountModel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时推送一批key到worker
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class PushSchedulerStarter {

    /**
     * 每0.5秒推送一次待测key
     */
    public static void startPusher(Long period) {
        if (period == null || period <= 0) {
            period = 500L;
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            IKeyCollector<HotKeyModel, HotKeyModel> collectHK = KeyHandlerFactory.getCollector();
            KeyHandlerFactory.getPusher().send(Context.APP_NAME, collectHK.lockAndGetResult());
            collectHK.finishOnce();
        },0, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 每10秒推送一次数量统计
     */
    public static void startCountPusher(Integer period) {
        if (period == null || period <= 0) {
            period = 10;
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            IKeyCollector<KeyHotModel, KeyCountModel> collectHK = KeyHandlerFactory.getCounter();
            KeyHandlerFactory.getPusher().sendCount(Context.APP_NAME, collectHK.lockAndGetResult());
            collectHK.finishOnce();
        },0, period, TimeUnit.SECONDS);
    }

}
