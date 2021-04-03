package com.atguigu.elasticsearch.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/14
 */
@Data
@AllArgsConstructor //全参构造器
@NoArgsConstructor  //无参构造器
/**
 * `@Document` 作用在类，标记实体类为文档对象，一般有四个属性
 * - indexName：对应索引库名称
 * - type：对应在索引库中的类型【也就是MySQL中的表】
 * - shards：分片数量，默认5
 * - replicas：副本数量，默认1
 */
@Document(indexName = "user", type = "info", shards = 3, replicas = 2)
public class User {
    /**
     * `@Id` 作用在成员变量，标记一个字段作为id主键，会映射到ES中的"_id"
     */
    @Id
    private Long id;

    /**
     * `@Field` 作用在成员变量，标记为文档的字段，并指定字段映射属性：
     * - type：字段类型，取值是枚举：FieldType
     * 一般取Java属性中的类型，只有String类型时，才会有Text（分词）、Keyword（不分词）的选择
     * - index：是否索引，布尔类型，默认是true【所以如果要建立索引时，可以省略不写】
     * - store：是否存储，布尔类型，默认是false【一般省略不写】
     * - analyzer：分词器名称：ik_max_word
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Integer)
    private Integer age;

    @Field(type = FieldType.Keyword)
    private String password;
}
