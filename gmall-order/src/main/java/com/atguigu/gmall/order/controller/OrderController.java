package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/31
 */
@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 处理订单结算页数据查询接口
     * 订单确认页面联调，跳转到 trade.html页面
     */
    @GetMapping("confirm")
    public String confirm(Model model) {
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

}
