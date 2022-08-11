/**
 * @Author: zengjucai
 * @Date: 2021/5/24 19:35
 */
/**
 * @description disruptor代码样例。每10ms向disruptor中插入一个元素，消费者读取数据，并打印到终端
 */
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class DisruptorMain
{
    public static void main(String[] args) throws Exception {
        // 队列中的元素
        class Element {

            private Map<String, Integer> map = new ConcurrentHashMap<>(100);

            public Map<String, Integer> get() {
                return map;
            }

            public void set(String key) {
                if (map.containsKey(key)) {
                    map.put(key, map.get(key) + 1);
                } else {
                    map.put(key, 1);
                }
            }

        }

        // 生产者的线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "simpleThread");
            }
        };

        // RingBuffer生产工厂,初始化RingBuffer的时候使用
        EventFactory<Element> factory = new EventFactory<Element>() {
            @Override
            public Element newInstance() {
                return new Element();
            }
        };

        // 处理Event的handler
        EventHandler<Element> handler = new EventHandler<Element>() {
            @Override
            public void onEvent(Element element, long sequence, boolean endOfBatch) throws InterruptedException {
                //Thread.sleep(1000);
                System.out.println(LocalDateTime.now() + "----" + "Element: " + element.get());
            }
        };

        // 阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();

        // 指定RingBuffer的大小
        int bufferSize = 16;

        // 创建disruptor，采用单生产者模式
        Disruptor<Element> disruptor = new Disruptor(factory, bufferSize, threadFactory, ProducerType.SINGLE, strategy);

        // 设置EventHandler
        disruptor.handleEventsWith(handler);

        // 启动disruptor的线程
        disruptor.start();

        RingBuffer<Element> ringBuffer = disruptor.getRingBuffer();

        LocalDateTime time = LocalDateTime.now();
        // 获取下一个可用位置的下标
        long sequence = ringBuffer.next();
        for (int i = 0; i < 1000; i++) {
            try {
                LocalDateTime time1 = LocalDateTime.now();
                Duration duration = Duration.between(time, time1);
                if (duration.getSeconds() >= 3) {
                    ringBuffer.publish(sequence);
                    sequence = ringBuffer.next();
                    time = LocalDateTime.now();
                }
                // 返回可用位置的元素
                Element event = ringBuffer.get(sequence);
                // 设置该位置元素的值
                event.set(String.valueOf(new Random().nextInt(20)));
            } finally {
                Thread.sleep(100);
            }
        }

    }
}