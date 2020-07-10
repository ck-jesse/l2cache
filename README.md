# L2cache  -- 基于内存和Redis的二级缓存框架

L2cache  是一个基于`内存`、 `Redis` 、 `Spring Cache` 实现的满足高并发场景下的分布式二级缓存框架。

**L2cache 的二级缓存结构：**

1、L1：一级缓存，内存缓存，`Caffeine` 和 `Guava Cache`。

2、L2：二级缓存，集中式缓存，支持`Redis`。

由于大量的缓存读取会导致 `L2` 的网络成为整个系统的瓶颈，因此 `L1` 的目标是降低对 `L2` 的读取次数。避免使用独立缓存系统所带来的网络IO开销问题。

`L2` 可以避免应用重启后导致的 `L1`数据丢失的问题，同时无需担心`L1`会增加太多的内存消耗，因为你可以设置 `L1`中缓存数据的数量。

**说明：**


> `L2cache` 在满足高并发的同时也引入了一些新的问题，比如怎么保证分布式场景下各个节点中本地缓存的一致性问题，本框架采用`数据变更通知`+`定期刷新过期缓存` 的策略来尽可能的保证缓存的一致性。具体见下文中的 `分布式缓存同步` 和 `分布式缓存一致性保证` 两个章节。
>
> `L2cache` 满足CAP定理中的AP，也就是满足可用性和分区容错性，至于C(一致性)因为缓存的特性所以无法做到强一致性，只能尽可能的去做到一致性，保证最终的一致。

**关键点：**

支持根据配置`缓存类型`来灵活的组合使用不同的Cache。

1、支持只使用一级缓存`Caffeine` 和 `Guava Cache`。

2、支持只使用二级缓存`Redis`。

3、支持同时使用一二级缓存`Composite`。

 **必知：**

> 若使用缓存，则必然可能出现不一致的情况，也就是说无法保证强一致性。



# 如何使用L2cache

### 集成Spring项目

#### 1、启动L2cache

- **方式一：Enable 启动模式**

引入jar包`l2cache-core`

```xml
<dependency>
    <groupId>com.coy.l2cache</groupId>
    <artifactId>l2cache-core</artifactId>
    <version>1.0</version>
</dependency>
```

在SpringBoot启动类上标注 `@EnableL2Cache`  启动`l2cache`。

```java
/**
 * 通过 Spring Enable 注解模式来启用二级缓存组件
 */
@EnableL2Cache
@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

- **方式二：starter 自动装载模式**

引入jar包 `l2cache-spring-boot-starter`
```xml
<dependency>
    <groupId>com.coy.l2cache</groupId>
    <artifactId>l2cache-spring-boot-starter</artifactId>
    <version>1.0</version>
</dependency>
```

#### 2、Spring配置

```yaml
spring:
  application:
    name: l2cache-example

# 二级缓存配置
l2cache:
  config:
    # 缓存实例Id，唯一标识应分布式场景下的一个缓存实例节点
    #instanceId: a1
    # 是否存储空值，默认true，防止缓存穿透
    allowNullValues: true
    # 缓存类型
    cacheType: composite
    # 组合缓存配置
    composite:
      # 一级缓存类型
      l1CacheType: caffeine
      # 二级缓存类型
      l2CacheType: redis
    # 一级缓存
    caffeine:
      # 是否构建异步Caffeine true 是 false 否
      asyncCache: false
      # 是否自动刷新过期缓存 true 是 false 否
      autoRefreshExpireCache: true
      # 缓存刷新调度线程池的大小
      refreshPoolSize: 1
      # 缓存刷新的频率(秒)
      refreshPeriod: 5
      # 高并发场景下建议使用refreshAfterWrite，在缓存过期后不会被回收，再次访问时会去刷新缓存，在新值没有加载完毕前，其他的线程访问始终返回旧值
      # Caffeine在缓存过期时默认只有一个线程去加载数据，配置了refreshAfterWrite后当大量请求过来时，可以确保其他用户快速获取响应。
      # 创建缓存的默认配置（完全与SpringCache中的Caffeine实现的配置一致）
      # 如果expireAfterWrite和expireAfterAccess同时存在，以expireAfterWrite为准。
      # 推荐用法：refreshAfterWrite 和 @Cacheable(sync=true)
      defaultSpec: initialCapacity=10,maximumSize=200,refreshAfterWrite=30s,recordStats
      # 设置指定缓存名的创建缓存配置(如：userCache为缓存名称)
      specs:
        userCache: initialCapacity=10,maximumSize=200,expireAfterWrite=30s
        userCacheSync: initialCapacity=10,maximumSize=200,refreshAfterWrite=30s,recordStats
    # 二级缓存
    redis:
      # 是否启用缓存Key prefix
      useKeyPrefix: true
      # 缓存Key prefix
      keyPrefix: ""
      # 缓存过期时间(ms)
      expireTime: 30000
      # 缓存最大空闲时间(ms)
      maxIdleTime: 30000
      # 最大缓存数
      maxSize: 200
      # Redisson 的yaml配置文件
      redissonYamlConfig: redisson.yaml
      # 缓存同步策略配置
    cacheSyncPolicy:
      # 策略类型
      type: redis
      # 缓存更新时通知其他节点的topic名称
      topic: l2cache
