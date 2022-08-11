package com.jd.platform.hotkey.dashboard.mapper;

import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.HitCountVo;
import com.jd.platform.hotkey.dashboard.model.Summary;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author liyunfeng31
 */
@Mapper
public interface SummaryMapper {

    /**
     * records
     * @param records records
     * @return row
     */
    int saveOrUpdate(Summary records);

    List<HitCountVo> listRuleHitCount(SearchReq req);

    int clearExpireData(String app, Date expireDate);
}