package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * @Description 自定义Spring拦截器：拦截器类
 * @Author Austin
 * @Date 2021/3/27
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    // 声明线程的局部变量，泛型写的是将来要取出来的数据对象
    private static final ThreadLocal<UserInfo> THREAD_LOCAL =
            new ThreadLocal<>();

    @Autowired
    private JwtProperties jwtProperties;

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
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */

    /*
    前置方法
    购物车的目的，不是拦截，
    而是在执行目标方法Handler之前，获取用户的登录信息
    所以不管登不登录，都需要放行，也就是返回值要是true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        System.out.println("执行目标方法之前，会执行此前置方法");

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

//        // 制造假数据，存放到ThreadLocal中进行测试
//        UserInfo userInfo = new UserInfo();
//        userInfo.setUserId(829L);
//        userInfo.setUserKey(UUID.randomUUID().toString());
//        // 把信息放入线程的局部变量
//        THREAD_LOCAL.set(userInfo);

        // 1.使用工具类CookieUtil获取登录头信息
        String userKey = CookieUtils.getCookieValue(request, this.jwtProperties.getUserKey());

        // 1.2.如果获取到的userKey是否为空，如果为空，手动制作一个userKey放入cookie中
        if (StringUtils.isBlank(userKey)) {
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response,
                    this.jwtProperties.getUserKey(), userKey, this.jwtProperties.getExpireTime());
        }

        // 1.3.将设置好的userKey，设置给UserInfo类的userKey属性
        UserInfo userInfo = new UserInfo();
        userInfo.setUserKey(userKey);

        // 2.使用工具类CookieUtil获取用户的登录信息，里面存放了用户id
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());

//        // 2.1.判断获取到token是否为空，如果不为空，则尝试进行解析
//        if (StringUtils.isNotBlank(token)) {
//            // 2.2.使用工具类解析jwt，根据公钥来进行解析
//            Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
//            // 2.3.在map中的用户id，是Object类型的，需要强转为long类型的
//            Long userId = Long.valueOf(map.get("userId").toString());
//            // 2.4.将userId设置给UserInfo类的userId属性
//            userInfo.setUserId(userId);
//        }
//
//        // 3.把用户信息放入线程的局部变量中，这样controller层、service层就能从getUserInfo()来获取里面的值
//        THREAD_LOCAL.set(userInfo);

        // 解析token的时候，可能会解析错误，需要抛出异常，即使解析异常，也进行set
        // 2.1.判断获取到token是否为空，如果不为空，则尝试进行解析
        try {
            if (StringUtils.isNotBlank(token)) {
                // 2.2.使用工具类解析jwt，根据公钥来进行解析
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
                // 2.3.在map中的用户id，是Object类型的，需要强转为long类型的
                Long userId = Long.valueOf(map.get("userId").toString());
                // 2.4.将userId设置给UserInfo类的userId属性
                userInfo.setUserId(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 3.把用户信息放入线程的局部变量中，这样controller层、service层就能从getUserInfo()来获取里面的值
            THREAD_LOCAL.set(userInfo);
        }


        // 4.不做拦截，只为获取用户登录信息，不管有没有登录都要放行
        return true;
    }

    /**
     * 封装了一个获取线程局部变量值的静态方法
     * 对外暴露可以get到THREAD_LOCAL的接口
     *
     * @return
     */
    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }

    /**
     * 在视图渲染完成之后执行，在完成方法中释放资源
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
