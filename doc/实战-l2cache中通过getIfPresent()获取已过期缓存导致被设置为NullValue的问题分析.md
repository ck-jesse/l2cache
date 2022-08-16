# 问题
8点开售的会场不显示商品


# 日志
```text
2021-05-07 20:00:00,536 []-[]-[][ForkJoinPool.commonPool-worker-1] [com.github.l2cache.load.ValueLoaderWarpper] [WARN] (ValueLoaderWarpper.java:40)- [ValueLoaderWarpper] valueLoader is null, return null, cacheName=goodsSimpleListCache, key=105481
2021-05-07 20:00:00,536 []-[]-[][ForkJoinPool.commonPool-worker-1] [com.github.l2cache.cache.RedissonRBucketCache] [WARN] (RedissonRBucketCache.java:193)- [RedissonRBucketCache] redis and db load value both is null, not need to publish message, cacheName=goodsSimpleListCache, key=goodsSimpleListCache:105481, value=null
2021-05-07 20:00:00,537 []-[]-[][ForkJoinPool.commonPool-worker-1] [com.github.l2cache.cache.RedissonRBucketCache] [INFO] (RedissonRBucketCache.java:224)- [RedissonRBucketCache] put cache, cacheName=goodsSimpleListCache, expireTime=1800000 ms, key=goodsSimpleListCache:105481, value=NullValue, oldValue=null
2021-05-07 20:00:00,537 []-[]-[][ForkJoinPool.commonPool-worker-1] [com.github.l2cache.load.LoadFunction] [INFO] (LoadFunction.java:124)- [LoadFunction] NullValueCache put, cacheName=goodsSimpleListCache, key=105481, value=1
2021-05-07 20:00:00,537 []-[]-[][ForkJoinPool.commonPool-worker-1] [com.github.l2cache.cache.expire.DefaultCacheExpiredListener] [INFO] (DefaultCacheExpiredListener.java:30)- level1Cache evict expired cache, removalCause=REPLACED, cacheName=goodsSimpleListCache, key=105481, value=[GoodsSimpleListRespBO(goodsId=10774121, lowestPrice=71.50, discount=0.21), GoodsSimpleListRespBO(goodsId=11377598, lowestPrice=36.50, discount=0.20), GoodsSimpleListRespBO(goodsId=10390119, lowestPrice=236.70, discount=0.21), GoodsSimpleListRespBO(goodsId=11492659, lowestPrice=131.50, discount=0.14), GoodsSimpleListRespBO(goodsId=11492660, lowestPrice=107.00, discount=0.14), GoodsSimpleListRespBO(goodsId=11492661, lowestPrice=34.00, discount=0.14), GoodsSimpleListRespBO(goodsId=10767417, lowestPrice=132.40, discount=0.22), GoodsSimpleListRespBO(goodsId=10767410, lowestPrice=49.00, discount=0.14), GoodsSimpleListRespBO(goodsId=7628439, lowestPrice=97.00, discount=0.27), GoodsSimpleListRespBO(goodsId=7137915, lowestPrice=1559.40, discount=0.24), GoodsSimpleListRespBO(goodsId=5966219, lowestPrice=7407.00, discount=0.22)]
```


# 分析

## 阶段一：
1、通过上面的日志可知两个结论：
1）某个动作触发了LoadFunction去执行数据加载，但由于redis中缓存已过期且valueLoader为null，所以缓存了一个NullValue到本地缓存和redis中。
2）本地缓存中存在缓存数据，且该缓存已经过期。

2、由于日志出现断层无法分析出来具体哪个动作触发去执行数据加载，所以结合日志和代码来分析最有可能有问题的点

3、先排除掉可能出现加载缓存数据的动作（排除getOrLoad，refresh，自动刷新等方法调用场景)，

4、分析到此，这个问题断层了，此路不通，所以只能从别的角度来着手分析


## 阶段二
5、结合问题表象和日志可知：会场是8点整开售，并且上面的日志也刚好是8点整，所以大胆假设是这个业务维度导致的该问题，所以基于这个点继续深入分析

6、结合业务，我们将关注点放在首页接口中的batchGetOrLoad方法上，然后采用排除法并结合代码来分析该方法

7、通过分析代码逻辑，最大的疑点在caffeine.batchGet方法中调用的caffeine.getIfPresent这个方法上

