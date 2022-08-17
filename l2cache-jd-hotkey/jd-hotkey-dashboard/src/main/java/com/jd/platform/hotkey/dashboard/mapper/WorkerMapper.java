package com.jd.platform.hotkey.dashboard.mapper;

import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.model.Worker;
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