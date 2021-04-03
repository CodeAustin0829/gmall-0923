package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description 自定义Spring拦截器：配置类
 * @Author Austin
 * @Date 2021/3/27
 */
@Configuration // 标明当前类是个配置类，注入到Spring容器中
public class MvcConfig implements WebMvcConfigurer {

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
