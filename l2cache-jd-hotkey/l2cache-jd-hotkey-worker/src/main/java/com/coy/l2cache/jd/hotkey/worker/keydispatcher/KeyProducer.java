package com.coy.l2cache.jd.hotkey.worker.keydispatcher;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyModel;
import com.coy.l2cache.jd.hotkey.worker.tool.InitConstant;
import org.springframework.stereotype.Component;

import static com.coy.l2cache.jd.hotkey.worker.tool.InitConstant.expireTotalCount;
import static com.coy.l2cache.jd.hotkey.worker.tool.InitConstant.totalOfferCount;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-09
 */
@Component
public class KeyProducer {

    public void push(HotKeyModel model, long now) {
        if (model == null || model.getKey() == null) {
            return;
        }
        //5秒前的过时消息就不处理了
        if (now - model.getCreateTime() > InitConstant.timeOut) {
            expireTotalCount.increment();
            return;
        }

        try {
            DispatcherConfig.QUEUE.put(model);
            totalOfferCount.increment();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
