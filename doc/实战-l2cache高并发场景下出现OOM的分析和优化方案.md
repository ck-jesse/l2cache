# 概要：

为应对双十二活动，现对电商平台核心主链路进行压测，在压测过程中商品中心出现OOM。

压测环境的基本情况（只列出部分数据）：
1、商品中心：8核16G，120个pod
2、redis配置：192G读写分离版（32节点），单片带宽：192M
3、DB配置：PolarDB顶配（88核 710G，1主6从）
4、商品中心集成 l2cache 缓存框架

# 问题

goods-rest在压测时出现OOM，具体错误日志如下：



![1606969776073](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606969776073.png)



一级缓存：caffeine

二级缓存：redis



# 分析

## 1、通过上面的日志可得知，具体的错误点为：

```
com.github.benmanes.caffeine.cache.LocalLoadingCache.java:166

Exception thrown during refresh
```

## 2、分析caffeine源码 LocalLoadingCache

```java
interface LocalLoadingCache<K, V> extends LocalManualCache<K, V>, LoadingCache<K, V> {
  // 省略无关代码 ...

  // refresh 方法的作用：异步加载key的新值。加载新值时，get（key）将继续返回以前的值（如果有），除非它被逐出
  default void refresh(K key) {
    requireNonNull(key);

    long[] writeTime = new long[1];
    long startTime = cache().statsTicker().read();
    V oldValue = cache().getIfPresentQuietly(key, writeTime);
    CompletableFuture<V> refreshFuture = (oldValue == null)
        ? cacheLoader().asyncLoad(key, cache().executor())
        : cacheLoader().asyncReload(key, oldValue, cache().executor());
    refreshFuture.whenComplete((newValue, error) -> {
      long loadTime = cache().statsTicker().read() - startTime;
      if (error != null) {
        // 关键的错误日志在这里
        logger.log(Level.WARNING, "Exception thrown during refresh", error);
        cache().statsCounter().recordLoadFailure(loadTime);
        return;
      }
  	// 省略无关代码 ...
  }
  // 省略无关代码 ...
}
```

通过上面的源码可知，关键的方法在 LocalLoadingCache.refresh()，所以继续排查l2cache里面有调用该方法的代码。

## 3、排查l2cache里面调用LocalLoadingCache.refresh()的代码

通过分析，发现有两处代码有调用，如下：

```
CaffeineCache#refreshAll
CaffeineCache#refresh
```

## 4、继续分析，上面两个方法在哪里被调用，如下：

```
# 该方法没有被调用的地方，所以可以忽略
CaffeineCache#refreshAll
```

```java
public class CaffeineCache extends AbstractAdaptingCache implements Level1Cache {
    // 该方法被CacheMessageListener.onMessage方法调用
    @Override
    public void refresh(Object key) {
        if (isLoadingCache()) {
            logger.debug("[CaffeineCache] refresh cache, cacheName={}, key={}", this.getCacheName(), key);
            ((LoadingCache) caffeineCache).refresh(key);
        }
    }
}
```

## 5、CacheMessageListene缓存监听器，作用是在集群环境下通过监听方式来保证缓存的一致性。

```java
public class CacheMessageListener implements MessageListener {

    @Override
    public void onMessage(CacheMessage message) {
        try {
            if (this.cacheInstanceId.equalsIgnoreCase(message.getInstanceId())) {
                logger.debug("[CacheMessageListener][SyncCache] instanceId is same no need to deal, message={}", message.toString());
                return;
            }
            logger.info("[CacheMessageListener][SyncCache] instanceId={}, cacheName={}, cacheType={}, optType={}, key={}",
                    message.getInstanceId(), message.getCacheName(), message.getCacheType(), message.getOptType(), message.getKey());

            Level1Cache level1Cache = getLevel1Cache(message);
            if (null == level1Cache) {
                return;
            }
            // 关键在这里
            if (CacheConsts.CACHE_REFRESH.equals(message.getOptType())) {
                level1Cache.refresh(message.getKey());
            } else {
                level1Cache.clearLocalCache(message.getKey());
            }
        } catch (Exception e) {
            logger.error("[CacheMessageListener][SyncCache] error", e);
        }
    }

}
```

通过上面的代码分析，已经定位到具体的代码，也可以基本判断该OOM问题是该场景下出现的。



## 6、回过头结合 CacheMessageListener 的日志继续分析

### 1）所有缓存维度的 refresh 消息日志数量

```
通过下图可以发现 2020-12-02 17:00 ~ 2020-12-03 23:00 缓存监听器的refresh消息操作日志，有13,916,848,020条（139亿）。这个数量已经非常恐怖啦，说明此处有问题。
```



