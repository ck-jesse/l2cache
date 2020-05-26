package com.coy.l2cache;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启 CaffeineRedisCache 组件
 * <p>
 * 注：Spring Enable 注解模式
 *
 * @author chenck
 * @date 2020/5/26 17:10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(CaffeineRedisCacheAutoConfiguration.class)
@Configuration
public @interface EnableCaffeineRedisCache {

}
