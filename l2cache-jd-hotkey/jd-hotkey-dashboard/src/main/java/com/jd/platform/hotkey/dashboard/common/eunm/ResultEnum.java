package com.jd.platform.hotkey.dashboard.common.eunm;


public enum ResultEnum {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功！"),

    NO_LOGIN(1000, "未登录"),

    NO_CHANGE(1001, "操作无影响"),

    PWD_ERROR(1002, "账户/密码错误"),

    NO_PERMISSION(1003, "没有操作权限"),

    NO_RESOURCE(1004, "没有资源"),

    PARAM_ERROR(1005, "参数错误"),

    BIZ_ERROR(1006, "业务异常"),

    CONFLICT_ERROR(1007, "用户名/手机号已存在"),

    ILLEGAL_JSON_ARR(1008, "非法的json数组"),

    TIME_RANGE_LARGE(1022, "查询时间过大");


    private int code;

    private String msg;

    ResultEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
