# yaml属性文件自定义属性提示，官方文档：

https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-configuration-metadata.html#configuration-metadata-additional-metadata

# 关于 自定义starter 配置的问题

1. spring-boot 2.x 自定义starter的配置文件，在resources下创建META-INF/spring.factories文件，内容如下：

```text
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.github.jesse.l2cache.spring.config.L2CacheConfiguration,\
com.github.jesse.l2cache.spring.config.HotKeyConfiguration
```

2. spring-boot 3.x 自定义starter的配置文件，在resources下创建META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件，内容如下：

```text
com.github.jesse.l2cache.spring.config.L2CacheConfiguration
com.github.jesse.l2cache.spring.config.HotKeyConfiguration
```