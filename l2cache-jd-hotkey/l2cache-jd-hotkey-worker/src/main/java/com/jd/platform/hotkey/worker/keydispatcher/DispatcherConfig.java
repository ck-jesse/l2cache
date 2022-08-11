package com.jd.platform.hotkey.worker.keydispatcher;

import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.worker.keylistener.IKeyListener;
import com.jd.platform.hotkey.worker.tool.CpuNum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-09
 */
@Configuration
public class DispatcherConfig {
    @Resource
    private IKeyListener iKeyListener;

    private ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();

    @Value("${thread.count}")
    private int threadCount;

    /**
     * 队列
     */
    public static BlockingQueue<HotKeyModel> QUEUE = new LinkedBlockingQueue<>(2000000);

    @Bean
    public Consumer consumer() {
        int nowCount = CpuNum.workerCount();
        //将实际值赋给static变量
        if (threadCount != 0) {
            nowCount = threadCount;
        } else {
            if (nowCount >= 8) {
                nowCount = nowCount / 2;
            }
        }

        List<KeyConsumer> consumerList = new ArrayList<>();
        for (int i = 0; i < nowCount; i++) {
            KeyConsumer keyConsumer = new KeyConsumer();
            keyConsumer.setKeyListener(iKeyListener);
            consumerList.add(keyConsumer);

            threadPoolExecutor.submit(keyConsumer::beginConsume);
        }
        return new Consumer(consumerList);
    }
}
