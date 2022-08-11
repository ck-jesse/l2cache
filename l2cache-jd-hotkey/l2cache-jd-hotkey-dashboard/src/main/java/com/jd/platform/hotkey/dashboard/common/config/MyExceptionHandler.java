package com.jd.platform.hotkey.dashboard.common.config;

import com.jd.platform.hotkey.dashboard.common.domain.Result;
import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;
import com.jd.platform.hotkey.dashboard.common.ex.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;



/**
 * @author liyunfeng31
 */
@ControllerAdvice
public class MyExceptionHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@ExceptionHandler(value = BizException.class)
	@ResponseBody
	public Result bizExceptionHandler(BizException e, HttpServletResponse resp){
		resp.setStatus(e.getCode());
		logger.info("业务异常：",e);
		return Result.error(e.getCode(),e.getMsg());

	}


	@ExceptionHandler(value =Exception.class)
	@ResponseBody
	public Result exceptionHandler(Exception e){
		logger.info("未知异常：",e);
		return Result.error(ResultEnum.BIZ_ERROR);
	}
}
