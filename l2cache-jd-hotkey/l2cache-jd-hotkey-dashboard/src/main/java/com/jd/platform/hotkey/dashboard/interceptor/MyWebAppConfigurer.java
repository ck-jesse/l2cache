package com.jd.platform.hotkey.dashboard.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
public class MyWebAppConfigurer  implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		//拦截路径可自行配置多个 可用 ，分隔开
		InterceptorRegistration registration = registry.addInterceptor(new JwtInterceptor()).addPathPatterns("/**");
		registration.excludePathPatterns("/user/login","/error","/static/**","/main/**");
	}


	@Override
	public void addViewControllers(ViewControllerRegistry registry){
		//设置访问路径为 “/”
		registry.addViewController("/").setViewName("admin/index.html");
		//设置为最高优先级
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
	}


	/** 解决跨域问题 **/
	@Override
	public void addCorsMappings(CorsRegistry registry){
	}

	/** 静态资源处理 **/
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/templates/**").addResourceLocations("classpath:/templates/");
	}

	/** 默认静态资源处理器 **/
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}




}
