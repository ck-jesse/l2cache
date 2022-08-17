--  !!! 注意设置sql model 否则可能sql报错 ！！！
--  查询你的sql_model参数：select @@global.sql_mode;  发现ONLY_FULL_GROUP_BY 则会导致报错
--  解决方式：set @@global.sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'
--  详情查阅：https://www.cnblogs.com/hjhsblogs/p/11079356.html

create database hotkey default character set utf8mb4 collate utf8mb4_general_ci;

DROP TABLE IF EXISTS `hk_change_log`;
CREATE TABLE `hk_change_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_id` varchar(128) DEFAULT ''  NOT NULL COMMENT '业务key',
  `biz_type` int(4) DEFAULT 1 NOT NULL COMMENT '业务类型：1规则变更；2worker变更',
  `from_str` varchar(1024)  NOT NULL DEFAULT '' COMMENT '原始值',
  `to_str` varchar(1024)  NOT NULL DEFAULT '' COMMENT '目标值',
  `app_name` varchar(32)  NOT NULL DEFAULT '' COMMENT '数据所属APP',
  `update_user` varchar(32)  NOT NULL DEFAULT '' COMMENT '修改人',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  `uuid` varchar(128) DEFAULT ''  NOT NULL COMMENT '防重ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_uuid`(`uuid`) USING BTREE COMMENT '防重索引'
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 日志变更记录 hk_change_log';


DROP TABLE IF EXISTS `hk_user`;
CREATE TABLE `hk_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `nick_name` varchar(32)  NOT NULL DEFAULT '' COMMENT '昵称',
  `user_name` varchar(32)  NOT NULL DEFAULT '' COMMENT '用户名',
  `pwd` varchar(64)   NOT NULL DEFAULT '' COMMENT '密码',
  `phone` varchar(16)   NOT NULL DEFAULT '' COMMENT '手机号',
  `role` varchar(16)   NOT NULL DEFAULT '' COMMENT '角色：ADMIN-超管，APPADMIN-app管理员，APPUSER-app用户',
  `app_name` varchar(64)   NOT NULL DEFAULT '' COMMENT '所属appName',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  `state` int(11) NOT NULL DEFAULT 1 COMMENT '状态：1可用；0冻结',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_userName`(`user_name`) USING BTREE COMMENT '账号唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 用户表 hk_user';


-- pwd: 123456
INSERT INTO `hk_user` VALUES (2, 'admin', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '1888888', 'ADMIN', '', '2020-07-28 14:01:03', 1, null,null,1);



DROP TABLE IF EXISTS `hk_key_record`;
CREATE TABLE `hk_key_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_name` varchar(128) NOT NULL DEFAULT '' COMMENT 'key',
  `app_name` varchar(64) NOT NULL DEFAULT '' COMMENT '所属appName',
  `val` varchar(2048)  NOT NULL DEFAULT '' COMMENT 'value',
  `duration` int(11) NOT NULL DEFAULT 60 COMMENT '缓存时间',
  `source` varchar(32)  NOT NULL DEFAULT '' COMMENT '来源',
  `type` int(11) NOT NULL DEFAULT 1 COMMENT '记录类型：1put；2del; -1unkonw',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `uuid` varchar(100)  NOT NULL DEFAULT '' COMMENT '防重ID',
  `rule` varchar(64)  NOT NULL DEFAULT '' COMMENT '规则',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_key`(`uuid`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 热key记录表 hk_key_record';


DROP TABLE IF EXISTS `hk_key_timely`;
CREATE TABLE `hk_key_timely`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_name` varchar(128)  NOT NULL DEFAULT '' COMMENT 'key',
  `val` varchar(2048) NOT NULL DEFAULT '' COMMENT 'value',
  `uuid` varchar(128) NOT NULL DEFAULT '' COMMENT '防重ID',
  `app_name` varchar(64) NOT NULL DEFAULT '' COMMENT '所属appName',
  `duration` int(11) NOT NULL DEFAULT 0 COMMENT '缓存时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_key`(`uuid`) USING BTREE COMMENT '唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 实时热点表 hk_key_timely';


CREATE TABLE `hk_statistics`  (
  `id` int(11) NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `key_name` varchar(100) DEFAULT '' NOT NULL COMMENT 'keyName',
  `count` int(11) DEFAULT 0 NOT NULL COMMENT '计数',
  `app` varchar(30)  DEFAULT '' NOT NULL COMMENT 'app',
  `days` int(11) DEFAULT 0 NOT NULL COMMENT '天数',
  `hours` bigint(11) DEFAULT 0 NOT NULL COMMENT '小时数',
  `minutes` bigint(11) DEFAULT 0 NOT NULL DEFAULT 0 COMMENT '分钟数',
  `biz_type` int(2) DEFAULT 0 NOT NULL COMMENT '业务类型',
  `rule` varchar(180) DEFAULT '' NOT NULL COMMENT '所属规则',
  `uuid` varchar(180) DEFAULT '' NOT NULL COMMENT '防重ID',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_uuid`(`uuid`) USING BTREE COMMENT '防重唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 热点统计表 hk_statistics';


CREATE TABLE `hk_rules`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rules` varchar(5000) DEFAULT ''  NOT NULL COMMENT '规则JSON',
  `app` varchar(30)  NOT NULL DEFAULT '' COMMENT '所属APP',
  `update_user` varchar(30) NOT NULL DEFAULT '' COMMENT '修改人',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '版本号',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_app`(`app`) USING BTREE COMMENT '防重索引'
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 规则表 hk_rules';



CREATE TABLE `hk_summary`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `index_name` varchar(32)  NOT NULL DEFAULT '' COMMENT '指标名称',
  `rule` varchar(32)   NOT NULL DEFAULT '' COMMENT '规则',
  `app` varchar(32)  NOT NULL DEFAULT '' COMMENT 'app',
  `index_val1` int(11) NOT NULL DEFAULT 0 COMMENT '指标值1',
  `index_val2` int(11) NOT NULL DEFAULT 0 COMMENT '指标值2',
  `index_val3` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '指标值3',
  `days` int(11) NOT NULL DEFAULT 0 COMMENT '天数',
  `hours` int(11) NOT NULL DEFAULT 0 COMMENT '小时数',
  `minutes` int(11) NOT NULL DEFAULT 0 COMMENT '分钟数',
  `seconds` int(11) NOT NULL DEFAULT 0 COMMENT '秒数',
  `biz_type` tinyint(2) NOT NULL DEFAULT 0 COMMENT '类型',
  `uuid` varchar(128)  NOT NULL DEFAULT '' COMMENT '防重ID',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) default 1 not null comment '删除标志，0删除 1正常',
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  comment '创建时间',
  `update_time` timestamp NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_uuid`(`uuid`) USING BTREE COMMENT '防重索引',
  INDEX `idx_apprule`(`app`, `rule`) USING BTREE COMMENT '查询索引',
  INDEX `idx_ct`(`create_time`) USING BTREE COMMENT '时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact comment '[架构二组] 汇总表 hk_summary';