```

注：通过自定义`CacheLoader`结合到`Caffeine`或`Guava`的`LoadingCache`来实现数据加载。 



**关键点：**

支持根据配置来灵活的组合使用不同的Cache。

1、支持只使用一级缓存`Caffeine` 和 `Guava Cache`。

```yaml
l2cache:
  config:
    cacheType: caffeine
```

2、支持只使用二级缓存`Redis`。

```yaml
l2cache:
  config:
    cacheType: redis
```

3、支持同时使用一二级缓存。

```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
```



#### 3、代码中的使用

结合Spring Cache的注解来使用。

```java
/**
 * sync=true 则表示并发场景下同步加载缓存项，
 * 注：因底层是基于caffeine来实现一级缓存，所以利用的caffeine本身的同步机制来实现
 * <p>
 * 建议：设置@Cacheable的sync=true，可以利用Caffeine的特性，防止缓存击穿（同一个key和不同key的击穿）
 */
@Cacheable(value = "userCacheSync", key = "#userId", sync = true)
public List<User> queryUserSync(String userId) {
	List<User> list = new ArrayList();
	list.add(new User(userId, "addr1"));
	list.add(new User(userId, "addr2"));
	list.add(new User(userId, "-addr-addr-addr-addr-addr-addr"));
	logger.info("加载数据:{}", list);
	return list;
}
```



### 手动构建Cache

详细的构建方法参见如下单元测试类：

```
com.coy.l2cache.test.GuavaCacheTest
com.coy.l2cache.test.CaffeineCacheTest
com.coy.l2cache.test.RedisCacheTest
com.coy.l2cache.test.CompositeCacheTest
com.coy.l2cache.test.KafkaCacheSyncPolicyTest
```

下面列举`CaffeineCacheTest`中的一部分使用场景：

```java
CacheConfig cacheConfig = new CacheConfig();
CaffeineCache cache;
Callable<String> callable;

@before
public void before() {
    // 默认配置 CAFFEINE
    cacheConfig.setCacheType(CacheType.CAFFEINE.name())
        .setAllowNullValues(true)
        .getCaffeine()
.setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=2s,recordStats")
        .setAutoRefreshExpireCache(true)
        .setRefreshPoolSize(3)
        .setRefreshPeriod(5L);

    cacheConfig.getCacheSyncPolicy()
        .setType(CacheSyncPolicyType.REDIS.name());

    // 构建缓存同步策略
    CacheSyncPolicy cacheSyncPolicy = new RedisCacheSyncPolicy()
        .setCacheConfig(cacheConfig)
        .setCacheMessageListener(new CacheMessageListener(cacheConfig.getInstanceId()))
        .setActualClient(Redisson.create());
    cacheSyncPolicy.connnect();

    // 构建cache
    cache = (CaffeineCache) new CaffeineCacheBuilder()
        .setCacheConfig(cacheConfig)
        .setExpiredListener(new DefaultCacheExpiredListener())
        .setCacheSyncPolicy(cacheSyncPolicy)
        .build("localCache");

    callable = new Callable<String>() {
        AtomicInteger count = new AtomicInteger(1);

        @Override
        public String call() throws Exception {
            String result = "loader_value" + count.getAndAdd(1);
            System.out.println("loader value from valueLoader, return " + result);
            return result;
        }
    };

    System.out.println("cacheName: " + cache.getCacheName());
    System.out.println("level: " + cache.getCacheName());
    System.out.println("actualCache: " + cache.getActualCache().getClass().getName());
    System.out.println();
}

