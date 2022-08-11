package cn.weeget.hotkey.dashboard.controller;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.util.StringUtil;
import cn.weeget.hotkey.dashboard.common.base.BaseController;
import cn.weeget.hotkey.dashboard.common.domain.Constant;
import cn.weeget.hotkey.dashboard.common.domain.dto.ExcelDataDto;
import cn.weeget.hotkey.dashboard.common.domain.Page;
import cn.weeget.hotkey.dashboard.common.domain.Result;
import cn.weeget.hotkey.dashboard.common.domain.req.ChartReq;
import cn.weeget.hotkey.dashboard.common.domain.req.PageReq;
import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.common.domain.vo.HotKeyLineChartVo;
import cn.weeget.hotkey.dashboard.model.KeyRecord;
import cn.weeget.hotkey.dashboard.model.KeyTimely;
import cn.weeget.hotkey.dashboard.model.Statistics;
import cn.weeget.hotkey.dashboard.service.KeyService;
import cn.weeget.hotkey.dashboard.util.DateUtil;
import cn.weeget.hotkey.dashboard.util.ExcelUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;



/**
 * @author liyunfeng31
 */
@Controller
@RequestMapping("/key")
public class KeyController extends BaseController {
	
	private String prefix = "admin/key";

	@Resource
	private KeyService keyService;


	@PostMapping("/ruleLineChart")
	@ResponseBody
	public HotKeyLineChartVo ruleLineChart(SearchReq req){
		return keyService.ruleLineChart(req,ownApp());
	}


	@PostMapping("/lineChart")
	@ResponseBody
	public HotKeyLineChartVo lineChart(ChartReq chartReq){
		return keyService.getLineChart(chartReq);
	}


	@GetMapping("/view")
	public String view(ModelMap modelMap){
		modelMap.put("title", Constant.KEY_RECORD_VIEW);
		return prefix + "/list";
	}


	@PostMapping("/list")
	@ResponseBody
	public Page<KeyRecord> list(PageReq page, SearchReq searchReq){
		PageInfo<KeyRecord> info = keyService.pageKeyRecord(page, searchReq);
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}

	@GetMapping("/viewTimely")
	public String viewTimely(ModelMap modelMap){
		modelMap.put("title", Constant.TIMELY_KEY_VIEW);
		return prefix + "/listtimely";
	}


	@PostMapping("/listTimely")
	@ResponseBody
	public Page<KeyTimely> listTimely(PageReq page, SearchReq searchReq){
		return keyService.pageKeyTimely(page, searchReq);
	}


	@GetMapping("/viewMaxHot")
	public String viewMaxHot(ModelMap modelMap){
		modelMap.put("title", Constant.TIMELY_KEY_VIEW);
		return prefix + "/listmaxhot";
	}


	@PostMapping("/listMaxHot")
	@ResponseBody
	public Page<Statistics> listMaxHot(PageReq page, SearchReq searchReq){
		PageInfo<Statistics> info = keyService.pageMaxHot(page, searchReq);
		return new Page<>(info.getPageNum(),(int)info.getTotal(),info.getList());
	}


	@GetMapping("/add")
    public String add(){
        return prefix + "/add";
    }

	@PostMapping("/add")
	@ResponseBody
	public Result add(KeyTimely key){
		checkApp(key.getAppName());
		key.setUpdater(userName());
		int b = keyService.insertKeyByUser(key);
		return b == 0 ? Result.fail():Result.success();
	}

	@PostMapping("/remove")
	@ResponseBody
	public Result remove(String key){
		String[] arr = key.split("/");
		checkApp(arr[0]);
		int b = keyService.delKeyByUser(new KeyTimely(key,userName()));
		return b == 0 ? Result.fail():Result.success();
	}


    @PostMapping("/edit")
    @ResponseBody
    public Result editSave(KeyTimely key) {
		checkApp(key.getAppName());
		return Result.success(keyService.updateKeyByUser(key));
    }



	@RequestMapping(value = "/export", method = RequestMethod.GET)
	@ResponseBody
	public void export(HttpServletResponse resp,String startTime,String endTime,String app,String key){
		SearchReq req = new SearchReq();
		if(StringUtil.isNotEmpty(startTime)){
			req.setStartTime(DateUtil.strToDate(startTime));
		}
		if(StringUtil.isNotEmpty(endTime)){
			req.setEndTime(DateUtil.strToDate(endTime));
		}
		req.setApp(app);
		req.setKey(key);
		List<Statistics> records = keyService.listMaxHot(req);
		List<List<String>> rows = transform(records);
		ExcelDataDto data = new ExcelDataDto("hotKey.xlsx", Constant.HEAD,rows);
		ExcelUtil.exportExcel(resp,data);
	}



	private List<List<String>> transform(List<Statistics> records){
		List<List<String>> rows = new ArrayList<>();
		for (Statistics record : records) {
			List<String> list = new ArrayList<>();
			list.add(record.getKeyName());
			list.add(record.getCount().toString());
			list.add(record.getApp());
			rows.add(list);
		}
		return rows;
	}
}

