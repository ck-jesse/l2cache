package com.coy.l2cache.jd.hotkey.dashboard.mapper;

import com.coy.l2cache.jd.hotkey.dashboard.model.Rules;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface RulesMapper {

    Rules select(String app);

    int insert(Rules record);

    int update(Rules record);

    int delete(String app);

}