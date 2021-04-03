package com.atguigu.elasticsearch.demo.repository;

import com.atguigu.elasticsearch.demo.pojo.User;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/14
 */
public interface UserRepository extends ElasticsearchRepository<User, Long> {

    /**
     * 自定义查询，范围查询
     * 根据年龄区间查询
     * @param age1
     * @param age2
     * @return
     */
    List<User> findByAgeBetween(Integer age1, Integer age2);


    /**
     * 自定义查询，范围查询
     * 根据年龄区间查询
     * 当方法名不符合模板时，使用注解来指定DSL语句
     * ?0、?1表示占位符
     * @param age1
     * @param age2
     * @return
     */
    @Query("{\n" +
            "    \"range\": {\n" +
            "      \"age\": {\n" +
            "        \"gte\": \"?0\",\n" +
            "        \"lte\": \"?1\"\n" +
            "      }\n" +
            "    }\n" +
            "  }")
    List<User> findByQuery(Integer age1, Integer age2);
}
