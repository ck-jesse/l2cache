package cn.weeget.hotkey.dashboard.service.impl;


import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ibm.etcd.api.Event;
import cn.weeget.hotkey.common.configcenter.ConfigConstant;
import cn.weeget.hotkey.common.configcenter.IConfigCenter;
import cn.weeget.hotkey.common.model.HotKeyModel;
import cn.weeget.hotkey.dashboard.common.domain.Constant;
import cn.weeget.hotkey.dashboard.common.domain.Page;
import cn.weeget.hotkey.dashboard.common.domain.req.ChartReq;
import cn.weeget.hotkey.dashboard.common.domain.req.PageReq;
import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.common.domain.vo.HotKeyLineChartVo;
import cn.weeget.hotkey.dashboard.mapper.ChangeLogMapper;
import cn.weeget.hotkey.dashboard.mapper.KeyRecordMapper;
import cn.weeget.hotkey.dashboard.mapper.StatisticsMapper;
import cn.weeget.hotkey.dashboard.model.ChangeLog;
import cn.weeget.hotkey.dashboard.model.KeyRecord;
import cn.weeget.hotkey.dashboard.model.KeyTimely;
import cn.weeget.hotkey.dashboard.model.Statistics;
import cn.weeget.hotkey.dashboard.netty.HotKeyReceiver;
import cn.weeget.hotkey.dashboard.service.KeyService;
import cn.weeget.hotkey.dashboard.service.RuleService;
import cn.weeget.hotkey.dashboard.util.CommonUtil;
import cn.weeget.hotkey.dashboard.util.DateUtil;
import cn.weeget.hotkey.dashboard.util.PageUtil;
import cn.weeget.hotkey.dashboard.util.RuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @ProjectName: hotkey
 * @ClassName: KeyServiceImpl
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 17:53
 */
@Service
public class KeyServiceImpl implements KeyService {

    @Resource
    private IConfigCenter configCenter;
    @Resource
    private KeyRecordMapper recordMapper;
    @Resource
    private StatisticsMapper statisticsMapper;
    @Resource
    private RuleService ruleService;
    @Resource
    private ChangeLogMapper logMapper;

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 折线图
     *
     * @param req req
     * @return vo
     */
    @Override
    public HotKeyLineChartVo ruleLineChart(SearchReq req, String app) {
        int type = req.getType();
        String appReq = req.getApp();
        // admin 全查
        if (StrUtil.isNotEmpty(appReq)) {
            app = appReq;
        }
        req.setApp(null);
        LocalDateTime now = LocalDateTime.now();
        req.setEndTime(req.getEndTime() == null ? DateUtil.ldtToDate(now) : req.getEndTime());
        List<String> rules = ruleService.listRules(null);
        if (type == 4) {
            LocalDateTime st = req.getStartTime() == null ? now.minusMinutes(31) : DateUtil.dateToLdt(req.getStartTime());
            req.setStartTime(DateUtil.ldtToDate(st));
            LocalDateTime et = DateUtil.dateToLdt(req.getEndTime());
            boolean longTime = Duration.between(st, et).toHours() > 2;
            req.setType(longTime ? 6 : 5);
            List<Statistics> list = statisticsMapper.listOrderByTime(req);
            return CommonUtil.processData(st, et, list, !longTime, rules, app);
        }

        if (type == 5) {
            LocalDateTime startTime = now.minusMinutes(31);
            req.setStartTime(DateUtil.ldtToDate(startTime));
            List<Statistics> list = statisticsMapper.listOrderByTime(req);
            return CommonUtil.processData(startTime, now, list, true, rules, app);
        } else if (type == 6) {
            LocalDateTime startTime2 = now.minusHours(25);
            req.setStartTime(DateUtil.ldtToDate(startTime2));
            List<Statistics> list2 = statisticsMapper.listOrderByTime(req);
            return CommonUtil.processData(startTime2, now, list2, false, rules, app);
        } else {
            LocalDateTime startTime3 = now.minusDays(7).minusHours(1);
            req.setStartTime(DateUtil.ldtToDate(startTime3));
            req.setType(6);
            List<Statistics> list3 = statisticsMapper.listOrderByTime(req);
            return CommonUtil.processData(startTime3, now, list3, false, rules, app);
        }
    }


    @Override
    public Page<KeyTimely> pageKeyTimely(PageReq page, SearchReq param) {
        List<KeyTimely> keyTimelies = HotKeyReceiver.list(param);
        return PageUtil.pagination(keyTimelies, page.getPageSize(), page.getPageNum()-1);

    }

    @Override
    public PageInfo<Statistics> pageMaxHot(PageReq page, SearchReq req) {
        checkParam(req);
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        List<Statistics> statistics = statisticsMapper.sumStatistics(req);
        return new PageInfo<>(statistics);
    }

    @Override
    public List<Statistics> listMaxHot(SearchReq req) {
        checkParam(req);
        return statisticsMapper.sumStatistics(req);
    }


