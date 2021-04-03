package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

/**
 * @Description 自定义redis缓存注解
 * @Author Austin
 * @Date 2021/3/23
 */
// 1.ElementType.TYPE：表示可作用在类上，ElementType.METHOD：表示可作用在方法上
@Target({ElementType.METHOD})
// 2.表示当前注解是运行时注解
@Retention(RetentionPolicy.RUNTIME)
// 3.表示此注解是否可被其它注解继承，设置为最底层注解，不可被继承，注掉
// @Inherited
// 3.表示可以生成文档，当为项目生成文档时，注解也会被生成进文档中
@Documented
public @interface GmallCache {

    /**
     * 设置缓存Key的前缀
     *
     * @return
     */
    String prefix() default "";

    /**
     * 设置缓存的过期时间，单位：分钟[min]，默认是1天1440分钟
     *
     * @return
     */
    int timeout() default 1440;

    /**
     * 防止缓存雪崩，给缓存的过期时间设置随机值范围，单位：分钟[min]，范围是50分钟
     *
     * @return
     */
    int random() default 50;

    /**
     * 防止缓存击穿，添加分布式锁，设置分布式锁key的前缀
     *
     * @return
     */
    String lock() default "lock:";

}
