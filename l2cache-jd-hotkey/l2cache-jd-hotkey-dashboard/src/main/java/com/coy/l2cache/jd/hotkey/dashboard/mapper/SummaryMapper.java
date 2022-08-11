package com.coy.l2cache.jd.hotkey.dashboard.mapper;

import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.vo.HitCountVo;
import com.coy.l2cache.jd.hotkey.dashboard.model.Summary;
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