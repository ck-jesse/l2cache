package com.jd.platform.hotkey.worker.keydispatcher;

import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.worker.keylistener.IKeyListener;
import com.jd.platform.hotkey.worker.keylistener.KeyEventOriginal;


import static com.jd.platform.hotkey.worker.tool.InitConstant.totalDealCount;

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