![1606971342869](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606971342869.png)



### 2）单个缓存维度的 refresh 消息日志数量

```
通过下图可以发现 2020-12-02 17:00 ~ 2020-12-03 23:00 缓存监听器的在单个缓存维度cacheName=actDiscountCache 的refresh消息操作日志有12,820,529,131条（128亿）。
这么大的refresh消息量，说明这个缓存维度有问题。
```



![1606971540484](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606971540484.png)



### 3）单个key维度的 refresh 消息日志数量

```
* and CacheMessageListener and refresh and actDiscountCache and 56781
key=56781的refresh操作日志有6,358,694,099条（63亿）。
```



![1606971713317](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606971713317.png)



### 4）单个key维度的 put 日志数量

```
* and RedissonRBucketCache and put and actDiscountCache and 56781
key=56781的put NullValue 到redis的操作日志有67,466,579条（0.67亿）。由下入可知该key的值为NullValue。
```



![1606971955110](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606971955110.png)



那么问题来了，为什么会有这么多refresh日志和put日志呢？

1）为什么同一个key有大量的put日志？

压测环境有120个pod，在高并发的情况下，获取同一个key的并发请求被打到这120个pod上，当redis中没有数据时，会从DB加载数据并put到redis中，然后发送一个refresh消息（短时间内可以理解为是重复的refresh消息）。以上就解释了同一个key为什么会有这么多的put日志。

理论上来讲，这种并发加载同一个key的情况，是需要对同一个key来加分布式锁处理的，否则请求会打到下游DB上，导致DB的压力增加，但是由于目前DB是阿里云PolarDB顶配，所以结合实际情况，在不影响用户体验且DB能支撑的情况下不加分布式锁。



2）为什么同一个key有大量的refresh日志？

压测环境有120个pod，每一个put日志会发送一个缓存刷新消息，这个缓存刷新消息会被其他119个pod消费，并执行refresh操作，结合上面的日志分析，可初步算出refresh日志数量=put日志数量(0.67亿) * 120=8,095,989,480（80亿+）。

从上面的分析可知，并发场景下同一个key存在大量的重复refresh消息。

> 注：这个值不是那么精准，是一个理论值，实际可能会更多。因为消息处理过程中如果redis缓存过期，那么还是会继续加载继续发送。





# 总结

通过上面的分析可得知问题关键点如下

1、高并发情况下，在加载同一个key的缓存数据时，会触发发送refresh消息，而每个refresh消息会被其他120个pod消费，所以每个pod都会受到大量重复的refresh消息。

2、在收到大量重复refresh消息时，每个消息都会往caffeine的默认线程池 ForkJoinPool 的无界队列中新增一个任务，而caffeine的refresh()方法是异步执行的，所以在 消费速度 跟不上 生产速度 的情况下，会导致队列中堆积大量消息，最终导致OOM，这个也就是这次OOM的根本原因所在。



# 优化

那么针对上面总结的问题该怎么处理呢？目前有两个初步的方案 :



## 1、发送消息侧的优化：

从源头控制发送refresh消息的量，限制集群环境下单位时间内同一个key只能发送一次refresh消息。具体优化如下：

1）发送消息优化为异步

2）对同一个key加分布式锁，保证500ms内同一个key只会发送一次refresh消息。



## 2、消费消息侧的优化：

从消费侧单pod维度过滤掉重复的refresh消息，去掉不必要的refresh操作。具体优化如下：

1）基于 `ValueLoaderWarpper` 定义一个类型为 `AtomicInteger` 的 `waitRefreshNum` 字段（默认值为0），该字段用于过滤并发执行同一个key的refresh操作，保证同一个key只有一个refresh操作在执行。

2）消费消息后，通过key获取对应的 `ValueLoaderWarpper` 。

3）执行 `waitRefreshNum.getAndIncrement()`，并发的原子操作。

4）如果返回值>0，则表示重复消息，直接返回。也就是说当前key存在正在执行的refresh任务，或者队列中存在等待执行的refresh任务。此处是关键，高并发情况下可过滤掉大量重复消息。

5）如果返回值=0，则执行LoadingCache.refresh()。也就是说当前key无正在执行的refresh任务，且队列中无等待执行的refresh任务。

6） `ValueLoaderWarpper`加载数据完毕后，将`waitRefreshNum` 设置为0，保证该key下次可继续执行refresh。



至此，通过上面两个维度的优化方案来解决 ForkJoinPool 的队列堆积大量refresh任务导致OOM的问题。



