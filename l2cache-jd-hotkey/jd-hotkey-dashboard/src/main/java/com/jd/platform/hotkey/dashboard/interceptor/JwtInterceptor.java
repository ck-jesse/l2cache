package com.jd.platform.hotkey.dashboard.interceptor;


import com.jd.platform.hotkey.dashboard.common.domain.Constant;
import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;
import com.jd.platform.hotkey.dashboard.common.ex.BizException;
import com.jd.platform.hotkey.dashboard.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author liyunfeng31
 */
public class JwtInterceptor extends HandlerInterceptorAdapter{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        String header = request.getHeader("x-requested-with");
        if(!StringUtils.isEmpty(header) && "XMLHttpRequest".endsWith(header) && request.getMethod().equals(Constant.POST)){
            String authHeader = request.getHeader(JwtTokenUtil.AUTH_HEADER_KEY);
            if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(JwtTokenUtil.TOKEN_PREFIX)) {
                throw new BizException(ResultEnum.NO_LOGIN);
            }
            final String token = authHeader.substring(2);
            Claims claims = JwtTokenUtil.parseJWT(token);
            String role = claims.get("role", String.class);
            if(role.equals(Constant.ADMIN)){
                return true;
            }
            String url = request.getRequestURI();
            if(url.contains(Constant.VIEW)||url.contains(Constant.LIST)||url.contains(Constant.INFO)){
                return true;
            }

        }
        return true;
    }

}
