package com.atguigu.gmallgate.way.filter;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmallgate.way.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
@EnableConfigurationProperties(JwtProperties.class)
@Component // 需要将拦截器放入到Spring容器中，才能进行拦截
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties properties;

    /**
     * (3)第三步：重写AbstractGatewayFilterFactory抽象类无参构造方法，并调用super(XXXConfig.class)
     * 一定要重写构造方法
     * 告诉父类，这里使用PathConfig对象接收配置内容
     */
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        // 写法一：使用匿名内部类，实现GatewayFilter接口
//        return new GatewayFilter() {
//            @Override
//            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                System.out.println("我是局部过滤器，我只拦截配置了该过滤器的服务");
//                // 放行
//                return chain.filter(exchange);
//            }
//        };
        // 写法二：由于GatewayFilter是函数式接口，可以使用Lambda 表达式：拷贝小括号，写死右箭头，落实大括号
        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
            System.out.println("我是局部过滤器，我只拦截配置了该过滤器的服务，paths = " + config.paths);


            /**
             * 自定义局部过滤器，对用户的token进行校验，如果发现未登录，则进行拦截
             * 1.第一步：
             * 从请求对象中，获取当前请求的路径，判断是否在拦截名单中，不在直接放行
             * 2.第二步：
             * 获取请求中的jwt：同步请求从cookie中获取，异步请求从请求头header中获取
             * 3.第三步：
             * 判断jwt类型的token是否为空，为空则进行拦截，重定向到登录页面
             * 4.第四步：
             * token不为空，尝试解析jwt，如果出现异常，，重定向到登录页面
             * 5.第五步：
             * 为了防止盗用，获取载荷中的ip地址和当前请求的ip地址比较，不同，进行拦截，重定向到登录页面
             * 6.第六步：
             * 把用户信息传递给后续服务（请求头信息），避免后续微服务每次拿到token时还得解析
             * 7.第七步：
             * 放行
             */
            // 1.第一步：从请求对象中，获取当前请求的路径，判断是否在拦截名单中，不在直接放行
            // 1.1.从exchange中，获取请求对象和响应对象
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            // 1.2.从请求对象中，获取当前请求路径
            String curPath = request.getURI().getPath();
            // 1.3.获取拦截名单
            List<String> pathList = config.paths;
            /*
             1.4.判断拦截名单中存在当前的请求路径，遍历pathList
             allMatch()，如果满足条件则返回true
             < 0 ,表示不匹配
             indexOf(A,B),参数B在参数A中查询第一个出现的索引，当索引<0時，表示A中不包含B
             */
            if (pathList.stream().allMatch(path ->
                    StringUtils.indexOf(curPath, path) < 0)) {
                // 不在拦截名单，直接放行请求
                return chain.filter(exchange);
            }


            // 2.第二步：获取请求中的jwt：同步请求从cookie中获取，异步请求从请求头header中获取
            // 2.1.从请求头中获取
            String token = request.getHeaders().getFirst("token");
            // 2.2.判断token，如果为空，表示请求头中没有，则从cookie中获取
            if (StringUtils.isBlank(token)) {
                // 2.3.获取cookie
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                // 2.4.判断cookies是否为空，并且包含当前cookie，containsKey()如果包含返回true
                if (!CollectionUtils.isEmpty(cookies) &&
                        cookies.containsKey(this.properties.getCookieName())) {
                    // 2.5.如果不为空且包含，获取token
                    token = cookies.getFirst(this.properties.getCookieName()).getValue();
                }
            }


            // 3.第三步：判断jwt类型的token是否为空，为空则进行拦截，重定向到登录页面
            if (StringUtils.isEmpty(token)) {
                // 3.2.重定向到登录
                // 303状态码表示由于请求对应的资源存在着另一个URI，应使用重定向获取请求的资源
                response.setStatusCode(HttpStatus.SEE_OTHER);  // 重定向状态码，告诉服务器来重定向
                // LOCATION头信息，用来指定一个URL，进行重定向
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                // 3.1.setComplete()表示响应结束，也就是进行拦截
                return response.setComplete();
            }


            try {
                // 4.第四步：token不为空，尝试解析jwt，如果出现异常，重定向到登录页面，在catch重定向了
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());

                // 5.第五步：为了防止盗用，获取载荷中的ip地址和当前请求的ip地址比较，不同，进行拦截，重定向到登录页面
                String ip = map.get("ip").toString();
                String curIp = IpUtils.getIpAddressAtGateway(request);
                if (!StringUtils.equals(ip, curIp)) {
                    // 重定向到登录页面
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }

                // 6.第六步：把用户信息传递给后续服务（请求头信息），避免后续微服务每次拿到token时还得解析
                // 将userId转变成request对象。mutate：转变的意思
                request.mutate().header("userId", map.get("userId").toString()).build();
                // 将新的request对象转变成exchange对象
                exchange.mutate().request(request).build();

                // 7.放行
                return chain.filter(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                // 重定向到登录页面
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                return response.setComplete();
            }
        };
    }

    /**
     * 通过局部过滤器获取配置中的参数，需要以下步骤：
     * (1)自定义一个静态内部实体类XXXConfig
     * (2)指定网关过滤器中泛型是XXXConfig
     * (3)重写AbstractGatewayFilterFactory抽象类无参构造方法，并调用super(XXXConfig.class)
     * (4)重写shortcutFieldOrder方法指定接受参数的字段顺序
     * 如果要接受任意个参数：
     * (5)重写shortcutType方法，指定接受数据的字段类型
     */
    /**
     * (4)第四步：重写shortcutFieldOrder方法指定接受参数的字段顺序
     * 指定字段顺序
     * 可以通过不同的字段分别读取：/toLogin.html,/login
     * 在这里希望通过一个集合字段读取所有的路径
     *
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    /**
     * (5)第五步：重写shortcutType方法，指定接受数据的字段类型
     * 指定读取字段的结果集类型
     * 默认通过map的方式，把配置读取到不同字段
     * 例如：/toLogin.html,/login
     * 由于只指定了一个字段，只能接收/toLogin.html
     *
     * @return
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    //(1)第一步：自定义一个静态内部实体类XXXConfig
    @Data
    public static class PathConfig {
        private List<String> paths;
    }
}
