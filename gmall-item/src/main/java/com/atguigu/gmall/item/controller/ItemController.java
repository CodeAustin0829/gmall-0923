package com.atguigu.gmall.item.controller;


import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/25
 */
@Controller
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * 加载商品详情页
     *
     * @param skuId
     * @return
     */
    @GetMapping("{skuId}.html")
    public String loadData(@PathVariable("skuId") Long skuId, Model model, @RequestHeader("userId") String userId) throws Exception {
        //测试是否获取拦截器的信息
        System.out.println("userId = " + userId);

        ItemVo itemVo = this.itemService.loadData(skuId);
        model.addAttribute("itemVo", itemVo);
        // 在返回动态数据时，先访问静态页面，将封装好数据的itemVo传递给生成静态页面的方法creatHtml
        this.itemService.createHtml(itemVo);
        return "item";
    }
}
