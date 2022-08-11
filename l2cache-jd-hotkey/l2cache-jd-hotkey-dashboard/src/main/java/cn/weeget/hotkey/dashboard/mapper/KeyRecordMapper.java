package cn.weeget.hotkey.dashboard.mapper;

import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.model.KeyRecord;
import cn.weeget.hotkey.dashboard.model.Statistics;
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