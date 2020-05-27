# Caffeine 简介
下面对`Caffeine`核心概念做一些简单介绍，若已了解，可忽略该章节。
```text
一、填充策略
1、手动加载：手动将值放入缓存。
2、同步加载：LoadingCache同步，Caffeine.build(LoadingCache)。
3、异步加载：AsyncLoadingCache异步，Caffeine.buildAsync(AsyncLoadingCache)。

二、驱逐策略
基于容量：当缓存大小超过配置的大小限制时会发生回收。
> 使用 Caffeine..maximumSize(10) 来启用。

基于权重：
> 使用 Caffeine..maximumWeight(10).weigher() 来启用。

基于时间：
> 1、Caffeine.expireAfterWrite：写入后到期策略。
> 2、Caffeine.expireAfterAccess: 访问后过期策略。
> 3、Caffeine.expireAfter：到期时间由 Expiry 实现独自计算。

基于引用：启用基于缓存键值的垃圾回收。
> 使用 Caffeine.weakKeys()、Caffeine.weakValues() 和 Caffeine.softValues() 来启用。 
> Java种有四种引用：强引用，软引用，弱引用和虚引用，caffeine可以将值封装成弱引用或软引用。
> 软引用：如果一个对象只具有软引用，则内存空间足够，垃圾回收器就不会回收它；如果内存空间不足了，就会回收这些对象的内存。
> 弱引用：在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。

三、自动刷新
1、Caffeine.refreshAfterWrite：自动刷新缓存。
> 结合CacheLoader来进行自动刷新，默认是同步进行的，可以通过buildAsync()方法进行异步构建。
> 注意：这里的刷新并不是到期就刷新，而是对这个数据再次访问时，才会刷新。
> 如果缓存项不存在，则阻塞至加载完成；如果缓存项可以刷新，则将返回一个旧值，并异步重新加载该值。

四、统计
> 使用 Caffeine.recordStats() 来启用。 
> 可以实时监控缓存当前的状态，以评估缓存的健康程度以及缓存命中率等，方便后续调整参数。

```

# 高效的缓存淘汰算法
注：从网上摘抄的。
缓存淘汰算法的作用是在有限的资源内，尽可能识别出哪些数据在短时间会被重复利用，从而提高缓存的命中率。常用的缓存淘汰算法有LRU、LFU、FIFO等。
```text
FIFO：first in first out。先进先出。选择最先进入的数据优先淘汰。
LRU：least recently used。最近最少使用。选择最近最少使用的数据优先淘汰。
LFU：least frequently used。最不经常使用。选择在一段时间内被使用次数最少的数据优先淘汰。
```

> LRU（Least Recently Used）算法认为最近访问过的数据将来被访问的几率也更高。
>
> LRU通常使用链表来实现，如果数据添加或者被访问到则把数据移动到链表的头部，链表的头部为热数据，链表的尾部如冷数据，当数据满时，淘汰尾部的数据。其实现比较简单，但是存在一些问题，如：当存在数据遍历时，会导致LRU命中率急剧下降，缓存污染情况比较严重。LRU算法也并非一无是处，其在突发流量下表现良好。

> LFU（Least Frequently Used）算法根据数据的历史访问频率来淘汰数据，其核心思想是“如果数据过去被访问多次，那么将来被访问的频率也更高”。
> 根据LFU的思想，如果想要实现这个算法，需要额外的一套存储用来存每个元素的访问次数，会造成内存资源的浪费。

Caffeine采用了一种结合LRU、LFU优点的算法：W-TinyLFU，其特点：高命中率、低内存占用。

其他说明：
> Caffeine的底层数据存储采用ConcurrentHashMap。因为Caffeine面向JDK8，在jdk8中ConcurrentHashMap增加了红黑树，在hash冲突严重时也能有良好的读性能。
> 
> Caffeine的缓存清除动作是触发式的，它可能发生在读、写请求后。这个动作在Caffeine中是异步执行的，默认执行的线程池是ForkJoinPool.commonPool()。相比较Guava cache的同步执行清除操作，Caffeine
的异步执行能够提高读写请求的效率。针对读写请求后的异步操作（清除缓存、调整LRU队列顺序等操作），Caffeine分别使用了ReadBuffer和WriterBuffer。使用Buffer一方面能够合并操作，另一方面可以避免锁争用的问题。
