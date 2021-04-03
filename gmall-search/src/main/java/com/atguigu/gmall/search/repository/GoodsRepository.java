package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
