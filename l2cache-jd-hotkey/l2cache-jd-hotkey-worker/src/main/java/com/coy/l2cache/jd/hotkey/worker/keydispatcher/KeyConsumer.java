package com.coy.l2cache.jd.hotkey.worker.keydispatcher;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyModel;
import com.coy.l2cache.jd.hotkey.worker.keylistener.IKeyListener;
import com.coy.l2cache.jd.hotkey.worker.keylistener.KeyEventOriginal;


import static com.coy.l2cache.jd.hotkey.worker.tool.InitConstant.totalDealCount;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-09
 */
public class KeyConsumer {


    private IKeyListener iKeyListener;

    public void setKeyListener(IKeyListener iKeyListener) {
        this.iKeyListener = iKeyListener;
    }

    public void beginConsume() {
        while (true) {
            try {
                HotKeyModel model = DispatcherConfig.QUEUE.take();
                if (model.isRemove()) {
                    iKeyListener.removeKey(model, KeyEventOriginal.CLIENT);
                } else {
                    iKeyListener.newKey(model, KeyEventOriginal.CLIENT);
                }

                //处理完毕，将数量加1
                totalDealCount.increment();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
