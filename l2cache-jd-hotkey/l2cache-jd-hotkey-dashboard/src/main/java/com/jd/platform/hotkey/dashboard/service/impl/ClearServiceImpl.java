package com.jd.platform.hotkey.dashboard.service.impl;

import com.jd.platform.hotkey.dashboard.service.UserService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.util.StringUtil;
import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.ClearCfgVo;
import com.jd.platform.hotkey.dashboard.service.ClearService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: hotkey
 * @ClassName: ClearServiceImpl
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/8/3 9:57
 */
@Service
public class ClearServiceImpl implements ClearService {


    @Resource
    private IConfigCenter configCenter;

    @Resource
    private UserService userService;


    @Override
    public PageInfo<ClearCfgVo> pageClearCfg(PageReq page, String app) {
        List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.clearCfgPath);
        if(CollectionUtils.isEmpty(keyValues)){
            List<String> apps = userService.listApp();
            for (String ap : apps) {
                configCenter.put(ConfigConstant.clearCfgPath + ap, Constant.THIRTY_DAY);
            }
            keyValues = configCenter.getPrefix(ConfigConstant.clearCfgPath);
        }
        List<ClearCfgVo> cfgVos = new ArrayList<>();
        for (KeyValue kv : keyValues) {
            String v = kv.getValue().toStringUtf8();
            String key = kv.getKey().toStringUtf8();
            if(StringUtil.isEmpty(v)){
                configCenter.put(key, Constant.THIRTY_DAY);
                continue;
            }
            long version = kv.getModRevision();
            String k = key.replace(ConfigConstant.clearCfgPath,"");
            if(StringUtils.isEmpty(app)){
                cfgVos.add(new ClearCfgVo(k, v, version));
            }else{
                if(k.equals(app)){
                    cfgVos.add(new ClearCfgVo(k, v, version));
                }
            }
        }
        return new PageInfo<>(cfgVos);
    }

    @Override
    public ClearCfgVo selectClearCfg(String app) {
        KeyValue kv = configCenter.getKv(ConfigConstant.clearCfgPath + app);
        if(kv == null || kv.getValue() == null){
            configCenter.put(ConfigConstant.clearCfgPath + app, Constant.THIRTY_DAY);
            return new ClearCfgVo(app, Constant.THIRTY_DAY, 0L);
        }
        String v = kv.getValue().toStringUtf8();
        return new ClearCfgVo(app,v,kv.getModRevision());
    }

    @Override
    public int saveClearCfg(ClearCfgVo cfg) {
        configCenter.put(ConfigConstant.clearCfgPath + cfg.getApp(), cfg.getTtl());
        return 1;
    }
}
