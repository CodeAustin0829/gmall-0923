package com.atguigu.gmall.order.interceptor;

import com.atguigu.gmall.order.entity.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description 自定义Spring拦截器：拦截器类
 * @Author Austin
 * @Date 2021/3/31
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    // 声明线程的局部变量，泛型写的是将来要取出来的数据对象
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 自定义Spring拦截器
     * 1.第一步：
     * 编写自定义拦截器类，实现HandlerInterceptor接口
     * 接口有三个方法：
     * (1)preHandle：
     * 前置方法，在Handler方法执行之前执行
     * 返回值为true-放行，false-被拦截,后面的代码不会被执行
     * (2)postHandle：
     * 后置方法，在Handler方法执行之后执行
     * (3)afterCompletion：
     * 完成方法，在视图渲染完成之后执行
     * 2.第二步：
     * 编写配置类（添加@Configuration注解）
     * 实现WebMvcConfigurer接口，重写addInterceptors方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**
         * 使用线程的局部变量来解决传递登录信息
         * （1）第一步：声明线程的局部变量ThreadLocal
         * 将其声明为private的，否则拿到set方法，会对ThreadLocal的值进行操作
         * 只要对外提供get接口即可
         * （2）第二步：封装了一个获取线程局部变量值的静态方法
         * 这样在其它地方，就可以通过类名来调用
         * 获取到ThreadLocal的值
         * （3）第三步：
         * 在视图渲染完成之后执行，执行释放资源remove()的方法
         * 调用删除方法，是必须选项。因为使用的是tomcat线程池，请求结束后，线程不会结束
         * 如果不手动删除线程变量，可能会导致内存泄漏
         */
        UserInfo userInfo = new UserInfo();

        // 从请求头中获取用户信息
        String userId = request.getHeader("userId");

        // 将userId设置到userInfo的属性中，将String类型转换为Long类型
        userInfo.setUserId(Long.valueOf(userId));

        // 设置到ThreadLocal中，传递给后续的业务
        THREAD_LOCAL.set(userInfo);

        // 不做拦截，只为获取用户登录信息，不管有没有登录都放行
        return true;
    }

    /**
     * 封装了一个获取线程局部变量值的静态方法
     *
     * @return
     */
    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }

    /**
     * 在视图渲染完成之后执行，经常在完成方法中释放资源
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        /*
        调用删除方法，是必须选项。
        因为使用的是tomcat线程池，请求结束后，线程不会结束。
        如果不手动删除线程变量，可能会导致内存泄漏
        因为THREAD_LOCAL底层本质上就是一个map
        map的key是ThreadLocal，是弱引用，自动会被GC回收
        map的value是UserInfo，也就是要传递的数据，是强引用，如果手动释放掉
        不会自动释放掉
        假设ThreadLocal被GC回收掉，会变成null，这样对应的value就不会被获取到来释放
        这样积少成多，会造成OOM  75af5d40-3469-48ea-b7c4-2b680fe5c6b0
         */
        THREAD_LOCAL.remove();
    }

}
