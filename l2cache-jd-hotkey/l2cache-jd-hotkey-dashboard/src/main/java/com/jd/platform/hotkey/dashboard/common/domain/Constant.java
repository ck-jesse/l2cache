package com.jd.platform.hotkey.dashboard.common.domain;

import java.util.ArrayList;
import java.util.List;

public class Constant {


    public static final String JOIN = "_";

    public static final String SPIT = ":";

    public static final String SYSTEM = "SYSTEM";

    public static final String HAND = "HAND";


    public static final int RULE_CHANGE = 1;

    public static final int WORKER_CHANGE = 2;

    public static final int HOTKEY_CHANGE = 3;

    public static final String MONITOR_VIEW = "秒级监控";

    public static final String CHANG_LOG_VIEW = "变更记录";

    public static final String KEY_RECORD_VIEW = "热点记录";

    public static final String TIMELY_KEY_VIEW = "实时热点";

    public static final String RULE_CONFIG_VIEW = "规则配置";

    public static final String USER_MANAGE_VIEW = "用户管理";

    public static final String WORKER_VIEW = "节点信息";

    public static final String CLEAR_VIEW = "数据清理";


    public static final int MAX_DAY_RANGE = 3;

    public static final int VERSION = 1;

    public static final String ADMIN = "ADMIN";

    public static final String APP_UER = "APPUER";

    public static final String POST = "POST";


    public static final String VIEW = "view";


    public static final String LIST = "list";


    public static final String INFO = "info";


    public static final String THIRTY_DAY = "30";


    public static final List<String> HEAD = new ArrayList<>();

    static { HEAD.add("热点key");  HEAD.add("次数");  HEAD.add("所属APP");}

}
