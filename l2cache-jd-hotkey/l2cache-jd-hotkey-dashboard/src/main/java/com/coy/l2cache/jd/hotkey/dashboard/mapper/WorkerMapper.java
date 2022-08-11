package com.coy.l2cache.jd.hotkey.dashboard.mapper;

import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.model.Worker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface WorkerMapper {

    int logicDeleteByKey(Integer id, String updateUser);

    @Options(useGeneratedKeys = true)
    int insertSelective(Worker record);

    Worker selectByPrimaryKey(Integer id);

    int updateByKey(Worker record);

    List<Worker> listWorker(SearchReq param);

    Worker selectByKey(String name);
}