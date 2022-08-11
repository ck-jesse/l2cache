package com.jd.platform.hotkey.dashboard.controller;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.Result;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.ClearCfgVo;
import com.jd.platform.hotkey.dashboard.service.ClearService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @ProjectName: hotkey
 * @ClassName: ClearController
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/8/3 9:48
 */
@Controller
@RequestMapping("/clear")
public class ClearController {


    private String prefix = "admin/clear";


    @Resource
    private ClearService clearService;

    @GetMapping("/view")
    public String view(ModelMap modelMap){
        modelMap.put("title", Constant.CLEAR_VIEW);
        return prefix + "/list";
    }


    @PostMapping("/list")
    @ResponseBody
    public Page<ClearCfgVo> list(PageReq page, String app){
        PageInfo<ClearCfgVo> info = clearService.pageClearCfg(page, app);
        return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
    }


    @GetMapping("/edit/{app}")
    public String edit(@PathVariable("app") String app, ModelMap modelMap){
        modelMap.put("clearCfg", clearService.selectClearCfg(app));
        return prefix + "/edit";
    }

    @PostMapping("/save")
    @ResponseBody
    public Result save(ClearCfgVo cfg){
        int b = clearService.saveClearCfg(cfg);
        return b == 0 ? Result.fail():Result.success();
    }

}
