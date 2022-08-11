package com.jd.platform.hotkey.dashboard.common.domain.req;


/**
 * @author liyunfeng31
 */
public class PageReq {
	private int pageNum;
	private int pageSize;
	public int getPageNum() {
		return pageNum;
	}
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
}
