# 记录一次阿里云Redis版超出最大内存限制异常

## 环境配置

**环境：** 压测环境

**版本：** 阿里云Redis版（4.0 社区版）

**规格：** 128G集群版（32节点）

**最大连接数：** 320,000

**客户端：** Redisson



## 业务场景

**前提：** 重构下单主流程，涉及活动、优惠券、订单、商品、库存等业务的拆分和优化。

**压测工具：** 阿里云 - 性能测试PTS

**压测场景：** 测试人员连续**`5天`**对 **`下单接口`** 进行压测，初步估算有产生**`3亿+`**的订单量，并且这些订单集中在7个商品上。并且`库存服务`为了提升吞吐量，将订单ID缓存到了Redis中。



## 具体错误

在扣减库存时，报如下错误，导致整个Redis不能对外提供服务。

```text
org.redisson.client.RedisOutOfMemoryException: command not allowed when used memory > 'maxmemory'.   
```

上面的错误信息很明显意思是redis的使用内存超出了最大内存限制。



## 分析

一个很蛋疼的历程，特此记录。

发现该错误并结合这几天的压测情况，第一直觉就是Redis服务端内存不够了，因为下单存在热点key的情况。

于是通过阿里云Redis管理控制台的性能监控查看Redis的各项指标，发现CPU、Memory、连接数等各项指标均为正常，甚至负载都非常低。也单独查看了前面几个节点的监控信息，也正常。这就奇怪了。

于是怀疑是`Redisson`的问题，在官网还真找到了 `RedisOutOfMemoryException` 相关的Issues，并且有一个`Jedis`和`Redisson`的对比案例，说是`Jedis`不报错误，而`Redisson`就是报这个错误，经过本地验证，确实如此。

> https://github.com/redisson/redisson/issues/1955

内心一阵独白MMP，临近上线，`Redisson`出现这么个巨坑。于是准备把涉及的多个服务和自研的二级缓存组件中相关Redisson的实现全部给替换掉。

但是时间和风险成本太高，于是拉上另一个架构师一起来分析，一个一个Redis节点排查，最终发现其中有一个节点内存使用率达到100%，具体如下：

### memory_usage

![memory_usage](img\memory_usage.png)



### evicted_keys  

另外，分析超出maxmemory限制的问题，可以通过 evicted_keys 这个指标直观的分析出来。

> evicted_keys  表示因达到maxmemory限制而被驱逐的键的数量。

若 evicted_keys > 0，则说明已经出现了超过maxmemory限制的情况，那么客户端极有可能会报 `command not allowed when used memory > 'maxmemory'.` 这个错误。

![evictd_keys](img\evictd_keys.png)

### info 

通过Info命令查看Redis状态

![info evictd_keys](img\info evictd_keys.png)

由此可见，除了要了解redis的实现原理外，还要对其各项运维指标有深入的了解，这样才能更好的去应对各种突发问题。



## 方案

1、清理掉Redis数据

2、减小库存服务中订单ID缓存的过期时间

> 单个数据结构只被保存在一个固定的槽内，所以存在单节点瓶颈

3、将`Hash数据结构`换为`String数据结构` ，达到将orderId均匀分布到不同的节点上的目的。

4、采用Redisson PRO版本来解决热点key数据路由到不同的数据分区进行存储，分散单节点的压力。

> Redisson PRO 为收费版本。



## 总结

从上面的这个坎坷历程可以发现几个点

1、不够细心，没有一个一个节点去排查分析

2、不够坚定，没有坚持自己最初的判断

3、对Redis的各项指标了解不够深入，其实可以很快定位到问题

4、技术选型一定要慎重，除了它的特性外，更多的要关注它有什么坑