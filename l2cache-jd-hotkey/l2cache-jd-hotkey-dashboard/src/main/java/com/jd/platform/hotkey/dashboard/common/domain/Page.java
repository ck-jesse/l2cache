package com.jd.platform.hotkey.dashboard.common.domain;

import java.io.Serializable;
import java.util.List;


public class Page<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 当前第几页
     */
    private  Integer page;
    /**
     * 总共多少条
     */
    private int total;

    private List<T> rows;

    public Page(Integer page, int total, List<T> rows) {
        this.page = page;
        this.total = total;
        this.rows = rows;
    }


    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "Page{" +
                "page=" + page +
                ", total=" + total +
                ", rows=" + rows +
                '}';
    }
}
