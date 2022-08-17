package com.jd.platform.hotkey.dashboard.common.monitor;


import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Queues;
import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.IRecord;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.mapper.KeyRecordMapper;
import com.jd.platform.hotkey.dashboard.mapper.StatisticsMapper;
import com.jd.platform.hotkey.dashboard.mapper.SummaryMapper;
import com.jd.platform.hotkey.dashboard.model.KeyRecord;
import com.jd.platform.hotkey.dashboard.model.Statistics;
import com.jd.platform.hotkey.dashboard.util.DateUtil;
import com.jd.platform.hotkey.dashboard.util.RuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class DataHandler {

    private static Logger log = LoggerFactory.getLogger(DataHandler.class);

    @Resource
    private KeyRecordMapper keyRecordMapper;
    @Resource
    private StatisticsMapper statisticsMapper;

    @Resource
    private SummaryMapper summaryMapper;

    @Resource
    private IConfigCenter configCenter;

    /**
     * 队列
     */
    private BlockingQueue<IRecord> queue = new LinkedBlockingQueue<>();

    /**
     * 入队
     */
    public void offer(IRecord record) {
        try {
            queue.put(record);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void insertRecords() {
        while (true) {
            try {
                List<IRecord> records = new ArrayList<>();
                Queues.drain(queue, records, 1000, 1, TimeUnit.SECONDS);
                if (CollectionUtil.isEmpty(records)) {
                    continue;
                }
                List<KeyRecord> keyRecordList = new ArrayList<>();
                for (IRecord iRecord : records) {
                    KeyRecord keyRecord = handHotKey(iRecord);
                    if (keyRecord != null) {
                        keyRecordList.add(keyRecord);
                    }
                }

                keyRecordMapper.batchInsert(keyRecordList);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * 处理热点key和记录
     */
    private KeyRecord handHotKey(IRecord record) {
        Date date = record.createTime();
        String appKey = record.appNameKey();
        String value = record.value();
        //appName+"/"+"key"
        String[] arr = appKey.split("/");
        String appName = arr[0];
        String key = arr[1];
        String uuid = UUID.randomUUID().toString();
        int type = record.type();

        //组建成对象，供累计后批量插入、删除
        if (type == 0) {
            //如果是客户端删除时发出的put指令
            if (com.jd.platform.hotkey.common.tool.Constant.DEFAULT_DELETE_VALUE.equals(value)) {
                log.info("client remove key event : " + appKey);
                return null;
            }
            //手工添加的是时间戳13位，worker传过来的是uuid
            String source = value.length() == 13 ? Constant.HAND : Constant.SYSTEM;
            String rule = RuleUtil.rule(appKey);
            KeyRecord keyRecord = new KeyRecord(key, rule, appName, 1, source, type, uuid, date);
            keyRecord.setRule(rule);
            return keyRecord;
        } else {
            //是删除
            return null;
        }
    }


    /**
     * 每小时 统计一次record 表 结果记录到统计表
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void offlineStatistics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Date nowTime = DateUtil.ldtToDate(now);
            int day = DateUtil.nowDay(now);
            int hour = DateUtil.nowHour(now);
            SearchReq preHour = new SearchReq(now.minusHours(1));
            List<Statistics> records = keyRecordMapper.maxHotKey(preHour);
            if (records.size() != 0) {
                records.forEach(x -> {
                    x.setBizType(1);
                    x.setCreateTime(nowTime);
                    x.setDays(day);
                    x.setHours(hour);
                    x.setUuid(1 + "_" + x.getKeyName() + "_" + hour);
                });
            }
            log.info("每小时统计最热点,时间：{}, 行数：{}", now.toString(), records.size());
            List<Statistics> statistics = keyRecordMapper.statisticsByRule(preHour);
            if (statistics.size() != 0) {
                statistics.forEach(x -> {
                    x.setBizType(6);
                    x.setRule(x.getRule());
                    x.setCreateTime(nowTime);
                    x.setDays(day);
                    x.setHours(hour);
                    x.setUuid(6 + "_" + x.getKeyName() + "_" + hour);
                });
                log.info("每小时统计规则,时间：{}, data list：{}", now.toString(), JSON.toJSONString(statistics));
                records.addAll(statistics);
            }
            if (records.size() > 0) {
                int row = statisticsMapper.batchInsert(records);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 每分钟统计一次record 表 结果记录到统计表
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void offlineStatisticsRule() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Date nowTime = DateUtil.ldtToDate(now);
            int day = DateUtil.nowDay(now);
            int hour = DateUtil.nowHour(now);
            int minus = DateUtil.nowMinus(now);

            List<Statistics> records = keyRecordMapper.statisticsByRule(new SearchReq(now.minusMinutes(1)));
            if (records.size() == 0) {
                return;
            }
            records.forEach(x -> {
                x.setBizType(5);
                x.setRule(x.getRule());
                x.setCreateTime(nowTime);
                x.setDays(day);
                x.setHours(hour);
                x.setMinutes(minus);
                // 骚操作 临时解决没有rule字段的问题
                x.setUuid(5 + "_" + x.getKeyName() + "_" + minus);
            });
            int row = statisticsMapper.batchInsert(records);
//            log.info("每分钟统计规则，时间：{}, 影响行数：{}, data list:{}", now.toString(), row, JSON.toJSONString(records));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 每天根据app的配置清理过期数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void clearExpireData() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.clearCfgPath);
            for (KeyValue kv : keyValues) {
                String key = kv.getKey().toStringUtf8();
                String ttl = kv.getValue().toStringUtf8();
                String app = key.replace(ConfigConstant.clearCfgPath, "");
                Date expireDate = DateUtil.ldtToDate(now.minusDays(Integer.parseInt(ttl)));
                summaryMapper.clearExpireData(app, expireDate);
                keyRecordMapper.clearExpireData(app, expireDate);
                statisticsMapper.clearExpireData(app, expireDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
