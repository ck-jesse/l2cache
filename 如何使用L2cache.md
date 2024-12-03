# 一、集成Spring项目

> 已部署到中央仓库，最新版本号见： [l2cache mvnrepository](https://mvnrepository.com/artifact/io.github.ck-jesse/l2cache-core)
> 
> 1.x 版本，支持jdk1.8
>
> 2.x 版本，支持jdk17


## 1、启动L2cache


- **starter 自动装载模式（推荐）**

引入jar包 `l2cache-spring-boot-starter` 即可
```xml
<dependency>
    <groupId>io.github.ck-jesse</groupId>
    <artifactId>l2cache-spring-boot-starter</artifactId>
    <version>x.x.x</version>
</dependency>
```

- 常见问题：在项目中引入l2cache后，项目启动失败。
- 原因分析：由于项目和l2cache中依赖的spring相关包版本不一致，导致启动失败。
- 解决方案：建议排除掉l2cache中的依赖，使用项目中的版本。

```xml
<dependency>
    <groupId>io.github.ck-jesse</groupId>
    <artifactId>l2cache-spring-boot-starter</artifactId>
    <version>x.x.x</version>
    <exclusions>
        <exclusion>
            <artifactId>spring-boot-starter-web</artifactId>
            <groupId>org.springframework.boot</groupId>
        </exclusion>
        <exclusion>
            <artifactId>spring-cloud-context</artifactId>
            <groupId>org.springframework.cloud</groupId>
        </exclusion>
    </exclusions>
</dependency>
```


## 2、Spring配置
具体代码示例参考：[l2cache](https://github.com/ck-jesse/l2cache) 中的 `l2cache-example` 模块。

2.0.0 版本，及之后版本的配置方式，最新配置见：[application.yml](https://github.com/ck-jesse/l2cache/blob/master/l2cache-example/src/main/resources/application.yml)
```yaml
spring:
  application:
    name: l2cache-example

# ======================================================================= #
# 从2.0.0版本开始，支持不同缓存维度的缓存类型配置（可按需配置）
# ======================================================================= #

# 二级缓存配置
# 注：caffeine 不适用于数据量大，并且缓存命中率极低的业务场景，如用户维度的缓存。请慎重选择。
l2cache:
  config:
    # Redisson 的yaml配置文件
    redissonYamlConfig: redisson.yaml
    # 默认缓存配置
    defaultConfig:
      # 是否存储空值，默认true，防止缓存穿透
      allowNullValues: true
      # 空值过期时间，单位秒
      nullValueExpireTimeSeconds: 30
      # 是否使用一级缓存的过期时间来替换二级缓存的过期时间，默认true，简化缓存配置
      useL1ReplaceL2ExpireTime: true
      # 缓存类型
      cacheType: COMPOSITE
      # 组合缓存配置
      composite:
        # 一级缓存类型
        l1CacheType: caffeine
        # 二级缓存类型
        l2CacheType: redis
        # 二级缓存是否通过batchPut()来处理缓存数据，默认false
        l2BatchPut: true
        # 二级缓存是否通过batchEvict()来处理缓存数据，默认false
        l2BatchEvict: true
        # 是否全部启用一级缓存，默认false
        l1AllOpen: false
        # 是否手动启用一级缓存，默认false
        l1Manual: true
        # 手动配置走一级缓存的缓存key集合，针对单个key维度
        l1ManualKeySet:
          - userCache:user01
          - userCache:user02
          - userCache:user03
          - userCache:user04
        # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
        l1ManualCacheNameSet:
          - newBrandCache
          - newGoodsPriceRevisionCache
      # 一级缓存
      caffeine:
        # 创建缓存的默认配置（格式完全与Caffeine配置一致）
        defaultSpec: initialCapacity=10,maximumSize=2,refreshAfterWrite=30m,softValues,recordStats
        # 设置指定缓存名的创建缓存配置(如：userCache为缓存名称)
        specs:
          userCache: initialCapacity=10,maximumSize=200,refreshAfterWrite=2m,recordStats
          newBrandCache: initialCapacity=64,maximumSize=5000,refreshAfterWrite=2h,recordStats
          newGoodsPriceRevisionCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=1d,recordStats
          # cacheName中含有: / * 等特殊字符，需要加 "[ ]"
          "[userCache:v1]": initialCapacity=64,maximumSize=10000,refreshAfterWrite=60m,recordStats
      # 二级缓存
      redis:
        # 加载数据时，是否加锁，默认false
        lock: false
        # 加锁时，true调用tryLock()，false调用lock()
        tryLock: true
        # 批量操作的大小，可以理解为是分页，默认50
        batchPageSize: 3
        # 默认缓存过期时间(ms)
        expireTime: 86400000
        # 针对cacheName维度的过期时间集合，单位ms
        expireTimeCacheNameMap:
          brandCache: 86400000
    # 缓存配置集合（针对cacheName的个性化缓存配置），按需配置
    configMap:
      # 样例：指定某个缓存维度走caffeine
      brandCache:
        # 缓存类型
        cacheType: caffeine
        # 一级缓存
        caffeine:
          # 创建缓存的默认配置
          defaultSpec: initialCapacity=10,maximumSize=2,refreshAfterWrite=30m,softValues,recordStats
      # 样例：指定某个缓存维度走redis
      goodsPriceRevisionCache:
        # 缓存类型
        cacheType: redis
        # 二级缓存
        redis:
          # 加载数据时，是否加锁
          lock: false
          # 加锁时，true调用tryLock()，false调用lock()
          tryLock: true
          # 批量操作的大小，可以理解为是分页
          batchPageSize: 3
          # 默认缓存过期时间(ms)
          expireTime: 86400000
          # 针对cacheName维度的过期时间集合，单位ms
          expireTimeCacheNameMap:
            brandCache: 86400000
    # 缓存同步策略配置
    cacheSyncPolicy:
      # 策略类型 kafka / redis，推荐使用redis
      type: redis
      # 缓存更新时通知其他节点的topic名称
      topic: l2cache
      # 具体的属性配置，不同的类型配置各自的属性即可(自定义和原生的都可以)
      #props:
      #  # kafka properties config
      #  bootstrap.servers: localhost:9092
      #  # 生产者id
      #  client.id: L2CacheProducer
      #  # 发送消息的确认机制
      #  acks: 1
      #  # key序列化处理器
      #  key.serializer: org.apache.kafka.common.serialization.StringSerializer
      #  # value序列化处理器
      #  value.serializer: org.apache.kafka.common.serialization.StringSerializer
      #  # 消费者groupid
      #  # 因为是缓存同步，所以必须让所有消费者都消费到相同的消息。采用动态生成一个id附加到配置的group.id上，实现每个consumer都是一个group，来实现发布订阅的模式。
      #  group.id: L2CacheConsumerGroup
      #  # 自动提交offset（默认true）
      #  enable.auto.commit: true
      #  # 自动提交间隔
      #  auto.commit.interval.ms: 1000
      #  # key反序列化处理器
      #  key.deserializer: org.apache.kafka.common.serialization.StringDeserializer
      #  # value反序列化处理器
      #  value.deserializer: org.apache.kafka.common.serialization.StringDeserializer
      #  # 设置消费的位置
      #  auto.offset.reset: latest
      #  # 设置一次最大拉取的消息条数（默认500）
      #  max.poll.records: 10
      #  # 设置poll最大时间间隔（默认3s）
      #  max.poll.interval.ms: 3000
    # 热key探测
    hotkey:
      # 热key探测类型,支持 none、jd、sentinel，目前 sentinel 仅支持单机，默认为 none，不启用热key探测。
      type: sentinel
      sentinel:
        # 若配置了默认规则，针对所有的cacheName，生成其默认的热点参数规则，简化配置
        # 若未配置默认规则，则仅针对 rules 中的配置进行热点参数探测
        # 注：规则具体的配置是针对ParamFlowRule的配置
        default-rule:
          grade: 1
          param-idx: 0
          count: 6
          durationInSec: 1
        rules:
          # 案例1
          # resourceName 资源名称，通常设置为缓存名称
          - resource: newBrandCache
            # 流量控制的阈值类型，0表示线程数，1表示qps，默认1
            grade: 1
            # 参数下标
            paramIdx: 0
            # 阈值计数
            count: 3
            # 统计窗口时间长度（单位为秒），默认为1
            durationInSec: 5
          # 案例2
          - resource: newGoodsPriceRevisionCache
            count: 5
      # jd:
      # serviceName: weeget-bullet-goods-rest
      # #etcd的地址，如有多个用逗号分隔
      # etcdUrl: http://127.0.0.1:2379
```

1.0.39 版本，及之前版本的配置方式，最新配置见：[application-1.0.39.yml](https://github.com/ck-jesse/l2cache/blob/master/l2cache-example/src/main/resources/application-1.0.39.yml)
```yaml

spring:
  application:
    name: l2cache-example

# ======================================================================= #
# 1.0.39版本及之前的版本，一个应用只能配置一个缓存类型，不支持针对不同缓存维度的按需配置
# ======================================================================= #

# 二级缓存配置
# 注：caffeine 不适用于数据量大，并且缓存命中率极低的业务场景，如用户维度的缓存。请慎重选择。
l2cache:
  config:
    # 缓存实例Id，唯一标识应分布式场景下的一个缓存实例节点
    #instanceId: a1
    # 是否存储空值，默认true，防止缓存穿透
    allowNullValues: true
    # 空值过期时间，单位秒
    nullValueExpireTimeSeconds: 30
    # 是否使用一级缓存的过期时间来替换二级缓存的过期时间，默认true，简化缓存配置
    useL1ReplaceL2ExpireTime: true
    # 缓存类型
    cacheType: COMPOSITE
    # 组合缓存配置
    composite:
      # 一级缓存类型
      l1CacheType: caffeine
      # 二级缓存类型
      l2CacheType: redis
      # 二级缓存是否通过batchPut()来处理缓存数据，默认false
      l2BatchPut: true
      # 二级缓存是否通过batchEvict()来处理缓存数据，默认false
      l2BatchEvict: true
      # 是否全部启用一级缓存，默认false
      l1AllOpen: false
      # 是否手动启用一级缓存，默认false
      l1Manual: true
      # 手动配置走一级缓存的缓存key集合，针对单个key维度
      l1ManualKeySet:
        - a
        - actLimitMarkupCache:11_2
        - userCache:user01
        - userCache:user02
        - userCache:user03
        - userCache:user04
        - "[actLimitMarkupCache:11_3]"
      # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
      l1ManualCacheNameSet:
        - compositeCache
        - goodsSpecCache
        - goodsPriceRevisionCache
        - brandCache
        #- newBrandCache
        #- newGoodsPriceRevisionCache
    # 一级缓存
    caffeine:
      # 是否自动刷新过期缓存 true 是 false 否
      autoRefreshExpireCache: false
      # 缓存刷新调度线程池的大小
      refreshPoolSize: 2
      # 缓存刷新的频率(秒)
      refreshPeriod: 10
      # 高并发场景下建议使用refreshAfterWrite，在缓存过期后不会被回收，再次访问时会去刷新缓存，在新值没有加载完毕前，其他的线程访问始终返回旧值
      # Caffeine在缓存过期时默认只有一个线程去加载数据，配置了refreshAfterWrite后当大量请求过来时，可以确保其他用户快速获取响应。
      # 创建缓存的默认配置（完全与SpringCache中的Caffeine实现的配置一致）
      # 如果expireAfterWrite和expireAfterAccess同时存在，以expireAfterWrite为准。
      # 最新最优的推荐用法：refreshAfterWrite 和 实现CacheService接口，只需实现4个业务相关的方法即可（缓存操作提炼到上层接口中去实现）
      # 基于注解的推荐用法：refreshAfterWrite 和 实现CacheService接口，并定义@Cacheable(sync=true)
      # 注意：当缓存项在有效期内重复利用率很低时，不适合用本地缓存，如3千万用户参加抢购活动，用户信息的缓存，则不能用本地缓存。
      # 因为超过最大数量限制时，再往里面添加元素，会异步按照LFU淘汰元素，若未及时淘汰，在大量用户请求的情况，会导致堆内存飙升，频繁的FullGC和YoungGC，最终导致OOM。
      defaultSpec: initialCapacity=10,maximumSize=2,refreshAfterWrite=30m,softValues,recordStats
      # 设置指定缓存名的创建缓存配置(如：userCache为缓存名称)
      specs:
        #userCache: initialCapacity=10,maximumSize=2,expireAfterWrite=2m
        userCache: initialCapacity=10,maximumSize=200,refreshAfterWrite=2m,recordStats
        userCacheSync: initialCapacity=10,maximumSize=200,refreshAfterWrite=2m,recordStats
        # 无过期时间配置
        queryUserSync: initialCapacity=10,maximumSize=20,refreshAfterWrite=1m,recordStats
        brandCache: initialCapacity=64,maximumSize=5000,refreshAfterWrite=2h,recordStats
        newBrandCache: initialCapacity=64,maximumSize=5000,refreshAfterWrite=2h,recordStats
        timeCache: initialCapacity=64,maximumSize=5000,refreshAfterWrite=30d,recordStats
        homeGoodsGroupPageCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats
        homeGoodsGroupCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats
        purchaseWouldCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats
        goodsPriceRevisionCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats
        actDiscountCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=30d,recordStats
        brandParentFirstPushCache: initialCapacity=64,maximumSize=10000,refreshAfterWrite=60m,recordStats
        # cacheName中含有: / * 等特殊字符，需要加 "[ ]"
        "[userCache:v1]": initialCapacity=64,maximumSize=10000,refreshAfterWrite=60m,recordStats
    # 二级缓存
    redis:
      # 加载数据时，是否加锁
      lock: false
      # 加锁时，true调用tryLock()，false调用lock()
      tryLock: true
      # 批量操作的大小，可以理解为是分页
      batchPageSize: 3
      # 默认缓存过期时间(ms)
      expireTime: 86400000
      # 针对cacheName维度的过期时间集合，单位ms
      expireTimeCacheNameMap:
        brandCache: 86400000
      # Redisson 的yaml配置文件
      redissonYamlConfig: redisson.yaml
    # 缓存同步策略配置
    cacheSyncPolicy:
      # 策略类型 kafka / redis，推荐使用redis
      type: redis
      # 缓存更新时通知其他节点的topic名称
      topic: l2cache
      # 具体的属性配置，不同的类型配置各自的属性即可(自定义和原生的都可以)
      #props:
      #  # kafka properties config
      #  bootstrap.servers: localhost:9092
      #  # 生产者id
      #  client.id: L2CacheProducer
      #  # 发送消息的确认机制
      #  acks: 1
      #  # key序列化处理器
      #  key.serializer: org.apache.kafka.common.serialization.StringSerializer
      #  # value序列化处理器
      #  value.serializer: org.apache.kafka.common.serialization.StringSerializer
      #  # 消费者groupid
      #  # 因为是缓存同步，所以必须让所有消费者都消费到相同的消息。采用动态生成一个id附加到配置的group.id上，实现每个consumer都是一个group，来实现发布订阅的模式。
      #  group.id: L2CacheConsumerGroup
      #  # 自动提交offset（默认true）
      #  enable.auto.commit: true
      #  # 自动提交间隔
      #  auto.commit.interval.ms: 1000
      #  # key反序列化处理器
      #  key.deserializer: org.apache.kafka.common.serialization.StringDeserializer
      #  # value反序列化处理器
      #  value.deserializer: org.apache.kafka.common.serialization.StringDeserializer
      #  # 设置消费的位置
      #  auto.offset.reset: latest
      #  # 设置一次最大拉取的消息条数（默认500）
      #  max.poll.records: 10
      #  # 设置poll最大时间间隔（默认3s）
      #  max.poll.interval.ms: 3000
    # 热key探测
    hotkey:
      # 热key探测类型,支持 none、jd、sentinel，目前 sentinel 仅支持单机，默认为 none，不启用热key探测。
      type: sentinel
      sentinel:
        # 若配置了默认规则，针对所有的cacheName，生成其默认的热点参数规则，简化配置
        # 若未配置默认规则，则仅针对 rules 中的配置进行热点参数探测
        # 注：规则具体的配置是针对ParamFlowRule的配置
        default-rule:
          grade: 1
          param-idx: 0
          count: 6
          durationInSec: 1
        rules:
          # 案例1
          # resourceName 资源名称，通常设置为缓存名称
          - resource: newBrandCache
            # 流量控制的阈值类型，0表示线程数，1表示qps，默认1
            grade: 1
            # 参数下标
            paramIdx: 0
            # 阈值计数
            count: 3
            # 统计窗口时间长度（单位为秒），默认为1
            durationInSec: 5
          # 案例2
          - resource: newGoodsPriceRevisionCache
            count: 5
      # jd:
      # serviceName: weeget-bullet-goods-rest
      # #etcd的地址，如有多个用逗号分隔
      # etcdUrl: http://127.0.0.1:2379

```

注：

1、通过自定义`CacheLoader`结合到`Caffeine`或`Guava`的`LoadingCache`来实现数据加载。

2、建议所有缓存都设置过期时间，如果有些缓存维度可以是永久，那么也建议将过期时间设置长一些即可。



**关键点：** 支持根据配置来灵活的组合使用不同的Cache，具体如下。

### （1）、支持只使用一级缓存`Caffeine` 和 `Guava Cache`。

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: caffeine
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: caffeine
```

### （2）、支持只使用二级缓存`Redis`。

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: redis
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: redis
```

### （3）、支持同时使用一二级缓存【推荐】
推荐该方式，因为可动态配置缓存是走本地缓存还是走redis。

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: composite
      composite:
        l1CacheType: caffeine
        l2CacheType: redis
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
```

### （4）、支持配置指定缓存走本地缓存。

**4.1）全部缓存 走本地缓存**

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: composite
      composite:
        l1CacheType: caffeine
        l2CacheType: redis
        # 是否全部启用一级缓存，默认false
        l1AllOpen: true
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
      # 是否全部启用一级缓存，默认false
      l1AllOpen: true
```

**4.2）指定key 走本地缓存**

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: composite
      composite:
        l1CacheType: caffeine
        l2CacheType: redis
        # 是否全部启用一级缓存，默认false
        l1AllOpen: false
        # 是否手动启用一级缓存，默认false
        l1Manual: true
        # 手动配置走一级缓存的缓存key集合，针对单个key维度
        l1ManualKeySet:
          - userCache:user01
          - userCache:user02
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
      # 是否全部启用一级缓存，默认false
      l1AllOpen: false
      # 是否手动启用一级缓存，默认false
      l1Manual: true
      # 手动配置走一级缓存的缓存key集合，针对单个key维度
      l1ManualKeySet:
        - userCache:user01
        - userCache:user02
```

**4.3）指定缓存名字 走本地缓存**

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: composite
      composite:
        l1CacheType: caffeine
        l2CacheType: redis
        # 是否全部启用一级缓存，默认false
        l1AllOpen: false
        # 是否手动启用一级缓存，默认false
        l1Manual: true
        # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
        l1ManualCacheNameSet:
          - compositeCache
          - goodsSpecCache
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
      # 是否全部启用一级缓存，默认false
      l1AllOpen: false
      # 是否手动启用一级缓存，默认false
      l1Manual: true
      # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
      l1ManualCacheNameSet:
        - compositeCache
        - goodsSpecCache
```

**4.4）指定key + 指定缓存名字 走本地缓存**

2.0.0 版本，及之后版本的配置方式
```yaml
l2cache:
  config:
    defaultConfig:
      cacheType: composite
      composite:
        l1CacheType: caffeine
        l2CacheType: redis
        # 是否全部启用一级缓存，默认false
        l1AllOpen: false
        # 是否手动启用一级缓存，默认false
        l1Manual: true
        # 手动配置走一级缓存的缓存key集合，针对单个key维度
        l1ManualKeySet:
          - userCache:user01
          - userCache:user02
        # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
        l1ManualCacheNameSet:
          - compositeCache
          - goodsSpecCache
```

1.0.39 版本，及之前版本的配置方式
```yaml
l2cache:
  config:
    cacheType: composite
    composite:
      l1CacheType: caffeine
      l2CacheType: redis
      # 是否全部启用一级缓存，默认false
      l1AllOpen: false
      # 是否手动启用一级缓存，默认false
      l1Manual: true
      # 手动配置走一级缓存的缓存key集合，针对单个key维度
      l1ManualKeySet:
        - userCache:user01
        - userCache:user02
      # 手动配置走一级缓存的缓存名字集合，针对cacheName维度
      l1ManualCacheNameSet:
        - compositeCache
        - goodsSpecCache
```
### （5）、支持按需配置不同缓存维度的缓存配置（如：配置不同缓存类型）

2.0.0 版本新增的功能。
```yaml
l2cache:
  config:
    # 缓存配置集合（针对cacheName的个性化缓存配置），按需配置
    configMap:
      # 样例：指定某个缓存维度走caffeine
      brandCache:
        # 缓存类型
        cacheType: caffeine
        # 一级缓存
        caffeine:
          # 创建缓存的默认配置
          defaultSpec: initialCapacity=10,maximumSize=2,refreshAfterWrite=30m,softValues,recordStats
      # 样例：指定某个缓存维度走redis
      goodsPriceRevisionCache:
        # 缓存类型
        cacheType: redis
        # 二级缓存
        redis:
          # 加载数据时，是否加锁
          lock: false
          # 加锁时，true调用tryLock()，false调用lock()
          tryLock: true
          # 批量操作的大小，可以理解为是分页
          batchPageSize: 3
          # 默认缓存过期时间(ms)
          expireTime: 86400000
          # 针对cacheName维度的过期时间集合，单位ms
          expireTimeCacheNameMap:
            brandCache: 86400000
```

### （6）、缓存同步策略配置
详细配置见：[缓存同步策略配置](https://github.com/ck-jesse/l2cache/blob/master/doc/%E7%BC%93%E5%AD%98%E5%90%8C%E6%AD%A5%E7%AD%96%E7%95%A5%E9%85%8D%E7%BD%AE%E6%A0%B7%E4%BE%8B.md)
```yaml
l2cache:
  config:
    # 缓存同步策略配置
    cacheSyncPolicy:
      # 策略类型 kafka / redis，推荐使用redis
      type: redis
      # 缓存更新时通知其他节点的topic名称
      topic: l2cache
```

- **1、为什么推荐使用Redis的pubsub，而不是kafka等MQ？**
- 1）复用Redis中间件，可少依赖一个中间件，降低维护成本和复杂度
- 2）利用Redis的pubsub的特性，在消费者重启时，无需处理重启之前的消息，避免做无用功


- **2、Redis的pubsub有和特性？**
- 两个特性：1、Redis消息不持久化，2、生产者发送一个消息，如果没有消费者，消息将会被直接丢弃。
- 由于缓存的使用要求是不强求保证强一致性，只需保证最终一致性即可。因此刚好利用上面两个特性，当某个消费者重启时，正好无需去处理那些重启之前的消息，消息丢了就丢了，对本地缓存无影响。因为请求进来时，会再从redis中获取缓存信息并缓存到本地缓存。


### （7）、热key探测配置

- 1、不启用热key探测：type配置为none，或者不配置hotkey相关属性
```yaml
l2cache:
  config:
    # 热key探测 
    hotkey:
      # 热key探测类型,支持 none、jd、sentinel，目前 sentinel 仅支持单机，默认为 none，不启用热key探测。
      type: none
```

- 2、基于jd-hotkey的热key探测配置（jd-hotkey的搭建请参考官方，相对复杂，因为需要搭建一个server端）
```yaml
l2cache:
  config:
    # 热key探测 
    hotkey:
      # 热key探测类型,支持 none、jd、sentinel，目前 sentinel 仅支持单机，默认为 none
      type: jd
      jd:
        serviceName: weeget-bullet-goods-rest
        #etcd的地址，如有多个用逗号分隔
        etcdUrl: http://127.0.0.1:2379
```

- 3、基于sentinel的热key探测配置【推荐，因sentinel单机版本不依赖其他组件或服务，相对简单】
```yaml
l2cache:
  config:
    # 热key探测
    hotkey:
      # 热key探测类型,支持 none、jd、sentinel，目前 sentinel 仅支持单机，默认为 none
      type: sentinel
      sentinel:
        # 若配置了默认规则，针对所有的cacheName，生成其默认的热点参数规则，简化配置
        # 若未配置默认规则，则仅针对 rules 中的配置进行热点参数探测
        # 注：规则具体的配置是针对ParamFlowRule的配置
        default-rule:
          grade: 1
          param-idx: 0
          count: 6
        rules:
          # 案例1
          # resourceName 资源名称，通常设置为缓存名称
          - resource: newBrandCache
            # 流量控制的阈值类型，0表示线程数，1表示qps，默认1
            grade: 1
            # 参数下标
            paramIdx: 0
            # 阈值计数
            count: 3
          # 案例2
          - resource: newGoodsPriceRevisionCache
            count: 5
```

## 3、多redis实例场景的支持

- 新增功能：支持一个服务中有多个redis实例的场景，可以为cacheName配置指定的redissonClient实例
- 使用方式：在CacheService的实现类上标记注解 @CacheNameRedissonClientAnno ，设置实例id即可
- 实现原理：在系统加载完毕后，解析到CacheService实现类上的注解，并设置到一个容器中，再在RedisCacheBuilder构建RedissonCache时，根据cacheName获取对应的redissonClient实例，从而实现不同cacheName对应不同的redis实例。


```java

/**
 * 多redis实例的场景：指定redissonClient实例
 */
@CacheNameRedissonClientAnno(instanceId = "redissonClient2")
@Component
@Slf4j
public class OrderCacheService extends AbstractCacheService<String, OrderRespBO> {
    public static final String CACHE_NAME = "orderCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public OrderRespBO queryData(String orderId) {
        OrderRespBO orderRespBO = new OrderRespBO();
        orderRespBO.setOrderId(orderId);
        orderRespBO.setUserName("test");
        orderRespBO.setGoodsNum(1);
        log.info("查询订单相关信息,orderId={},orderRespBO={}", orderId, JSON.toJSONString(orderRespBO));
        return orderRespBO;
    }

    @Override
    public Map<String, OrderRespBO> queryDataList(List<String> notHitCacheKeyList) {
        Map<String, OrderRespBO> map = new HashMap<>();
        // 模拟返回数据，业务系统中可直接从DB加载数据
        for (int i = 0; i < notHitCacheKeyList.size(); i++) {
            String orderId = notHitCacheKeyList.get(i);
            OrderRespBO orderRespBO = new OrderRespBO();
            orderRespBO.setOrderId(orderId);
            orderRespBO.setUserName("test" + i);
            orderRespBO.setGoodsNum(1);
            map.put(orderId, orderRespBO);
        }
        log.info("[批量获取订单信息] valueLoader 分页获取订单信息, result={}", JSON.toJSONString(map));
        return map;
    }
}
```

## 4、代码中的使用

### 方式一：基于 CacheService 缓存层来使用（推荐-最佳实践）

- **问：为什么推荐CacheService 缓存层的这种模式，而不推荐使用更灵活的注解模式？如@Cacheable 等。**
- 答：随着系统的持续迭代，通过注解的方式，你会发现，这种缓存注解的代码是散落在程序各处的，当你想要维护时会无能为力。因此抽象出来一个CacheService缓存层，来统一定义并封装对缓存的通用操作，规范缓存操作，简化开发，你会发现这种方式真的很香。


- **问：CacheService 缓存层这种模式，适用于什么场景？**
- 答：1、适用于需要新引入缓存的系统； 2、适用于想规范统一缓存操作的项目，尤其是哪些缓存操作已经遍地开花的项目。


```java
/**
 * 第三阶段：重新优化 抽象出的缓存使用规范 CacheService，进一步简化业务开发
 * <p>
 * 优点：极简开发，完全屏蔽复杂的缓存实现细节，仅需实现几个业务方法，使得开发和维护更加简单，且解决随着业务迭代，代码坏味道递增的问题
 * 缺点：目前暂未发现明显缺点，若有，请留言。
 */
@Component
@Slf4j
public class NewBrandCacheService extends AbstractCacheService<Integer, BrandRespBO> {

    public static final String CACHE_NAME = "newBrandCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(Integer brandId) {
        return String.valueOf(brandId);
    }

    @Override
    public BrandRespBO queryData(Integer brandId) {
        if (null == brandId || 1002 == brandId) {
            return null;
        }
        // 模拟返回数据，业务系统中可直接从DB加载数据
        BrandRespBO brandRespBO = new BrandRespBO();
        brandRespBO.setBrandId(brandId);
        brandRespBO.setGroupId(0);
        brandRespBO.setBrandName("");
        brandRespBO.setBrandNumber("");
        brandRespBO.setDescription("brandId " + brandId);
        brandRespBO.setState(0);
        log.info("查询获取品牌相关信息,brandId={},brandInfoRespBO={}", brandId, JSON.toJSONString(brandRespBO));
        return brandRespBO;
    }

    @Override
    public Map<Integer, BrandRespBO> queryDataList(List<Integer> notHitCacheKeyList) {
        Map<Integer, BrandRespBO> map = new HashMap<>();
        // 模拟返回数据，业务系统中可直接从DB加载数据
        for (Integer brandId : notHitCacheKeyList) {
            BrandRespBO brandRespBO = new BrandRespBO();
            brandRespBO.setBrandId(brandId);
            brandRespBO.setGroupId(0);
            brandRespBO.setBrandName("");
            brandRespBO.setBrandNumber("");
            brandRespBO.setDescription("brandId " + brandId);
            brandRespBO.setState(0);
            map.put(brandId, brandRespBO);
        }
        log.info("[批量获取品牌信息] valueLoader 分页获取品牌信息, result={}", JSON.toJSONString(map));
        return map;
    }

}

```


### 方式二：结合 Spring Cache 的注解来使用

```java
/**
 * 第一阶段：无缓存使用规范，灵活但不可控
 * <p>
 * 优点：屏蔽二级缓存操作的复杂度
 * 缺点：没有限制，导致随着业务迭代，代码会变得混乱不堪，难以维护和扩展。
 * 如下代码中，注解的使用 和 直接操作缓存的代码混合在一起，甚至可能将不同缓存维度的缓存也混合在一起，过于灵活
 */
@Service
public class UserCacheService {

    private final Logger logger = LoggerFactory.getLogger(UserCacheService.class);

    @Autowired
    L2CacheCacheManager cacheManager;

    /**
     * 用于模拟db
     */
    private static Map<String, User> userMap = new HashMap<>();

    {
        userMap.put("user01", new User("user01", "addr"));
        userMap.put("user02", new User("user02", "addr"));
        userMap.put("user03", new User("user03", "addr"));
        userMap.put("user04", new User("user04", "addr"));
    }

    /**
     * 获取或加载缓存项
     * <p>
     * 注：sync=false，CaffeineCache在定时刷新过期缓存时，是通过get(Object key)来获取缓存项，由于没有valueLoader（加载缓存项的具体逻辑），所以定时刷新缓存时，缓存项过期则会被淘汰。
     */
    @Cacheable(value = "userCache", key = "#userId")
    public User queryUser(String userId) {
        User user = userMap.get(userId);
        try {
            Thread.sleep(1000);// 模拟加载数据的耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("加载数据:{}", user);
        return user;
    }

    @Cacheable(value = "userCache", key = "#userId", sync = true)
    public User queryUserSync(String userId) {
        User user = userMap.get(userId);
        logger.info("加载数据:{}", user);
        return user;
    }

    /**
     * 获取或加载缓存项
     * <p>
     * 注：因底层是基于caffeine来实现一级缓存，所以利用的caffeine本身的同步机制来实现
     * sync=true 则表示并发场景下同步加载缓存项，
     * sync=true，是通过get(Object key, Callable<T> valueLoader)来获取或加载缓存项，此时valueLoader（加载缓存项的具体逻辑）会被缓存起来，所以CaffeineCache在定时刷新过期缓存时，缓存项过期则会重新加载。
     * sync=false，是通过get(Object key)来获取缓存项，由于没有valueLoader（加载缓存项的具体逻辑），所以CaffeineCache在定时刷新过期缓存时，缓存项过期则会被淘汰。
     * <p>
     * 建议：设置@Cacheable的sync=true，可以利用Caffeine的特性，防止缓存击穿（方式同一个key和不同key）
     */
    @Cacheable(value = "userCache", key = "#userId", sync = true)
    public List<User> queryUserSyncList(String userId) {
        User user = userMap.get(userId);
        List<User> list = new ArrayList();
        list.add(user);
        logger.info("加载数据:{}", list);
        return list;
    }

    /**
     * 设置缓存
     * 注：通过 @CachePut 标注的方法添加的缓存项，在CaffeineCache的定时刷新过期缓存任务执行时，缓存项过期则会被淘汰。
     * 如果先执行了 @Cacheable(sync = true) 标注的方法，再执行 @CachePut 标注的方法，那么在CaffeineCache的定时刷新过期缓存任务执行时，缓存项过期则会重新加载。
     */
    @CachePut(value = "userCache", key = "#userId")
    public User putUser(String userId, User user) {
        return user;
    }

    /**
     * 淘汰缓存
     */
    @CacheEvict(value = "userCache", key = "#userId")
    public String evictUserSync(String userId) {
        return userId;
    }

    /**
     * 批量get
     */
    public Map<String, User> batchGetUser(List<String> userIdList) {
        Cache l2cache = (Cache) cacheManager.getCache("userCache").getNativeCache();
        Map<String, User> dataMap = l2cache.batchGet(userIdList);
        return dataMap;
    }

    /**
     * 批量get或load
     */
    public Map<String, User> batchGetOrLoadUser(List<String> userIdList) {
        Cache l2cache = (Cache) cacheManager.getCache("userCache").getNativeCache();
        Map<String, User> dataMap = l2cache.batchGetOrLoad(userIdList, notHitCacheKeyList -> {
            Map<String, User> valueLoaderHitMap = new HashMap<>();
            notHitCacheKeyList.forEach(key -> {
                valueLoaderHitMap.put(key, new User("user_load_" + key, "addr"));
            });
            return valueLoaderHitMap;
        });
        return dataMap;
    }
}

```



# 二、手动构建Cache

详细的构建方法参见l2care-core中如下单元测试类：

```
GuavaCacheTest
CaffeineCacheTest
RedisCacheTest
CompositeCacheTest
KafkaCacheSyncPolicyTest
```

下面列举`CaffeineCacheTest`中的一部分使用场景：

```java
L2CacheConfig l2CacheConfig = new L2CacheConfig();
CaffeineCache cache;
Callable<String> callable;

@Before
public void before() {
    L2CacheConfig.CacheConfig cacheConfig = new L2CacheConfig.CacheConfig();
    l2CacheConfig.setDefaultConfig(cacheConfig);
    // 默认配置 CAFFEINE
    cacheConfig.setCacheType(CacheType.CAFFEINE.name())
            .setAllowNullValues(true)
            .getCaffeine()
            .setDefaultSpec("initialCapacity=10,maximumSize=200,expireAfterWrite=2s,recordStats")
//                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=60s,recordStats")
            .setAutoRefreshExpireCache(false)
            .setRefreshPoolSize(3)
            .setRefreshPeriod(5L)
    ;

    l2CacheConfig.getCacheSyncPolicy()
            .setType(CacheSyncPolicyType.REDIS.name());

    // 构建缓存同步策略
    CacheSyncPolicy cacheSyncPolicy = new RedisCacheSyncPolicy()
            .setCacheConfig(l2CacheConfig)
            .setCacheMessageListener(new CacheMessageListener(L2CacheConfig.INSTANCE_ID))
            .setActualClient(Redisson.create());
    cacheSyncPolicy.connnect();//

    // 构建cache
    cache = (CaffeineCache) new CaffeineCacheBuilder()
            .setL2CacheConfig(l2CacheConfig)
            .setExpiredListener(new DefaultCacheExpiredListener())
            .setCacheSyncPolicy(cacheSyncPolicy)
            .build("localCache");

    callable = new Callable<String>() {
        AtomicInteger count = new AtomicInteger(1);

        @Override
        public String call() throws Exception {
            String result = "loader_value" + count.getAndAdd(1);
            System.out.println("loader value from valueLoader, return " + count.getAndAdd(1));
            return result;
        }
    };

    System.out.println("cacheType: " + cache.getCacheType());
    System.out.println("cacheName: " + cache.getCacheName());
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


# 三、核心原理

具体见：[L2Cache 核心原理解析](https://blog.csdn.net/icansoicrazy/article/details/131959016)