8、原本对getIfPresent方法的理解是只是获取缓存，而不会触发加载缓存，那么假设调用该方法时，某种特殊场景下会导致触发加载缓存，如果假设成立那么该问题就解释的通了。

9、结合上面的日志分析可知：本地缓存中存在缓存数据，且该缓存已经过期。那么需要验证这种情况下调用caffeine.getIfPresent这个方法是否会触发执行cacheloader.load方法(本质是调用LoadFunction.apply方法来进行异步加载)。

10、如果会触发，那么当valueloader为null时就会出现设置NullValue到缓存的情况。

下面将进行验证，具体如下：

# 验证
## 验证 expireAfterWrite模式
```java
@Test
public void getIfPresentTest() throws InterruptedException{
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(5)
            .removalListener((key, value, cause) -> {
                System.out.println("[getIfPresentTestCache] remove removalCause={}, cacheName=" + cacheName + ", key=" + key + ", value=" + value);
            })
            .build(key -> {
                System.out.println("load value = 0");
                return 0;
            });

        Integer key1 = 1;
        cache.put(key1, 1);

        System.out.println("[缓存未过期] get key = " + key1 + ", value =" + cache.get(key1));
        Thread.sleep(5000);

        // expireAfterWrite模式下：获取已过期的缓存，这种情况下不会触发load
        System.out.println("[缓存已过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // expireAfterWrite模式下：获取未过期的缓存，这种情况下不会触发load
        System.out.println("[缓存未过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // expireAfterWrite模式下：获取不存在的缓存，这种情况下不会触发load
        Integer key2 = 2;
        System.out.println("[缓存不存在] getIfPresent key = " + key2 + ", value =" + cache.getIfPresent(key2));
        System.out.println();
}
```

- 输出结果
```text
[缓存未过期] get key = 1, value =1
[缓存已过期] getIfPresent key = 1, value =null
[缓存未过期] getIfPresent key = 1, value =null
[缓存不存在] getIfPresent key = 2, value =null
```

- 结论
```text
expireAfterWrite模式下：
1)获取已过期的缓存，这种情况下不会触发load
2)获取未过期的缓存，这种情况下不会触发load
3)获取不存在的缓存，这种情况下不会触发load
```

## 验证 refreshAfterWrite模式
```java
@Test
public void getIfPresentTest() throws InterruptedException{
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
            .refreshAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(5)
            .removalListener((key, value, cause) -> {
                System.out.println("[getIfPresentTestCache] remove removalCause={}, cacheName=" + cacheName + ", key=" + key + ", value=" + value);
            })
            .build(key -> {
                System.out.println("load value = 0");
                return 0;
            });

        Integer key1 = 1;
        cache.put(key1, 1);

        System.out.println("[缓存未过期] get key = " + key1 + ", value =" + cache.get(key1));
        Thread.sleep(5000);

        // refreshAfterWrite模式下：获取已过期的缓存，这种情况下会触发load
        System.out.println("[缓存已过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // refreshAfterWrite模式下：获取未过期的缓存，这种情况下不会触发load
        System.out.println("[缓存未过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // refreshAfterWrite模式下：获取不存在的缓存，这种情况下不会触发load
        Integer key2 = 2;
        System.out.println("[缓存不存在] getIfPresent key = " + key2 + ", value =" + cache.getIfPresent(key2));
        System.out.println();
}
```

- 输出结果
```text
[缓存未过期] get key = 1, value =1
[缓存已过期] getIfPresent key = 1, value =null
[缓存未过期] getIfPresent key = 1, value =null
[缓存不存在] getIfPresent key = 2, value =null
```

- 结论
```text
refreshAfterWrite模式下：
1)获取已过期的缓存，这种情况下会触发load【重点】
2)获取未过期的缓存，这种情况下不会触发load
3)获取不存在的缓存，这种情况下不会触发load
```

goodsSimpleListCache 缓存配置（实际缓存配置）
> goodsSimpleListCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats

分析至此，已完全确定该问题是由于caffeine.getIfPresent这个方法触发了load。

# 方案
修改缓存配置：采用 expireAfterWrite模式即可
> goodsSimpleListCache: initialCapacity=64,maximumSize=10000,expireAfterWrite=30d,recordStats
> 


# 总结
1、问题分析时一定要细心，不要放过细节信息
2、当分析出现断层无以为继时，换一个思路，然后大胆假设认真求证
