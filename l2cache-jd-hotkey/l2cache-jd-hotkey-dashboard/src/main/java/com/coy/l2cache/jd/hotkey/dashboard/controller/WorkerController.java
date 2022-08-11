package com.coy.l2cache.jd.hotkey.dashboard.controller;

import com.coy.l2cache.jd.hotkey.dashboard.common.domain.Constant;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.Page;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.Result;
import com.github.pagehelper.PageInfo;
import com.coy.l2cache.jd.hotkey.dashboard.common.base.BaseController;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.PageReq;
import com.coy.l2cache.jd.hotkey.dashboard.model.Worker;
import com.coy.l2cache.jd.hotkey.dashboard.service.WorkerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@Controller
@RequestMapping("/worker")
public class WorkerController extends BaseController {
	
	private String prefix = "admin/worker";

	@Resource
	private WorkerService workerService;

	@GetMapping("/view")
    public String view(ModelMap modelMap){
		modelMap.put("title", Constant.WORKER_VIEW);
    	return prefix + "/list";
    }


	@PostMapping("/list")
	@ResponseBody
		public Page<Worker> list(PageReq page, String searchText){
		PageInfo<Worker> info = workerService.pageWorker(page, param(searchText));
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}


    @GetMapping("/add")
    public String add(){
        return prefix + "/add";
    }

	@PostMapping("/add")
	@ResponseBody
	public Result add(Worker worker){
		worker.setUpdateUser(userName());
		int b=workerService.insertWorkerByUser(worker);
		return b == 0 ? Result.fail():Result.success();
	}

	@PostMapping("/remove")
	@ResponseBody
	public Result remove(String key){
		int b=workerService.delWorkerByUser(new Worker(key.replace("_","/"),0,userName()));
		return b == 0 ? Result.fail():Result.success();
	}


	@GetMapping("/edit/{key}")
    public String edit(@PathVariable("key") String key, ModelMap modelMap){
		modelMap.put("worker", workerService.selectByKey(key.replace("_","/")));
        return prefix + "/edit";
    }
	

    @PostMapping("/edit")
    @ResponseBody
    public Result editSave(Worker worker) {
		worker.setUpdateUser(userName());
		return Result.success(workerService.updateWorkerByUser(worker));
    }

}

