package com.atguigu.gmall.index.controller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/20
 */
@Controller //让返回的视图名称String类型的不会被转换Json，不能使用@RestController
public class IndexController {

    @Autowired
    private IndexService indexService;

    /**
     * 跳转到首页，无需在域名后面加路径
     */
    @GetMapping
    // 返回值是视图名称，thymeleaf方可通过前缀和后缀进行解析，方法参数要追加Model，将数据共享到request域中，然后html就可以获取到
    public String toIndex(Model model) {

        //查询商品一级分类，查询返回来的是List<CategoryEntity>
        List<CategoryEntity> categoryEntityList = this.indexService.queryLvl1Categories();

        // 将数据添加到request域中，以键值对的方式，方便html通过Key获取到后端的Value值
        model.addAttribute("categories", categoryEntityList);

        // TODO: 加载广告

        // 返回的名称需要跟templates\index.html名称一样，这样thymeleaf才能通过前缀+后缀拼接出index.html。跳转到指定页面[首页]
        return "index";
    }

    /**
     * 首页查询二级分类及三级分类
     *
     * @param pid
     * @return
     */
    @ResponseBody
    @GetMapping("index/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSub(@PathVariable("pid") Long pid) {
        List<CategoryEntity> categoryEntityList = this.indexService.queryLvl2CategoriesWithSub(pid);
        return ResponseVo.ok(categoryEntityList);
    }

    /**
     * 锁的压力测试
     *
     * @return
     */
    @ResponseBody
    @GetMapping("index/testlock")
    public ResponseVo testlock() {
        this.indexService.testlock();
        return ResponseVo.ok();
    }

    /**
     * 写的测试
     */
    @ResponseBody
    @GetMapping("index/write")
    public ResponseVo write() {
        this.indexService.writeLock();
        return ResponseVo.ok();
    }

    /**
     * 读的测试
     */
    @ResponseBody
    @GetMapping("index/read")
    public ResponseVo read() {
        this.indexService.readLock();
        return ResponseVo.ok();

    }

    /**
     * 闭锁（CountDownLatch）的测试
     * 等待latch
     */
    @ResponseBody
    @GetMapping("index/latch")
    public ResponseVo countDownLatch(){
        this.indexService.latch();
        return ResponseVo.ok("班长等待锁门... ...");
    }


    /**
     * 闭锁（CountDownLatch）的测试
     * 计数
     */
    @ResponseBody
    @GetMapping("index/out")
    public ResponseVo countDownLatchOut(){
        this.indexService.countDown();
        return ResponseVo.ok("一个同学出门了... ...");
    }
}
