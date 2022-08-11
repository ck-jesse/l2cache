package cn.weeget.hotkey.worker.counter;

import cn.weeget.hotkey.common.configcenter.ConfigConstant;
import cn.weeget.hotkey.common.configcenter.IConfigCenter;
import cn.weeget.hotkey.common.model.KeyCountModel;
import cn.weeget.hotkey.common.tool.Constant;
import cn.weeget.hotkey.common.tool.FastJsonUtils;
import cn.weeget.hotkey.common.tool.IpUtils;
import cn.weeget.hotkey.worker.tool.AsyncPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.weeget.hotkey.worker.counter.CounterConfig.COUNTER_QUEUE;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-28
 */
public class CounterConsumer {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public void beginConsume(IConfigCenter configCenter) {
        AsyncPool.asyncDo(() -> {
            Map<String, String> map = new HashMap<>(500);
            while (true) {
                try {
                    KeyCountItem item = COUNTER_QUEUE.take();
                    //每个List是一个client的10秒内的数据，一个rule如果每秒都有数据，那list里就有10条
                    List<KeyCountModel> keyCountModels = item.getList();
                    String appName = item.getAppName();
                    for (KeyCountModel keyCountModel : keyCountModels) {
                        //如 rule + Constant.COUNT_DELIMITER + nowTime;
                        //rule + 分隔符 + 2020-10-23 21:11:22
                        //pin__#**#2020-10-23 21:11:22
                        String ruleKey = keyCountModel.getRuleKey();
                        int hotHitCount = keyCountModel.getHotHitCount();
                        int totalHitCount = keyCountModel.getTotalHitCount();
                        //key：ConfigConstant.keyHitCountPath + appName + "/" + IpUtils.getIp() + "-" + System.currentTimeMillis()
                        String mapKey = appName + Constant.COUNT_DELIMITER + ruleKey;
                        if (map.get(mapKey) == null) {
                            map.put(mapKey, hotHitCount + "-" + totalHitCount);
                        } else {
                            String[] counts = map.get(mapKey).split("-");
                            int hotCount = Integer.valueOf(counts[0]) + hotHitCount;
                            int totalCount = Integer.valueOf(counts[1]) + totalHitCount;
                            map.put(mapKey, hotCount + "-" + totalCount);
                        }
                    }
                    //500是什么意思呢？300就代表了300秒的数据了，已经不少了
                    if (map.size() >= 300) {
                        configCenter.putAndGrant(ConfigConstant.keyHitCountPath + appName + "/" + IpUtils.getIp()
                                + "-" + System.currentTimeMillis(),
                                FastJsonUtils.convertObjectToJSON(map), 30);
                        logger.info("key Hit count : " + map);
                        map.clear();
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}
