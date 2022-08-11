package com.jd.platform.hotkey.dashboard.service.impl;

import cn.hutool.core.date.SystemClock;
import com.alibaba.fastjson.JSON;
import com.jd.platform.hotkey.dashboard.service.WorkerService;
import com.github.pagehelper.PageInfo;
import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.mapper.ChangeLogMapper;
import com.jd.platform.hotkey.dashboard.mapper.WorkerMapper;
import com.jd.platform.hotkey.dashboard.model.ChangeLog;
import com.jd.platform.hotkey.dashboard.model.Worker;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ProjectName: hotkey
 * @ClassName: WorkerService
 * @Author: liyunfeng31
 * @Date: 2020/4/17 18:19
 */
@Service
public class WorkerServiceImpl implements WorkerService {

    @Resource
    private IConfigCenter configCenter;

    @Resource
    private WorkerMapper workerMapper;

    @Resource
    private ChangeLogMapper changeLogMapper;

    public static void main(String[] args) {
        String a = "/jd/workers/catr/WORKER";
        int index = a.lastIndexOf("/");
        String aa = a.substring(index + 1);
        System.out.println(JSON.toJSONString(aa));
        System.out.println(JSON.toJSONString(index).hashCode());

    }

    @Override
    public PageInfo<Worker> pageWorker(PageReq page, SearchReq param) {
        //key:  "/jd/workers/cartsoa/host-10-173-244-22"	value:"10.173.244.22:11111"
        List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.workersPath);
        List<Worker> workers = new ArrayList<>();

        // /jd/count/cartsoa/10.173.244.22	2945
        List<KeyValue> clientCountKeyValues = configCenter.getPrefix(ConfigConstant.clientCountPath);
        // /jd/caffeineSize/10.173.244.22	{"cart0":25001, "cart1":1243}
        List<KeyValue> caffeineKeyValues = configCenter.getPrefix(ConfigConstant.caffeineSizePath);
        for (KeyValue kv : keyValues) {
            //"/jd/workers/cartsoa/host-10-173-244-22"
            String k = kv.getKey().toStringUtf8();
            //10.173.244.22:11111
            String v = kv.getValue().toStringUtf8();

            Worker worker = new Worker(k, v);
            // 多个app的连接该worker的count聚合
            int cliCount = 0;
            for (KeyValue keyValue : clientCountKeyValues) {
                //如果是该ip的count，都累加
                if (keyValue.getKey().toStringUtf8().endsWith(worker.getIp())) {
                    cliCount = cliCount + Integer.parseInt(keyValue.getValue().toStringUtf8());
                }
            }
            worker.setCliCount(cliCount);

            int caffeineCount = 0;
            for (KeyValue keyValue : caffeineKeyValues) {
                //如果是该ip的count，都累加
                if (keyValue.getKey().toStringUtf8().endsWith(worker.getIp())) {
                    String value = keyValue.getValue().toStringUtf8();
                    Map<String, Integer> caffeineMap = FastJsonUtils.stringToCollect(value);
                    for (Integer oneCount : caffeineMap.values()) {
                        caffeineCount += oneCount;
                    }
                }
            }
            worker.setCaffeineCount(caffeineCount);
            workers.add(worker);
        }

        return new PageInfo<>(workers);
    }


    @Override
    public int insertWorkerByUser(Worker worker) {
        configCenter.put(worker.getName(), worker.getIp() + Constant.SPIT + worker.getPort());
        return this.insertWorkerBySys(worker);
    }

    @Override
    public int insertWorkerBySys(Worker worker) {
        worker.setUpdateTime(new Date());
        try {
            workerMapper.insertSelective(worker);
            String to = JSON.toJSONString(worker);
            return changeLogMapper.insertSelective(new ChangeLog(worker.getName(), Constant.WORKER_CHANGE, "",
                    to, worker.getUpdateUser(), SystemClock.now() + ""));
        } catch (DuplicateKeyException e) {

        }
        return 0;
    }


    @Override
    public Worker selectByPrimaryKey(int id) {
        return workerMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateWorkerByUser(Worker worker) {
        configCenter.put(worker.getName(), worker.getIp() + Constant.SPIT + worker.getPort());
        return this.updateWorker(worker);
    }

    @Override
    public int delWorkerByUser(Worker worker) {
        configCenter.delete(worker.getName());
        return this.updateWorker(worker);
    }

    @Override
    public int updateWorker(Worker worker) {
        try {
            Worker oldWorker = workerMapper.selectByKey(worker.getName());
            String from = JSON.toJSONString(oldWorker);
            String to = JSON.toJSONString(worker);
            changeLogMapper.insertSelective(new ChangeLog(worker.getName(), Constant.WORKER_CHANGE, from, to, worker.getUpdateUser(), SystemClock.now() + ""));
            return workerMapper.updateByKey(worker);
        } catch (DuplicateKeyException e) {

        }
        return 0;
    }

    @Override
    public Worker selectByKey(String key) {
        String val = configCenter.get(key);
        return new Worker(key, val, 0);
    }
}
