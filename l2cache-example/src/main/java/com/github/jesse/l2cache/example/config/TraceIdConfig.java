package com.github.jesse.l2cache.example.config;

import com.github.jesse.l2cache.example.filter.ServletTraceInfoAttachmentFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenck
 * @date 2023/5/5 15:38
 */
@Configuration
public class TraceIdConfig {
    /**
     * 配置http请求日志跟踪信息拦截器，header中无trace_id则生成
     */
    @Bean
    public FilterRegistrationBean logFilterRegister() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new ServletTraceInfoAttachmentFilter());
        registration.setName(ServletTraceInfoAttachmentFilter.class.getSimpleName());
        registration.addUrlPatterns("/*");
        // 从小到大的顺序来依次过滤
        registration.setOrder(1);
        return registration;
    }
}