package com.jd.platform.hotkey.dashboard.common.domain.dto;

import java.util.List;

/**
 * @author liyunfeng31
 */
public class ExcelDataDto {
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 数据的表头(类属性)
     */
    private List<String> head;
    /**
     * 数据集合
     */
    private List<List<String>> rows;

    public ExcelDataDto(String fileName, List<String> head, List<List<String>> rows) {
        this.fileName = fileName;
        this.head = head;
        this.rows = rows;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getHead() {
        return head;
    }

    public void setHead(List<String> head) {
        this.head = head;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
}