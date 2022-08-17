//package cn.weeget.hotkey.worker.tool;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//
//import java.util.concurrent.*;
//
///**
// * @author wuweifeng
// * @version 1.0
// * @date 2020-06-10
// */
//public class TestBlockQueue {
//    public static BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>(2000000);
//    private static Cache cache = buildAllKeyCache();
//    private static Cache hotCache = buildAllKeyCache();
//    private static ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
//
//    public static void main(String[] args) throws InterruptedException {
//
//        for (int i = 0; i < 8; i++) {
//            TestBlockQueue testBlockQueue = new TestBlockQueue();
//            threadPoolExecutor.submit(testBlockQueue::beginConsume);
//        }
//
//        Thread.sleep(1000);
//        while (true) {
//            QUEUE.put("abc");
//        }
//
//    }
//
//    public void beginConsume() {
//        while (true) {
//            try {
//                String key = QUEUE.take();
//                deal(key);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//
//    public void deal(String key) {
//        Object o = hotCache.getIfPresent(key);
//        if (o != null) {
//            return;
//        }
//        SlidingWindow slidingWindow = checkWindow(key);
//        //看看hot没
//        boolean hot = slidingWindow.addCount(1);
//
//        if (!hot) {
//            //如果没hot，重新put，cache会自动刷新过期时间
//            cache.put(key, slidingWindow);
//        } else {
//            hotCache.put(key, 1);
//            cache.invalidate(key);
//            System.out.println("key Hot " + System.currentTimeMillis() + " " + slidingWindow);
//        }
//    }
//
//    private SlidingWindow checkWindow(String key) {
//        //取该key的滑窗
//        SlidingWindow slidingWindow = (SlidingWindow) cache.getIfPresent(key);
//        //考虑在某个APP的rule变化后，清空该APP所有key
//        if (slidingWindow == null) {
//            //是个新key，获取它的规则
//            slidingWindow = new SlidingWindow(2, 20);
//
//        }
//        return slidingWindow;
//    }
//
//    /**
//     * 构建所有来的要缓存的key cache
//     */
//    public static Cache<String, Object> buildAllKeyCache() {
//        //老版本jdk1.8.0_20之前，caffeine默认的forkJoinPool在及其密集的淘汰过期时，会有forkJoinPool报错。建议用新版jdk
//        return Caffeine.newBuilder()
//                .initialCapacity(8192)//初始大小
//                .maximumSize(5000000)//最大数量
//                .expireAfterWrite(5, TimeUnit.SECONDS)//过期时间
//                .softValues()
//                .build();
//    }
//}
