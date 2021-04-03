package com.atguigu.thymeleaf.demo.controller;

import com.atguigu.thymeleaf.demo.pojo.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/17
 */
@Controller
@RequestMapping
public class HelloController {

    /**
     * SpringMVC：
     * 在Controller的方法中，添加Map类型、Model类型、ModelMap类型的参数
     * 都可以直接用来保存域数据到Request对象中
     */
    @GetMapping("test")
    public String test(Model model) {
        User user = new User("Austin", 18, new User("CHY", 18, null));
        model.addAttribute("Key", "Value，将数据保存到Request域中");
        model.addAttribute("user", user);
        return "LiBiFu"; //视图解析器，return的名称，跟templates\LiBiFu.html一样，才能会被解析到
    }
}