    @Override
    public HotKeyLineChartVo getLineChart(ChartReq chartReq) {
        int hours = 6;
        // 默认查询6小时内的数据
        SearchReq req = new SearchReq();
        req.setStartTime(DateUtil.preTime(hours));
        req.setEndTime(new Date());
        List<Statistics> statistics = statisticsMapper.listStatistics(req);
        // 获取data Y轴
        Map<String, int[]> keyDateMap = keyDateMap(statistics, hours);
        // 获取时间x轴
        List<String> list = new ArrayList<>();
        for (int i = hours; i > 0; i--) {
            LocalDateTime time = LocalDateTime.now().minusHours(i - 1);
            int hour = time.getHour();
            list.add(hour + "时");
        }
        return new HotKeyLineChartVo(list, keyDateMap);
    }


    @Override
    public PageInfo<KeyRecord> pageKeyRecord(PageReq page, SearchReq param) {
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        List<KeyRecord> listKey = recordMapper.listKeyRecord(param);
        for (KeyRecord keyRecord : listKey) {
            keyRecord.setRuleDesc(RuleUtil.ruleDesc(keyRecord.getAppName() + "/" + keyRecord.getKey()));
        }

        return new PageInfo<>(listKey);
    }


    @Override
    public int insertKeyByUser(KeyTimely key) {
        configCenter.putAndGrant(ConfigConstant.hotKeyPath + key.getAppName() + "/" + key.getKey(),
                System.currentTimeMillis() + "", key.getDuration());

        //写入本地缓存，实时热key信息
        HotKeyModel hotKeyModel = new HotKeyModel();
        hotKeyModel.setCreateTime(System.currentTimeMillis());
        hotKeyModel.setAppName(key.getAppName());
        hotKeyModel.setKey(key.getKey());
        HotKeyReceiver.put(hotKeyModel);
        return logMapper.insertSelective(new ChangeLog(key.getAppName(), Constant.HOTKEY_CHANGE, "",
                key.getKey(), key.getUpdater(), SystemClock.now() + ""));
    }

    @Override
    public int updateKeyByUser(KeyTimely key) {
        String ectdKey = ConfigConstant.hotKeyPath + key.getAppName() + "/" + key.getKey();
        configCenter.putAndGrant(ectdKey, "UPDATE", key.getDuration());
        return 1;
    }

    @Override
    public int delKeyByUser(KeyTimely keyTimely) {
        //app + "_" + key
        String[] arr = keyTimely.getKey().split("/");
        //删除client监听目录的key
        String etcdKey = ConfigConstant.hotKeyPath + arr[0] + "/" + arr[1];

        //删除caffeine里的实时key
        HotKeyReceiver.delete(arr[0] + "/" + arr[1]);

        if (configCenter.get(etcdKey) == null) {
            //如果手工目录也就是client监听的目录里没有该key，那么就往里面放一个，然后再删掉它，这样client才能监听到删除事件
            configCenter.putAndGrant(etcdKey, cn.weeget.hotkey.common.tool.Constant.DEFAULT_DELETE_VALUE, 1);
        }
        configCenter.delete(etcdKey);

        KeyRecord keyRecord = new KeyRecord(arr[1], "", arr[0], 0, Constant.HAND,
                Event.EventType.DELETE_VALUE, UUID.randomUUID().toString(), new Date());
        recordMapper.insertSelective(keyRecord);
        return logMapper.insertSelective(new ChangeLog(keyTimely.getKey(), Constant.HOTKEY_CHANGE, keyTimely.getKey(), "", keyTimely.getUpdater(), SystemClock.now() + ""));
    }


    private Map<String, int[]> keyDateMap(List<Statistics> statistics, int hours) {
        Map<String, int[]> map = new HashMap<>(10);
        Map<String, List<Statistics>> listMap = statistics.stream().collect(Collectors.groupingBy(Statistics::getKeyName));
        for (Map.Entry<String, List<Statistics>> m : listMap.entrySet()) {
            int start = DateUtil.preHoursInt(5);
            map.put(m.getKey(), new int[hours]);
            int[] data = map.get(m.getKey());
            int tmp = 0;
            for (int i = 0; i < hours; i++) {
                Statistics st;
                try {
                    st = m.getValue().get(tmp);
                    if (String.valueOf(start).endsWith("24")) {
                        start = start + 77;
                    }
                    if (start != st.getHours()) {
                        data[i] = 0;
                    } else {
                        tmp++;
                        data[i] = st.getCount();
                    }
                    start++;
                } catch (Exception e) {
                    data[i] = 0;
                }
            }
        }
        return map;
    }


    private void checkParam(SearchReq req) {
        if (req.getStartTime() == null || req.getEndTime() == null) {
            req.setStartTime(DateUtil.preTime(5));
            req.setEndTime(new Date());
        }
       /* long day = (req.getEndTime().getTime() - req.getStartTime().getTime()) / 86400000;
        if( day > Constant.MAX_DAY_RANGE){
            throw new BizException(ResultEnum.TIME_RANGE_LARGE);
        }*/
    }

}


