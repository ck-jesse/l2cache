package com.github.l2cache.sync;

import com.github.l2cache.CacheConfig;
import com.github.l2cache.util.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 kafka 的同步策略
 *
 * @author chenck
 * @date 2020/7/7 17:15
 */
public class KafkaCacheSyncPolicy extends AbstractCacheSyncPolicy {

    private static final Logger logger = LoggerFactory.getLogger(KafkaCacheSyncPolicy.class);

    AtomicBoolean start = new AtomicBoolean(false);

    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> consumer;

    @Override
    public void connnect() {
        if (!start.compareAndSet(false, true)) {
            logger.info("[KafkaCacheSyncPolicy] already started");
            return;
        }
        CacheConfig.CacheSyncPolicy cacheSyncPolicy = this.getCacheConfig().getCacheSyncPolicy();

        // 生成 Consumer 的groupId
        genConsumerGroupName(cacheSyncPolicy);

        // 对于Properties中具体的属性，直接通过Kafka来进行解析和识别，未设置的属性，则取默认值
        producer = new KafkaProducer<>(cacheSyncPolicy.getProps());
        consumer = new KafkaConsumer<>(cacheSyncPolicy.getProps());

        // 启动一个线程订阅消息
        Thread subscribeThread = new Thread(() -> {
            // 订阅消息
            consumer.subscribe(Collections.singletonList(cacheSyncPolicy.getTopic()));
            while (true) {
                try {
                    // 拉取消息，设置指定超时时间
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
                    logger.debug("[KafkaCacheSyncPolicy] poll messages, topic={}, records={}", cacheSyncPolicy.getTopic(), records.count());

                    for (ConsumerRecord<String, String> record : records) {
                        logger.debug("[KafkaCacheSyncPolicy] received a message, topic={}, record={}", cacheSyncPolicy.getTopic(), record.toString());
                        CacheMessage message = ObjectMapperUtil.toObject(record.value(), CacheMessage.class);
                        KafkaCacheSyncPolicy.this.getCacheMessageListener().onMessage(message);
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    logger.error("[KafkaCacheSyncPolicy] poll message deal error", e);
                }
            }
        });
        subscribeThread.start();
    }

    @Override
    public void publish(CacheMessage message) {
        CacheConfig.CacheSyncPolicy cacheSyncPolicy = this.getCacheConfig().getCacheSyncPolicy();
        try {
            String messageStr = ObjectMapperUtil.toJson(message);
            logger.info("[KafkaCacheSyncPolicy] publish cache sync message, message={}", messageStr);

            // 异步发送，采用回调接收结果
            if (cacheSyncPolicy.isAsync()) {
                producer.send(new ProducerRecord<>(cacheSyncPolicy.getTopic(), null, messageStr), (recordMetadata, e) -> {
                    if (recordMetadata != null) {
                        logger.debug("[KafkaCacheSyncPolicy] sent to partition({}), offset({}), message({}) ",
                                recordMetadata.partition(), recordMetadata.offset(), messageStr);
                    } else {
                        logger.error("[KafkaCacheSyncPolicy] async publish cache message error", e);
                    }
                });
                logger.debug("[KafkaCacheSyncPolicy] async publish cache message");
                return;
            }
            // 同步发送消息
            RecordMetadata recordMetadata = producer.send(new ProducerRecord<>(cacheSyncPolicy.getTopic(), null, messageStr)).get();
            logger.info("[KafkaCacheSyncPolicy] publish topic={}, RecordMetadata={}", cacheSyncPolicy.getTopic(), recordMetadata.toString());
        } catch (Exception e) {
            logger.error("[KafkaCacheSyncPolicy] publish cache sync message error", e);
        }
    }

    @Override
    public void disconnect() {

    }

    /**
     * 生成 Consumer 的groupId
     * 一个group只有一个consumer,保证消息被每个group的consumer消费到，实现消息订阅模式
     */
    private void genConsumerGroupName(CacheConfig.CacheSyncPolicy cacheSyncPolicy) {
        String groupId = cacheSyncPolicy.getProps().getProperty(ConsumerConfig.GROUP_ID_CONFIG, "l2cacheGroup");
        cacheSyncPolicy.getProps().put(ConsumerConfig.GROUP_ID_CONFIG, groupId + genRandomSrc());
    }

    /**
     *
     */
    private int genRandomSrc() {
        long ct = System.currentTimeMillis();
        Random rnd_seed = new Random(ct);
        return (int) (rnd_seed.nextInt(10000) * 1000 + ct % 1000);
    }

    /**
     * 构建 Producer Properties
     */
    private Properties buildProducerProps(Properties properties) {
        Properties props = new Properties();
        setProp(properties, props, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        setProp(properties, props, ProducerConfig.CLIENT_ID_CONFIG);
        setProp(properties, props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        setProp(properties, props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        setProp(properties, props, ProducerConfig.ACKS_CONFIG);
        return props;
    }

    /**
     * 构建 Consumer Properties
     */
    private Properties buildConsumerProps(Properties properties) {
        Properties props = new Properties();
        setProp(properties, props, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        setProp(properties, props, ConsumerConfig.GROUP_ID_CONFIG);
        setProp(properties, props, ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG);
        setProp(properties, props, ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG);
        setProp(properties, props, ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG);
        setProp(properties, props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        setProp(properties, props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        setProp(properties, props, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG);
        setProp(properties, props, ConsumerConfig.MAX_POLL_RECORDS_CONFIG);
        setProp(properties, props, ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG);
        return props;
    }

    private void setProp(Properties source, Properties target, String key) {
        setProp(source, target, key, null);
    }

    private void setProp(Properties source, Properties target, String key, String defaultValue) {
        String value = source.getProperty(key, defaultValue);
        if (StringUtils.isEmpty(value)) {
            return;
        }
        target.put(key, value);
    }

}
