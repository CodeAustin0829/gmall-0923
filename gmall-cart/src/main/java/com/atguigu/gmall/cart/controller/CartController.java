package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/27
 */
//@RestController
@Controller
@EnableConfigurationProperties({JwtProperties.class})
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 新增购物车：
     * 1.将购物车信息，新增到数据库
     * 主要携带的两个参数：在地址栏?skuId=40&count=2
     * (1)skuId：商品id
     * (2)count：商品数量
     * 多个参数，在方法形参上使用cart对象来接收参数
     * 2.新增成功后，重定向到新增购物车成功页面，给出添加成功提示
     * 重定向，返回值使用String
     */
    @GetMapping
    public String addCart(Cart cart) {

        // 1.将购物车保存到数据库中，因为只有两个字段：skuId、count，信息不全，还需要在Service进行判断添加
        this.cartService.addCart(cart);

        // 2.重定向到成功页面，所以还需要一个方法来处理跳转到成功页面，重定向时，在地址栏?后面将skuId传递过来，这样就可以根据skuId来查询数据库了
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    /**
     * 新增购物车成功页面：
     * 本质上，就是根据用户的登录信息和skuId来查询数据库，显示所添加商品信息
     * 1.addCart()方法在重定向的时候，在？后面传递了skuId参数，需要在该方法的形参上使用@RequestParam来接惨
     * 2.返回视图名称，要跟所要跳转的页面addCart.html名称一致，这样thymeleaf才能解析视图跳转到页面
     * 3.不能@RestController，否则返回的视图名称，会被解析成Json字符串
     */
    @GetMapping("addCart.html")
    public String queryCart(@RequestParam("skuId") Long skuId, Model model) {

        // 1.根据skuId，查询所添加到购物车的商品信息
        Cart cart = this.cartService.queryCartBySkuId(skuId);

        // 2.将cart添加到request域中，以键值对的方式，方便html通过Key获取到后端的Value值
        model.addAttribute("cart", cart);

        // 3.返回的名称需要跟templates\addCart.html名称一样，这样thymeleaf才能通过前缀+后缀拼接出addCart.html，跳转到指定页面
        return "addCart";
    }

    /**
     * 查询购物车
     * 查询成功后，返回视图名称，并将数据模型共享到request域中
     * 然后在页面进行渲染
     */
    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        // 查询购物车，返回的是一个购物车集合
        List<Cart> carts = this.cartService.queryCarts();
        // 将购物车共享到request域中，让页面从域中获取数据模型
        model.addAttribute("carts", carts);
        // 返回视图名称
        return "cart";
    }

    /**
     * 更新购物车
     * - 请求方式：Post
     * - 请求路径：/updateNum
     * - 请求参数：json格式 {skuId: 30, count: 3}
     * - 响应数据：ResponseVo<Object> 或者 ResponseVo
     * ---------------------------------------------
     * 有两种更新：
     * （1）更新商品数量：点击“+”、“-”、或者直接输入数字
     * （2）更新商品状态：多选框是选中还是未选中
     * 需要将userId、skuId、count传递过去
     * 来判定是哪个用户的哪个商品，然后对商品数量进行更行
     * userId不用传递，因为在自定义拦截器已经获取到了
     */
    @PostMapping("updateNum")
    @ResponseBody // 返回Json数据，别忘了加上@ResponseBody注解
    public ResponseVo updateNum(@RequestBody Cart cart) {
        // 参数cart里面传递了skuId商品id、count商品数量，根据cart来进行更新
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    /**
     * 删除购物车
     * - 请求方式：Post
     * - 请求路径：/deleteCart?skuId=30
     * - 请求参数：skuId
     * - 返回结果：无
     */
    @PostMapping("deleteCart")
    public ResponseVo deleteCart(@RequestParam("skuId") Long skuId) {
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    /**
     * 测试自定义spring拦截器
     */
    @GetMapping("testInterceptor")
    @ResponseBody
    public String testInterceptor() {
        // LoginInterceptor的getUserInfo()属于静态方法，可以直接通过类名来调用，里面有ThreadLocal设定好的值
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);
        return "测试自定义spring拦截器!";
        /*
        测试：
        http://cart.gmall.com/testInterceptor
        在执行当前方法，也就是Handler方法之前
        会执行拦截器的前置方法
        也就是会进入到LoginInterceptor类的preHandle中
         */
    }

    /**
     * 测试异步调用
     */
    @GetMapping("testAsync")
    @ResponseBody
    public String testAsync() throws ExecutionException, InterruptedException {
        // 获取当前系统时间，单位毫秒
        long now = System.currentTimeMillis();
        System.out.println("controller.testAsync方法开始执行！");

//        // 开启子线程1
//        this.cartService.executor1();
//        // 开启子线程2
//        this.cartService.executor2();


//        // 获取子线程3的返回结果集
//        Future<String> stringFuture3 = this.cartService.executor3();
//        // 获取子线程4的返回结果集
//        Future<String> stringFuture4 = this.cartService.executor4();
//        System.out.println("stringFuture3的执行结果：" + stringFuture3.get());
//        System.out.println("stringFuture4的执行结果：" + stringFuture4.get());

//        ListenableFuture<String> future5 = this.cartService.executor5();
//        ListenableFuture<String> future6 = this.cartService.executor6();
//        // 捕获子任务5的异常信息
//        future5.addCallback(result ->
//                        System.out.println("调用成功future1: " + result),
//                ex -> System.out.println("调用失败future1：" + ex.getMessage()));
//        // 捕获子任务6的异常信息
//        future6.addCallback(result ->
//                        System.out.println("调用成功future2: " + result),
//                ex -> System.out.println("调用失败future2：" + ex.getMessage()));


        // 异常的统一处理
        this.cartService.executor8();

        // (System.currentTimeMillis() - now，获取执行时间
        System.out.println("controller.test方法结束执行！！！" + (System.currentTimeMillis() - now));

        return "hello libifu!";
    }

    /**
     * 订单确认页所需接口：获取登录用户勾选的购物车
     * ---------------------------------------------------------------
     * 需要传递参数userId来进行，虽然在拦截中将请求拦截下拉，从Cookie中获取到用户信息
     * 但是前提是浏览器发过来的请求，浏览器是携带coolie的
     * 而gmall-order微服务远程调用，是没有coolie的，拦截器无法从coolie中获取用户信息
     * 所以还需要传递userId过来
     */
    @GetMapping("check/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCarts(@PathVariable("userId") Long userId) {
        List<Cart> carts = this.cartService.queryCheckedCarts(userId);
        return ResponseVo.ok(carts);
    }

}
