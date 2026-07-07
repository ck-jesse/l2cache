package com.github.jesse.l2cache.util;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应DTO，主要用于不同服务之间的数据传递
 * 建议用法：必须指定业务数据T的具体类型，如： PageResult<UserInfoDto>
 *
 * @author chenck
 * @date 2024/4/28 20:29
 */
@Data
@Accessors(chain = true)// 链式调用
public class PageResult<T> implements Serializable {

    public static final Integer SUCC = 0;
    public static final String SUCC_MSG = "ok";
    public static final Integer ERROR = 1;
    /**
     * 页开始下标 默认1
     */
    private Long pageNum = 1L;

    /**
     * 每页大小 默认20
     */
    private Long pageSize = 10L;

    /**
     * 总记录数
     */
    private Long total = 0L;

    /**
     * 总页数
     */
    private Long pages = 0L;

    /**
     * 返回码
     */
    private Integer code = SUCC;

    /**
     * 描述信息
     */
    private String msg = SUCC_MSG;

    /**
     * 返回业务数据
     */
    private List<T> rows = new ArrayList<>();

    /**
     * 附加数据
     * 如：可以存放统计维度数据等
     */
    private Object attach;

    // 定义构造函数，方便使用
    public PageResult() {
    }

    public PageResult(String msg) {
        this.msg = msg;
    }

    public PageResult(List<T> rows) {
        this.rows = rows;
    }

    public PageResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public PageResult(Integer code, String msg, Long pageNum, Long pageSize, Long total) {
        this.code = code;
        this.msg = msg;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 校验是否成功
     */
    @JsonIgnore
    public boolean isSucc() {
        return SUCC.equals(code);
    }

    private static <T> PageResult<T> result(List<T> rows, Integer code, String msg) {
        return new PageResult<T>().setCode(code).setMsg(msg).setRows(rows);
    }

    private static <T> PageResult<T> result(List<T> rows, Integer code, String msg, Long pageNum, Long pageSize, Long total) {
        return new PageResult<T>().setCode(code).setMsg(msg)
                .setRows(rows)
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setTotal(total);
    }

    private static <T> PageResult<T> result(List<T> rows, Integer code, String msg, Long pageNum, Long pageSize, Long total, Long pages) {
        return new PageResult<T>().setCode(code).setMsg(msg)
                .setRows(rows)
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setTotal(total)
                .setPages(pages);
    }

    /**
     * 成功设置
     */
    public static <T> PageResult<T> succ() {
        return PageResult.result(null, SUCC, SUCC_MSG);
    }

    public static <T> PageResult<T> succ(String msg) {
        return PageResult.result(null, SUCC, msg);
    }

    public static <T> PageResult<T> succ(List<T> rows) {
        return PageResult.result(rows, SUCC, SUCC_MSG);
    }

    public static <T> PageResult<T> succ(List<T> rows, String msg) {
        return PageResult.result(rows, SUCC, msg);
    }

    public static <T> PageResult<T> succ(List<T> rows, Integer code, String msg) {
        return PageResult.result(rows, code, msg);
    }

    public static <T> PageResult<T> succ(List<T> rows, Long pageNum, Long pageSize, Long total) {
        return PageResult.result(rows, SUCC, SUCC_MSG, pageNum, pageSize, total);
    }

    public static <T> PageResult<T> succ(List<T> rows, Long pageNum, Long pageSize, Long total, Long pages) {
        return PageResult.result(rows, SUCC, SUCC_MSG, pageNum, pageSize, total, pages);
    }

    public static <T> PageResult<T> succ(List<T> rows, Long pageNum, Long pageSize, Long total, String msg) {
        return PageResult.result(rows, SUCC, msg, pageNum, pageSize, total);
    }

    /**
     * 失败设置
     */
    public static <T> PageResult<T> error(String msg) {
        return PageResult.result(null, ERROR, msg);
    }

    public static <T> PageResult<T> error(Integer code, String msg) {
        return PageResult.result(null, code, msg);
    }

    public static <T> PageResult<T> error(List<T> rows) {
        return PageResult.result(rows, ERROR, "操作失败");
    }

    public static <T> PageResult<T> error(List<T> rows, String msg) {
        return PageResult.result(rows, ERROR, msg);
    }

    public static <T> PageResult<T> error(List<T> rows, Integer code, String msg) {
        return PageResult.result(rows, code, msg);
    }

}
