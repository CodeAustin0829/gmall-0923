package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.search.vo.SearchParamVo;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
//@RestController
//@RequestMapping("search")
//public class SearchController {
//
//    @Autowired
//    private SearchService searchService;
//
//    @GetMapping //搜索参数都是拼接在地址栏发送请求，使用Get请求，方法参数直接使用对象来入参，无需加注解
//    public ResponseVo<SearchResponseVo> search(SearchParamVo searchParamVo) {
//        // 自定义搜索查询ES方法，将解析搜索结果集后的结果，返回给前端，进行显示
//        SearchResponseVo searchResponseVo = this.searchService.search(searchParamVo);
//        return ResponseVo.ok(searchResponseVo);
//    }
//}

//以上代码要进行修改，返回一个视图名称进行解析，让页面进行显示数据

@Controller //让返回的视图名称String类型的不会被转换Json，不能使用@RestController
@RequestMapping("search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping //搜索参数都是拼接在地址栏发送请求，使用Get请求，方法参数直接使用对象来入参，无需加注解
    // 返回值是视图名称，thymeleaf方可通过前缀和后缀进行解析，方法参数要追加Model，将数据共享到request域中，然后html就可以可以获取到
    public String search(SearchParamVo searchParamVo, Model model) {
        // 自定义搜索查询ES方法，将解析搜索结果集后的结果，返回给前端，进行显示
        SearchResponseVo searchResponseVo = this.searchService.search(searchParamVo);

        // 将数据添加到request域中，以键值对的方式，方便html通过Key获取到后端的Value值
        model.addAttribute("response", searchResponseVo); //将搜索结果集，共享到request域
        model.addAttribute("searchParam", searchParamVo); //将前端传过来的数据【也就是地址栏参数】，共享到request域
        return "search";

        /**
         * 浏览器测试：
         * http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1,2,3&categoryId=225&priceFrom=1000&priceTo=10000&store=false&props=4:8G-12G&props=5:256G-512G&sort=1&pageNum=2
         */
    }
}
