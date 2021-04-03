package com.atguigu.gmall.order.config;

import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/31
 */
@Configuration // 标明当前类是个配置类，注入到Spring容器中
public class WebMvcConfig implements WebMvcConfigurer {

    // 注入自定义的拦截器
    @Autowired
    private LoginInterceptor loginInterceptor;

    // 重写addInterceptors方法，在里面配置拦截器和设置拦截路径
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //【/**】，表示拦截所有路径
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**");
    }
}
