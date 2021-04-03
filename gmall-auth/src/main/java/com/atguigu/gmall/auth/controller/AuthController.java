package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 视图解析器：跳转到登录页面login.html，让用户进行登录
     * returnUrl是指记录登录前的页面，等用户登录成功后，会回到当前页面
     * 当跳转路径为空时，指定一个默认跳转路径http://gmall.com
     */
    @GetMapping("toLogin.html")
    public String toLogin(@RequestParam(value = "returnUrl", defaultValue = "http://gmall.com") String returnUrl, Model model) {
        // 把登录前的页面地址，记录到登录页面，以备将来登录成功，回到登录前的页面
        model.addAttribute("returnUrl", returnUrl);
        return "login"; // 通过视图解析器解析跳转到登录的页面login.html
    }

    /**
     * 处理用户登录的请求
     */
    @PostMapping("login")
    public String login(
            @RequestParam("loginName") String loginName,
            @RequestParam("password") String password,
            @RequestParam("returnUrl") String returnUrl,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        this.authService.login(loginName, password, httpServletRequest, httpServletResponse);
        //在service校验登录成功后，跳转重定向回登录前的页面
        return "redirect:" + returnUrl;
    }
}
