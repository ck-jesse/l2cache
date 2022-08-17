package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.consts.CacheSyncPolicyType;
import com.github.jesse.l2cache.sync.CacheMessageListener;
import com.github.jesse.l2cache.sync.KafkaCacheSyncPolicy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

/**
 * @author chenck
 * @date 2020/7/8 14:51
 */
public class KafkaCacheSyncPolicyTest {
    CacheConfig cacheConfig = new CacheConfig();

    @Before
    public void before() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "DemoProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ConsumerSubscribeGroup");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");// 自动提交offset（默认true）将在后台定期提交
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");// 自动提交间隔
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");// 设置一次最大拉取的消息条数（默认500）
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "3000");

        cacheConfig.getCacheSyncPolicy()
                .setType(CacheSyncPolicyType.KAFKA.name())
                .setProps(props);
    }

    @Test
    public void test() {
        CacheSyncPolicy cacheSyncPolicy = new KafkaCacheSyncPolicy()
                .setCacheConfig(cacheConfig)
                .setCacheMessageListener(new CacheMessageListener(cacheConfig.getInstanceId()));
        cacheSyncPolicy.connnect();
    }
}
