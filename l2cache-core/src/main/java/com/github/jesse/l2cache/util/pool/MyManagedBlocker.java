package com.github.jesse.l2cache.util.pool;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * Java 8中的默认并行流使用公共ForkJoinPool，如果提交任务时公共池线程耗尽，会导致任务延迟执行。
 * <p>
 * CPU密集型：如果在ForkJoinPool中填充的任务，执行时间足够短，且CPU的可用能力足够，那么将不会出现上述延迟的问题。（ForkJoinPool的大多数使用场景）
 * I/O密集型：如果在ForkJoinPool中填充的任务，执行时间足够长，且是不受CPU限制的I/O任务，那么任务将延迟执行，并出现瓶颈。
 * 小结：ForkJoinPool 最适合的是CPU密集型的任务，如果存在 I/O，线程间同步，sleep() 等会造成线程长时间阻塞的情况时，最好配合使用 ManagedBlocker。
 * <p>
 * 对I/O阻塞型任务提供一个ManagedBlocker，让ForkJoinPool知道当前任务即将阻塞，因此需要创建新的`备用线程`来执行新提交的任务.
 * <p>
 * 【问题】通过ManagedBlocker来管理阻塞时，最大正在运行的线程数限制为32767，如果不限制新创建的线程数量，可能导致oom。如何控制ForkJoinPool中新创建的最大备用线程数？
 * 【分析】
 * 1、ForkJoinPool.common.commonMaxSpares 表示 tryCompensate 中`备用线程`创建的限制，默认为256
 * 2、上面这个参数，只能针对commonPool进行限制，并且tryCompensate方法不一定能会命中该限制，若未命中该限制，则可能无限制的创建`备用线程`来避免阻塞，最终还是可能出现oom
 * 3、ManagedBlocker将最大正在运行的线程数限制为32767.尝试创建大于最大数目的池导致IllegalArgumentException，只有当池被关闭或内部资源耗尽时，此实现才会拒绝提交的任务（即通过抛出RejectedExecutionException ）。
 * 【方案】
 * 在管理阻塞时，通过自定义 {@LimitedThreadForkJoinWorkerThreadFactory} 来限制ForkJoinPool最大可创建的线程数，并复用当前的ForkJoinPool的线程，以此来避免无限制的创建`备用线程`
 * <p>
 * TODO 暂未找到方法对ForkJoinPool的线程回收进行精确控制，因此废弃该类
 *
 * @author chenck
 * @date 2023/5/5 18:30
 */
@Deprecated
public class MyManagedBlocker implements ForkJoinPool.ManagedBlocker {
    private Function function;
    private Object key;
    private Object result;
    private boolean done = false;

    public MyManagedBlocker(Object key, Function function) {
        this.key = key;
        this.function = function;
    }


    @Override
    public boolean block() throws InterruptedException {
        // 当阻塞条件满足时，返回 true，否则返回 false
        done = true;

        // 执行阻塞操作，这可能是一个 IO 操作、等待锁等
        result = function.apply(key);

        // 返回 true 表示阻塞成功，返回 false 表示阻塞被中断
        return false;
    }

    @Override
    public boolean isReleasable() {
        // 判断是否可以释放阻塞
        // 返回 true，表示可以释放阻塞，返回 false，表示继续阻塞
        return done;
    }

    public Object getResult() {
        return result;
    }

}
