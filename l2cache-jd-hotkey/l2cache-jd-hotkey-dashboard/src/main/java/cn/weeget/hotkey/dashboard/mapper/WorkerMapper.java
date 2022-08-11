package cn.weeget.hotkey.dashboard.mapper;

import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.model.Worker;
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