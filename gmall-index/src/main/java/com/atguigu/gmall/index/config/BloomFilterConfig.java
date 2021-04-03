package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsFeign;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/23
 */
@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsFeign gmallPmsFeign;

    private static final String KEY_PREFIX = "index:category:[";

    @Bean
    public RBloomFilter rBloomFilter() {
        // 1.初始化基于redisson实现的布隆过滤器
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("index:bloomFilter");

        // 2.为布隆过滤器设置插入数据及误判率
        bloomFilter.tryInit(1000L, 0.03);

        // 3.远程调用PMS查询分类数据
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsFeign.queryCategoryByParentId(0L);
        List<CategoryEntity> categoryEntityList = listResponseVo.getData();
        // 4.遍历一级分类集合，将一级分类id【也就是pid=0】设置到布隆过滤器中
        if (!CollectionUtils.isEmpty(categoryEntityList)) {
            categoryEntityList.forEach(categoryEntity -> {
                bloomFilter.add(KEY_PREFIX + categoryEntity.getId()+"]");
            });
        }
        return bloomFilter;
    }
}