@Test
public void caffeineCacheTest() throws InterruptedException {
    String key = "key1";
    String value = "value1";
    cache.put(key, value);// 设置缓存项

    Object value1 = cache.get(key);// 获取缓存项
    System.out.println(String.format("get key=%s, value=%s", key, value1));

    String value = cache.get(key, callable);// 获取或设置缓存项
    System.out.println(String.format("get key=%s, value=%s", key, value));
}
```




# 关于缓存的几个常见问题分析和处理方案

### 分布式缓存同步

首先要搞清楚同步的目的，是为了尽可能保证分布式缓存的一致性。目前支持通过`Redis` 和 `Kafka` 的`发布订阅功能`来实现分布式缓存下不同节点的缓存同步。当然该框架留好扩展点，可以快速便捷的扩展其他MQ来实现缓存同步。

### 缓存更新

`缓存更新`包含了对`Caffeine` 和 `redis`的操作，同时会通知其他缓存节点进行`缓存更新`操作。

**1、主动更新**

> 1）获取缓存时，若缓存不存在或缓存已过期，则重新加载缓存。
>
> *2）源数据变更后，可调用`CacheManagerController.refresh(cacheName,key)`接口重新加载缓存（只对已存在的key重新加载）。
>  在重构后的版本中已经去掉`CacheManagerController`的实现，因为很少会有场景会使用到。

**2、自动更新**

> 通过`定期刷新过期缓存`（只对过期缓存进行重新加载），尽可能的保证分布式缓存的一致性。
>
> 每一个`cacheName`对应一个刷新任务，通过任务调度线程池实现调度。相比第一个版本，粒度更细。
> 
> 如果 `L1Cache` 是 `LoadingCache`，并且自定义`CuntomCacheLoader`中 `L2Cache` 不为空，则同时刷新`L1Cache`和`L2Cache`。详见`CaffeineCache`。
>


### 缓存淘汰

`缓存淘汰`包含了对`Caffeine` 和 `redis`的操作，同时会通知其他缓存节点进行`缓存淘汰`操作。

**1、主动淘汰**

> 1）获取缓存时去检查缓存是否过期，若过期则淘汰缓存。
>
> 2）结合`@CacheEvict`在源数据修改前或修改后，淘汰缓存。
> 
> 3）源数据变更后，可调用`CacheManagerController.clear(cacheName,key)`接口淘汰缓存。

**2、自动淘汰**

> *第一个版本`redis`中的缓存数据是利用`redis`的淘汰策略来管理的。具体可参考redis的6种淘汰策略。*
>
> 第二个版本是基于`redisson`实现，而其是通过`org.redisson.EvictionScheduler`实例来实现定期清理的，也就是`redis`中的缓存不设置过期时间，由应用自身来进行维护。


### 缓存预热

**1、手动预热**

> 直接调用标记了 `@Cacheable` 或 `CachePut` 注解的业务接口进行缓存的预热即可。

**2、自动预热** 

> 系统启动完毕后，自动调用业务接口将数据加载到缓存。
> 
>注：`缓存预热` 逻辑需要业务系统自行实现。



### 热点数据

**定义：**

> 缓存集群中的某个key瞬间被数万甚至十万的并发请求打爆。

**方案：**

1、采用本地缓存来缓解缓存集群和数据库集群的压力。本二级缓存框架可完全应对该场景。

2、应用层面做限流熔断保护，保护后面的缓存集群和数据库集群不被打死。

> 问：怎么保证`redis`中的数据都是热点数据?
>
> 当`redis`内存数据集上升到一定大小时，通过`redis`的淘汰策略来保证。通过`maxmemory`设置最大内存。


### 缓存雪崩

**定义：**

> 由于大量缓存失效，导致大量请求打到DB上，DB的CPU和内存压力巨大，从而出现一系列连锁反应，造成整个系统崩溃。

**方案：**

> `Caffeine`默认使用异步机制加载缓存数据，可有效防止缓存击穿（防止同一个key或不同key被击穿的场景）。注：结合`refreshAfterWrite` 异步刷新缓存，。

**预防：**

**缓存高可用**

> 缓存层设计成高可用，防止缓存大面积故障。例如 `Redis Sentinel` 和 `Redis Cluster` 都实现了高可用。

**缓存降级**

> 利用本地缓存，一定程度上保证服务的可用性(即使是有损的)。但主要还是通过对源服务的访问进行限流、熔断、降级等手段。

**提前演练**

> 建议项目上线前，演练缓存层宕机后，应用以及后端的负载情况以及可能出现的问题，对高可用提前预演，提前发现问题。


### 缓存击穿

**定义：**

> 在平常高并发的系统中，大量的请求同时查询一个 key 时，此时这个key正好失效了，就会导致大量的请求都打到数据库上面去。这种现象我们称为**缓存击穿**。
>
> 注：缓存击穿也可以理解为是热点数据的一种场景。

**方案：**

> `Caffeine`默认使用异步机制加载缓存数据，可有效防止缓存击穿（防止同一个key或不同key被击穿的场景）。

### 缓存穿透

**定义：**

> 请求根本就不存在的数据，也就是缓存和数据库都查询不到这条数据，但是请求每次都会打到数据库上面去。这种查询不存在数据的现象我们称为**缓存穿透**。

**方案：**

> 通过对不存在的key缓存空值，来防止缓存穿透。
>
> 注：也可以采用`BloomFilter`来对key进行过滤（暂未实现）。

> 注：对于高并发系统，可以结合 `Hystrix` 或 `Sentinel`来做应用级别的限流和降级，以保护下游系统不会被大量的请求给打死。


### 分布式缓存一致性保证
尽可能保证集群环境下各个节点中`L1`的一致性。

**缓存不一致场景分析：**

>1、请求走`节点A`获取数据key1，本地缓存和redis中无缓存，则从DB加载数据，并添加到本地缓存和`redis`。然后发送`redis`消息，通知其他节点。
> 
>2、请求走`节点B`获取数据key1，本地缓存无，`redis`中有，则添加到本地缓存
> 
> 3、请求走`节点A`获取数据key1，缓存过期，则从DB加载数据，并添加到本地缓存和`redis`。
>然后发送`redis`消息，通知`节点B`重新加载缓存key1，来保证不同节点的缓存一致性。

![levelcache](img/levelcache.png)

**描述：**


> 因为 `Caffeine` 在初始化时就指定了缓存过期时间，所以同一个缓存下的key的过期时间是固定的。那么`节点B`通过消息重新加载缓存到本地后，该key1在`节点B`的过期时间与在`节点A`上的过期时间是不一致的，实质表现是`节点A`的缓存key1已过期，但`节点B`的缓存key1未过期。
>假设后续的请求一直落在`节点B`上，也就会出现获取到过期缓存key1，这种现象的本质是缓存一致性问题，要怎么解决呢？

**分析：**

> 如果可以让`节点B`上的缓存key1在同一时间点10过期，那岂不是完美。
>

**方案：**

> 1）`节点B`在获取缓存key1时就设置过期时间点为10。
>
> 具体通过自定义`Caffeine`的`Expiry`来实现。
>
> 缺点：使用了自定义`Expiry`后，如果并发获取key1，那么只有一个线程会去加载数据，其他线程均会阻塞。
>
> 2）`节点A`上的key1在过期时通知`节点B`。
>
> 具体可以通过定时任务来刷新过期缓存。
>
> 缺点：该方案在时间窗口内会出现缓存不一致的情况。
>
> 注：本组件采用`redis发布订阅功能`和`定时刷新过期缓存`来尽可能保证缓存一致性。

### 定期刷新过期缓存的实现
本来是想通过`LoadingCache.refresh(key) `来刷新缓存，但`refresh()`不管key有没有过期都会重新加载数据，所以不合适；期望是只加载过期缓存，那么该怎么实现呢？ 经分析发现可以通过`LoadingCache.get(key)`来达到只对过期缓存重新加载的目的。


## 后记
> 经过一些途径发现市面上已经存在一些二级缓存的解决方案的实现，如：
>
> 1、Redisson PRO 支持的Spring缓存功能，其中 RedissonSpringLocalCachedCacheManager 支持本地缓存。
> 
> https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks#142-spring-cache
>
> 2、J2Cache 也是一个二级缓存的实现，经研究发现本组件与J2Cache的实现原理非常的类似。 
>
> https://gitee.com/ld/J2Cache
>

若提前知道有这两种二级缓存实现，很大可能会偷懒不去实现本组件，毕竟没必要重复造轮子嘛；
从另一方面来看，自己实现该二级缓存组件也是有很多好处的，一个是验证了自己的思路是对的，另一个是对二级缓存的本质和在研发过程中碰到问题的思考和解决，不是直接拿来使用所能比的。