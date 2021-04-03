package com.atguigu.gmallgate.way.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;


/**
 * @Description
 * 跨域过滤器：
 * springcloud-gateway集成的是webflux，使用的是CorsWebFilter
 * @Author Austin
 * @Date 2021/3/9
 */
//（1）第一步：标明该类是一个配置类，将当前过滤器类添加到Spring容器中
@Configuration
public class CorsConfig {

    //（2）在方法之上，加上 @Bean 注解，将方法的返回值对象注入到容器，也就是将CorsWebFilter对象注入到容器中
    @Bean
    public CorsWebFilter corsWebFilter(){
        //（5）第五步：提供参数config，初始化CORS对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许的域，不要写*，否则cookie就无法使用了
        config.addAllowedOrigin("http://manager.gmall.com");
        config.addAllowedOrigin("http://localhost:1000");
        config.addAllowedOrigin("http://127.0.0.1:1000");
        config.addAllowedOrigin("http://www.gmall.com");
        config.addAllowedOrigin("http://gmall.com");
        config.addAllowedOrigin("http://api.gmall.com");
        config.addAllowedOrigin("http://search.gmall.com");
        config.addAllowedOrigin("http://item.gmall.com");
        config.addAllowedOrigin("http://sso.gmall.com");
        config.addAllowedOrigin("http://order.gmall.com");
        // 允许所有头信息
        config.addAllowedHeader("*");
        // 允许所有请求方式
        config.addAllowedMethod("*");
        // 允许携带Cookie信息
        config.setAllowCredentials(true);


        //（4）第四步：提供构造器所需参数，
        //由于CorsConfigurationSource，所以new它的实现类即可
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        //第一个参数：添加映射路径，/**表示拦截一切请求
        //第二个参数，表示配置信息
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",config);

        //（3）第三步：创建一个CorsWebFilter对象，当作返回值
        //构造器必须需要一个参数CorsConfigurationSource configSource
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }

}
