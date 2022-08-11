package com.jd.platform.hotkey.worker.keydispatcher;

import java.util.List;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-09
 */
public class Consumer {
    private List<KeyConsumer> consumerList;

    public Consumer(List<KeyConsumer> consumerList) {
        this.consumerList = consumerList;
    }

    public KeyConsumer get(int index) {
        return consumerList.get(index);
    }
}
