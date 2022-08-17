package com.jd.platform.hotkey.dashboard.mapper;

import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.model.Statistics;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author liyunfeng31
 */
@Mapper
public interface StatisticsMapper {


    /**
     * 查看
     * @return list
     */
    List<Statistics> listStatistics(SearchReq req);


    /**
     * 查看
     * @return list
     */
    List<Statistics> listOrderByTime(SearchReq req);

    /**
     * records
     * @param records records
     * @return row
     */
    int batchInsert(List<Statistics> records);

    /**
     * 多个时间聚合统计列表
     * @param req req
     * @return list
     */
    List<Statistics> sumStatistics(SearchReq req);

    /**
     * 清理
     */
    int clearExpireData(String app, Date expireDate);
}