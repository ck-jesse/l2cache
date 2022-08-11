package com.coy.l2cache.jd.hotkey.dashboard.mapper;

import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.model.KeyRecord;
import com.coy.l2cache.jd.hotkey.dashboard.model.Statistics;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author liyunfeng31
 */
@Mapper
public interface KeyRecordMapper {

    int insertSelective(KeyRecord record);

    KeyRecord selectByPrimaryKey(Long id);

    List<KeyRecord> listKeyRecord(SearchReq req);

    int batchInsert(List<KeyRecord> list);

    List<Statistics> maxHotKey(SearchReq req);

    List<Statistics> statisticsByRule(SearchReq req);

    int clearExpireData(String app, Date expireDate);
}