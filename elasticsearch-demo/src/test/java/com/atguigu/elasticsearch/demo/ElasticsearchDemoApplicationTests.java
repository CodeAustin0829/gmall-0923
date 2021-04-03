package com.atguigu.elasticsearch.demo;

import com.atguigu.elasticsearch.demo.pojo.User;
import com.atguigu.elasticsearch.demo.repository.UserRepository;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class ElasticsearchDemoApplicationTests {

    /**
     * 自动注入ESRest客户端模板
     * ElasticsearchTemplate，是TransportClient客户端
     * ElasticsearchRestTemplate是RestHighLevel客户端
     */
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 自动注入 UserRepository
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 创建索引及映射
     */
    @Test
    void contextLoads() {
        //基于ESRest客户端模板，创建索引库
        this.elasticsearchRestTemplate.createIndex(User.class);
        //基于ESRest客户端模板，创建映射
        this.elasticsearchRestTemplate.putMapping(User.class);
    }

    /**
     * 新建文档【行数据】
     */
    @Test
    void testAdd() {
        /**
         * ElasticsearchRestTemplate 没有提供对新建文档的操作
         * 需要继承ElasticsearchRepository接口来实现
         * （1）自定义一个接口 UserRepository
         * （2）继承 ElasticsearchRepository<T, ID> 接口
         *     泛型1：即返回结果集，也就是查询的返回对象User
         *     泛型2：ID的类型，也就是Long类型
         * （3）在当前类注入UserRepository，进行文档的CRUD
         */
        this.userRepository.save(new User(1616L, "李青廷", 18, "123456"));
    }

    @Test
    void testAddAll() {
        List<User> users = new ArrayList<>();
        users.add(new User(1l, "柳岩", 18, "123456"));
        users.add(new User(2l, "范冰冰", 19, "123456"));
        users.add(new User(3l, "李冰冰", 20, "123456"));
        users.add(new User(4l, "李青廷", 21, "123456"));
        users.add(new User(5l, "小鹿", 22, "123456"));
        users.add(new User(6l, "韩红", 23, "123456"));
        this.userRepository.saveAll(users);
    }

    /**
     * 删除文档【行数据】
     */
    @Test
    void testDelete() {
        this.userRepository.deleteById(1616L);
    }

    /**
     * 查询文档【行数据】，根据id
     */
    @Test
    void testFind() {
//        System.out.println(this.userRepository.findById(1616L));
        /*
        输出结果：
        Optional[User(id=1616, name=李青廷, age=18, password=123456)]
        通过底层Optional类的方法get()，可以拿到对象
         */
        System.out.println(this.userRepository.findById(1616L).get());

    }

    /**
     * 测试自定义查询，范围查询
     */
    @Test
    void testFindByAgeBetween(){
        System.out.println(this.userRepository.findByAgeBetween(20, 30));
    }

    /**
     * 测试自定义查询，范围查询，注解的方式
     */
    @Test
    void testFindByQuery(){
        System.out.println(this.userRepository.findByQuery(20, 30));
    }

    /**
     * 自定义查询构建器QueryBuilder【还是不能完成分页、高亮查询】
     */
    @Test
    void testSearch(){
        Iterable<User> users = this.userRepository.search(QueryBuilders.rangeQuery("age").gte(20).lte(30));
        //System.out.println("users = " + users);
        /*
        输出结果：
        users = Page 1 of 1 containing com.atguigu.elasticsearch.demo.pojo.User instances
         */
        users.forEach(System.out::println);
    }

    /**
     * 原生搜索查询构建器NativeSearchQueryBuilder
     */
    @Test
    void testNative(){
        // 初始化自定义查询对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        // 构建查询[使用工具类QueryBuilders]，根据name来查询
        nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name", "冰冰"));
        /*
        NativeSearchQueryBuilder result = nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name", "冰冰"));
        System.out.println("result = " + result);
        输出结果：
        result = org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder@4462efe1
        */
        // 排序【跟据年龄进行降序排序】
        nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort("age").order(SortOrder.DESC));
        // 分页，默认分页从0开始
        nativeSearchQueryBuilder.withPageable(PageRequest.of(0,2));
        // 高亮
        nativeSearchQueryBuilder.withHighlightBuilder(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        // 执行查询，获取分页结果集
        Page<User> userPage = this.userRepository.search(nativeSearchQueryBuilder.build());
        // 打印总页数
        System.out.println(userPage.getTotalPages());
        // 打印总记录数
        System.out.println(userPage.getTotalElements());
        // 打印当前页数据
        System.out.println(userPage.getContent());
    }
}
