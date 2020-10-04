# 实战-l2cache-caffeine之OOM异常分析

## 场景

1、电商平台，有几千万的用户量。

2、下单时，会调用用户服务获取用户信息

3、用户服务使用l2cache二级缓存（caffeine+redis）来缓存用户信息

4、本地缓存caffeine的最大元素大小设置为5000，过期时间为30分钟


## 问题
通过阿里云ARMS监控发现，用户服务存在频繁FullGC和YoungGC的情况，最终导致OOM。

## 分析

结合业务场景进行分析，发现用户维度的本地缓存的命中率非常低，相当于是每个用户请求都会打到redis或db上。
并且当本地缓存达到5000个元素的限制，这时若有大量不同用户请求，会触发caffeine的最大元素淘汰机制（异步），若未及时淘汰，经历过几次YoungGC后，缓存对象从Young区转移到Old区，导致Old区飙升，出现频繁的FullGC和YoungGC，最终导致OOM。


## 方案

1、直接使用Redis作为缓存即可，无需本地缓存。
```yaml
l2cache:
  config:
    cacheType: redis
```

2、本地缓存不适用于数据量大且命中率极低的缓存场景，如用户维度缓存。

## 总结

1、l2cache 二级缓存并不是一个万能药

- 适用场景：缓存项命中率较高的业务场景
> 
> 举例：如商品维度缓存，因为不同用户看到的是同一个商品，所以命中率高。
>
- 不适用场景：缓存项命中率低的业务场景
> 
> 举例：如用户维度缓存，因为不同用户只能获取自己的用户信息，所以命中率低。

2、一定要结合业务场景来分析是否需要用到二级缓存


## 思考：

为什么caffeine超过最大元素限制后，缓存项没有被及时淘汰掉，而是进入到了Old区，最终导致频繁的GC和OOM出现呢？

- 猜想：

1、需要被淘汰的缓存项是通过异步的方式去清理的

2、为了提高资源利用率，通过线程池来进行异步清理

3、达到最大元素限制后，若有大量不能命中缓存的请求，则会加载数据并put到缓存后，会产生大量缓存项淘汰任务（基于大小的过期）

4、在线程池处理不过来淘汰任务的情况，会出现缓存项堆积，最终导致出现频繁的GC，甚至OOM

- 分析

这种问题一般很难百度到，所以结合百度和caffeine源码来进行分析。

com.github.benmanes.caffeine.cache.LocalAsyncLoadingCache.get(key)
> 以 LoadingCache.get(key) 为入口进行分析

com.github.benmanes.caffeine.cache.BoundedLocalCache.replace(key, value)
> 替换缓存值

com.github.benmanes.caffeine.cache.BoundedLocalCache.afterWrite(Runnable task)
> 缓存值写入后的处理

com.github.benmanes.caffeine.cache.BoundedLocalCache.scheduleDrainBuffers()
> 尝试执行一个异步任务PerformCleanupTask，以将挂起的操作采用替换策略。
>
> executor.execute(drainBuffersTask); 
> // 此处可证明通过线程池的方式来进行任务处理。
>
> 如果执行器拒绝任务，则直接运行 maintenance(Runnable)。
> // 此处可证明超过任务等待队列大小后，会进行补偿处理，同时可证明堆积了大量清理任务，也证明了超过缓存大小限制后有大量缓存没有被及时清理掉。

com.github.benmanes.caffeine.cache.BoundedLocalCache.PerformCleanupTask
> 执行清理任务。此类可证明通过异步任务的形式来处理。
>

com.github.benmanes.caffeine.cache.BoundedLocalCache.performCleanUp(Runnable task)
> 由于执行器的所有线程都很忙，维护任务可能已被调度但未运行。如果所有线程都在写入缓存，那么没有帮助就无法取得任何进展
> 
> 实际调用 maintenance(Runnable)

com.github.benmanes.caffeine.cache.BoundedLocalCache.maintenance(Runnable task)
> 任务执行体。清空读取缓冲区、写入缓冲区和引用队列，然后是过期和基于大小的逐出

com.github.benmanes.caffeine.cache.BoundedLocalCache.evictEntries()
> 如果缓存超过最大值，则逐出条目

com.github.benmanes.caffeine.cache.BoundedLocalCache.PerformCleanupTask
> 执行清理任务
>

- 结论
 
 通过上面的分析，可以发现我们的猜想全部得到证实。由此我们可以得出如下结论：
 
 使用 caffeine 缓存命中率低的缓存项时，在超过缓存大小限制后，若还有大量请求通过 caffeine 获取值，那么可能会出现缓存项没有被及时清理掉的情况，最终导致频繁的GC，甚至OOM。
 
 所以，caffeine 不适用于数据量大，并且缓存命中率极低的业务场景，如用户维度的缓存。