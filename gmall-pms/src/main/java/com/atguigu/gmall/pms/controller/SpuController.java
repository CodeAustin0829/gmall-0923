package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.entity.vo.SpuVO;
import com.atguigu.gmall.pms.service.SpuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * spu信息
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
@Api(tags = "spu信息 管理")
@RestController
@RequestMapping("pms/spu")
public class SpuController {

    @Autowired
    private SpuService spuService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /***
     * 查询商品列表
     * 前端的请求
     * Request URL: http://api.gmall.com/pms/spu/category/0?t=1615307437710&pageNum=1&pageSize=10&key=
     * Request Method: GET
     * （1）请求方式：GET
     * （2）请求路径：/pms/spu/category/0，0是url参数，0在数据库不存在，也就是查询所有
     * （3）请求参数：
     * ① url参数：pms_spu表中的category_id，0代表查询所有
     *           后端接参：@PathVariable
     * ② 请求参数：在url路径后面以?和&的方式传参
     *            后端接参：1、@RequestParam 获取指定一个参数
     *                     2、POJO入参：只要提交的参数的key和pojo的属性名类型一致就可，此方式不需要加注解
     *                                  pageNum、pageSize、key
     *                                  这三个请求参数，对应的pojo类有PageParamVo
     *                                  所以可以用PageParamVo类来接参
     *                     3、Map入参
     *                     4、List<Integer> id接参：比如批量参数 url?id=1,2,3,4
     *  （4）返回值：ResponseVo<PageResultVo>
     *              PageResultVo封装的是分页结果集
     */
    @GetMapping("category/{categoryId}")
    public ResponseVo<PageResultVo> querySpuByCategoryIdOrKey(@PathVariable("categoryId") Long categoryId,
                                                              PageParamVo pageParamVo) {
        //要查询的是PageResultVo，查询后返回的就是PageResultVo
        PageResultVo pageResultVo = spuService.querySpuByCategoryIdOrKey(categoryId, pageParamVo);
        //将得到的pageResultVo，封装到ResponseVo的data属性中，返回给前端
        return ResponseVo.ok(pageResultVo);

    }

    /**
     * ES搜索之分页查询spu，让gmall-search进行远程调用
     * （1）feign远程调用，使用的是Json才能传对象，？后面传参在feign中不能使用对象类接收
     * 参数中添加注解：@RequestBody
     * （2）Json只支持Post请求
     * （3）返回给浏览器，是返回一个分页对象
     * 但是返回给gmall-search保存ES中，只需要传Spu集合集合
     * 所以无需返回分页对象，否则还得再次解析
     * 直接返回List<SpuEntity>即可
     * ----------------------------------------------------------------------------
     * 将此接口，放在gmall-pms-interface中，以供gmall-search进行远程调用
     */
    @PostMapping("/Page")
    @ApiOperation("分页查询")
    public ResponseVo<List<SpuEntity>> querySpuPage(@RequestBody PageParamVo paramVo) {
        PageResultVo pageResultVo = spuService.queryPage(paramVo);
        //从分页对象中，获取列表数据，得到的是List<?>，所以需要强转一下
        List<SpuEntity> spuEntityList = (List<SpuEntity>) pageResultVo.getList();
        return ResponseVo.ok(spuEntityList);
    }


    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySpuByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = spuService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id) {
        SpuEntity spu = spuService.getById(id);

        return ResponseVo.ok(spu);
    }

    /**
     * 保存
     * 大保存：保存spu、sku、营销相关信息
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SpuVO spu) {
        spuService.bigSave(spu);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SpuEntity spu) {
        spuService.updateById(spu);
        // 修改价格的时候，向消息队列发送消息，然后cart监听消息队列中的消息，对价格进行修改
        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE", "item.update", spu.getId());
        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        spuService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
