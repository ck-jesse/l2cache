package com.github.jesse.l2cache.example.filter;

import com.github.jesse.l2cache.example.util.MDCLogTracerContextUtil;
import com.github.jesse.l2cache.example.util.TracingVariable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * @author chenck
 * @date 2023/5/5 15:37
 */
public class ServletTraceInfoAttachmentFilter implements Filter {

    public ServletTraceInfoAttachmentFilter() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest requestHttp = (HttpServletRequest) request;
        // 默认从请求中获取trace_id（nginx中生成trace_id）
        String traceId = requestHttp.getHeader(TracingVariable.TRACE_ID);
        try {
            // 当请求中无trace_id时，默认生成一个
            if (traceId == null) {
                traceId = genTraceId();
            }
            MDCLogTracerContextUtil.attachTraceId(traceId);
            chain.doFilter(request, response);
        } finally {
            MDCLogTracerContextUtil.removeTraceId();
        }
    }

    /**
     * 生成trace_id
     */
    private String genTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
