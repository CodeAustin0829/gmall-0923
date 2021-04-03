package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.vo.SearchParamVo;
import com.atguigu.gmall.search.vo.SearchResponseAttrVo;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
@Service
public class SearchService {

    /**
     * 要查询高亮、分页数据，使用ES原生的客户端RestHighLevelClient
     */
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            /*
            2. 提供search()方法的参数一：创建SearchRequest对象
            构造方法参数1：String[] indices，可以指定要搜索的是哪一个索引库
            构造方法参数2：SearchSourceBuilder source，用来构建各种查询：匹配查询、高亮查询、排序查询... ...
                          专门提供一个方法，来进行构建
             */
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.builder(searchParamVo));

            /*
             1. 调用ES原生客户端提供的search()方法，使用Search API执行搜索请求
            参数一：SearchRequest searchRequest，搜索请求
            参数二：RequestOptions options，请求选项，一般使用默认的，RequestOptions.DEFAULT
            完成搜索后，会返回一个搜索响应对象searchResponse
            还需要进行解析，封装在vo类中，返回给前端进行显示
            专门提供一个方法parseResult()，来进行解析
             */
            SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 3.调用解析搜索结果集的方法parseResult()
            SearchResponseVo searchResponseVo = this.parseResult(searchResponse);
            /*
            在parseResult()中，可以解析出Vo类SearchResponseVo所需要的属性数据
            但是：
            private Integer pageNum;
            private Integer pageSize;
            这两个数据在ES中并没有，而是存在于前端传过来的参数，也就是SearchParamVo searchParamVo中
             */
            searchResponseVo.setPageNum(searchParamVo.getPageNum());
            searchResponseVo.setPageSize(searchParamVo.getPageSize());
            // 将解析搜索结果集的searchResponseVo，返回给controller，继而返回前端
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * （1）构建各种查询DSL语句
     */
    public SearchSourceBuilder builder(SearchParamVo searchParamVo) {
        // 1.创建SearchSourceBuilder对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 根据分析DSL语句，需要构建5种查询
        /**
         * 一、“query”，查询和过滤条件的构建
         * bool查询下
         * ① must 匹配查询
         * ② filter 过滤查询
         *          a：品牌过滤
         *          b：分类过滤
         *          c：价格区间过滤
         *          d：是否有货过滤
         *          f：规格参数的嵌套过滤
         */
        // 1.1 获取关键字
        String keyword = searchParamVo.getKeyword();
        // 1.2 对关键字进行判空，如果为空：打广告 or return null
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        // 1.3 构建布尔查询，通过工具类QueryBuilders
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 将布尔查询，放置到query查询中，完成布尔查询的构建
        searchSourceBuilder.query(boolQueryBuilder);
        // 1.4 构建must匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.5 构建filter品牌过滤
        // 1.5.1 获取品牌Id
        List<Long> brandId = searchParamVo.getBrandId();
        // 1.5.2 对品牌Id进行判空，不为空则设置到布尔查询的filter过滤中
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }

        // 1.6 构建filter分类过滤
        // 1.6.1 获取分类id
        List<Long> cid = searchParamVo.getCid();
        // 1.6.2 对分类Id进行判空，不为空则设置到布尔查询的filter过滤中
        if (!CollectionUtils.isEmpty(cid)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId", cid));
        }

        // 1.7 构建filter价格区间过滤
        // 1.7.1 获取起始价格、结束价格
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        // 1.7.2 对起始价格、结束价格进行判空，如果两个都为空，则不设置过滤，如果有一个不为空，则设置过滤
        if (priceFrom != null || priceTo != null) {
            // 设置到range过滤，根据价格“price”
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            // 当起始价格不为空，结束价格为空，设置大于等于到range过滤查询中
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            // 当起始价格为空，结束价格不为空，设置小于等于到range过滤查询中
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            // 将range过滤查询设置到布尔查询中
            boolQueryBuilder.filter(rangeQuery);
        }

        // 1.8 构建filter是否有货过滤
        // 1.8.1 获取库存
        Boolean store = searchParamVo.getStore();
        // 1.8.2 对库存进行判空【实际开发中会判断是true还是false来进行过滤】
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 1.9 构建filter规格参数过滤
        // 1.9.1 获取规格参数
        List<String> props = searchParamVo.getProps();
        // 1.9.2 对规格参数进行判空，如果不为空，则进行过滤条件的设置
        if (!CollectionUtils.isEmpty(props)) {
            // 1.9.3 props是个集合，遍历出每个规格参数对象
            props.forEach(prop -> {
                // 分析京东的路径参数，该项目中将来的规格参数，会以这样的方式进行传递props=5:8-12,5:256G
                // “:”前面是attrId规格id，“:”后面是规格参数的值，并且是以“-”进行分隔的
                // 1.9.4 使用split()方法，对规格参数对象prop进行分隔
                String[] attrs = StringUtils.split(prop, ":");
                // 1.9.5 对attrs进行判空，同时它的长度要等于两位5:8-12
                if (attrs != null && attrs.length == 2) {
                    // 1.9.6 在嵌套查询里面，还有一个布尔查询，还需要再new出来一个，跟上面的大布尔查询不是同一个
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 1.9.7 将满足条件的、同时被“:”分隔后的attrs，取它索引为0的值，也是attrId规格id，设置到小布尔查询中
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    // 1.9.8 对“:”后面的参数值再次进行分隔，分隔符是“-”
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    // 1.9.9 将规格参数值，设置到小布尔查询中
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    // 1.9.10 将小布尔查询，设置到filter的嵌套查询中，
                    // 第一个参数是path【searchAttrs】，第二个参数是要设置的查询，第三参数是得分模式，过滤模式不影响得分，设置为ScoreMode.None
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                    /**
                     * 浏览器测试：
                     * http://localhost:18086/search?keyword=手机&brandId=1,2,3&categoryId=225&priceFrom=1000&priceTo=10000&store=false&props=4:8G-12G&props=5:256G-512G
                     */
                }
            });
        }

        /**
         * 二、“sort”，排序条件的构建
         * 0-默认，得分降序；1-按价格降序；2-按价格升序；3-按创建时间降序；4-按销量降序
         */
        // 2.1 获取排序
        Integer sort = searchParamVo.getSort();
        // 2.2 对排序进行非空判断
        if (sort != null) {
            // 2.3 使用分支结构：switch选择结构，设置排序查询条件
            switch (sort) {
                case 1:
                    searchSourceBuilder.sort("price", SortOrder.DESC);
                    break;
                case 2:
                    searchSourceBuilder.sort("price", SortOrder.ASC);
                    break;
                case 3:
                    searchSourceBuilder.sort("createTime", SortOrder.DESC);
                    break;
                case 4:
                    searchSourceBuilder.sort("sales", SortOrder.DESC);
                    break;
                default:
                    //其它数字，使用得分降序排序
                    searchSourceBuilder.sort("_score", SortOrder.DESC);
                    break;
            }
        }

        /**
         * 三、“from”、“size”，分页条件的构建
         */
        // 获取分页页码、每页显示记录数
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        // 设置当前页【从第几条数据开始查】：公式(pageNum - 1) * pageSize
        searchSourceBuilder.from((pageNum - 1) * pageSize);
        // 设置每页显示的记录数
        searchSourceBuilder.size(pageSize);

        /**
         * 四、“highlight”，高亮条件的构建
         */
        searchSourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));

        /**
         * 五、“aggs”，聚合条件的构建
         * ① 品牌的聚合
         * ② 分类的聚合
         * ③ 规格参数的嵌套集合
         */
        // 5.1 构建品牌聚合
        // brandNameAgg聚合、logoAgg聚合是brandIdAgg聚合的子聚合，使用subAggregation进行构建即可
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        // 5.2 构建分类聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        // 5.3 构建规格参数的嵌套聚合
        // attrIdAgg聚合是嵌套聚合attrAgg的子聚合，attrNameAgg聚合、attrValueAgg聚合又是attrIdAgg聚合的子聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        /**
         * 浏览器测试：
         * http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1,2,3&categoryId=225&priceFrom=1000&priceTo=10000&store=false&props=4:8G-12G&props=5:256G-512G&sort=1&pageNum=2
         */

        /**
         * 六、构建结果集过滤
         */
        searchSourceBuilder.fetchSource(new String[]{"skuId", "title", "subTitle", "price", "defaultImage"}, null);

        // 2.打印searchSourceBuilder，打印的是DSL语句
        System.out.println(searchSourceBuilder);
        // 3.返回searchSourceBuilder给调用者
        return searchSourceBuilder;
    }


    /**
     * （2）解析搜索结果集，对搜索响应对象进行解析，最终要返回给前端进行显示
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        // 创建SearchResponseVo类，用来封装解析搜索结果集的结果
        SearchResponseVo searchResponseVo = new SearchResponseVo();

        /**
         * 一、解析SearchResponse searchResponse中的hits
         * 获取SearchResponseVo类需要的属性：
         * Long total 总记录数、List<Goods> goods 商品列表
         */
        //（1）从搜索响应对象searchResponse中，获取hits
        SearchHits hits = searchResponse.getHits();

        //（2）从hits中，获取total总记录数，并设置到SearchResponseVo类的属性中
        searchResponseVo.setTotal(hits.getTotalHits());

        //（3）在hits的里面，又有一个hits，里面封装所有Goods对象
        SearchHit[] hitsHits = hits.getHits();


        //（5）将获取到的List<Goods>，设置到SearchResponseVo类的属性中
        searchResponseVo.setGoodsList(
                //（4）对封装所有Goods对象的数组hitsHits，进行遍历，取出每个Goods对象，使用Stream流【数组一般使用Stream.of()来构建stream流】
                Stream.of(hitsHits).map(hitsHit -> {
                    // 一般是new一个Goods，来接收数据的转换，但是在此处，可以通过hits\hits\_source里面的数据，里面封装了Goods对象
                    String goodsJson = hitsHit.getSourceAsString();
                    // 但是得到的是Json数据，需要反序列化成Goods对象
                    Goods goods = JSON.parseObject(goodsJson, Goods.class);
                    // Goods对象的title不是高亮的，还需要将hits\highlight\title赋值给Goods对象的title
                    Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                    // 拿到的高亮字段是一个Map，可以通过Key，取出对应的Value值
                    HighlightField highlightField = highlightFields.get("title");
                    // 拿到高亮字段之后，再通过getFragments()方法[返回的是一个数组，取出索引为0的，就是高亮标题]，获取高亮title
                    String highlightTitle = highlightField.getFragments()[0].toString();
                    // 将高亮标题，赋值给Goods对象的title
                    goods.setTitle(highlightTitle);
                    // 将goods返回到stream流中，也就是新的list集合中
                    return goods;
                }).collect(Collectors.toList())
        );

        /**
         * 二、解析SearchResponse searchResponse中的aggregations
         * 获取SearchResponseVo类需要的属性：
         * List<BrandEntity> brands 品牌过滤条件
         * List<CategoryEntity> categories 分类过滤条件
         * List<SearchResponseAttrVo> filters 规格参数过滤条件
         */
        /*
        1.
        先从搜索响应对象searchResponse中，获取aggregations，
        为了可以通过key获取品牌、分类、规格的聚合value，使用asMap()
        得到一个aggregations的Map集合
         */
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();

        // 2.解析品牌聚合结果集，获取品牌
        /*
        2.1
        从aggregationMap中，
        通过品牌Id聚合名【Key】，获取品牌Id聚合
        返回的是顶层接口类型Aggregation brandIdAgg
        由于Id是Long类型
        可以使用Aggregation的具体实现类ParsedLongTerms来接收
         */
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");

        /*
         2.2
         有了品牌Id聚合以后，就可以获取到里面的聚合内容，也就是桶聚合buckets
         在桶聚合buckets中，封装了多个品牌对象
         */
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();

        // 2.3 对获取到的桶聚合进行非空判断
        if (!CollectionUtils.isEmpty(buckets)) {
            // 2.4 当桶聚合buckets不为空时，将桶对象转换为品牌对象
            buckets.stream().map(bucket -> {
                // 2.5 创建一个品牌对象，用来接收转换
                BrandEntity brandEntity = new BrandEntity();
                // 2.7 设置品牌Id：获取bucket中的key，这个key就是品牌的id
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 2.8 设置品牌名称：解析buckets的子聚合brandNameAgg，通过解析子聚合，再获取品牌名称
                Map<String, Aggregation> brandSubAggMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                // 2.8.1 通过聚合名，获取子聚合brandNameAgg
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandSubAggMap.get("brandNameAgg");
                // 2.8.2 再获取brandNameAgg里面的桶聚合
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                // 2.8.3 对名称桶聚合进行非空判断
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    // 2.8.4 取出名称桶聚合的第一个元素，也就是key，就是品牌的名称
                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }

                // 2.9 设置品牌Logo：通过聚合名，获取子聚合logoAgg
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandSubAggMap.get("logoAgg");
                // 2.9.1 再获取logoAgg里面的桶聚合
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                // 2.9.2 对logo桶聚合进行非空判断
                if (!CollectionUtils.isEmpty(logoAggBuckets)) {
                    // 2.9.3 取出名称桶聚合的第一个元素，也就是key，就是品牌的名称
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                // 2.6 将品牌对象返回到stream流中，封装到新的List集合
                return brandEntity;
            }).collect(Collectors.toList());
        }

        // 3.解析聚合结果集，获取分类
        // 3.1 通过分类Id聚合名称，从aggregationMap中获取分类Id聚合categoryIdAgg
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        // 3.2 获取分类Id聚合下面的桶聚合
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        // 3.3 对分类Id桶聚合进行非空判断
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            // 3.4 分类Id桶聚合不为空，将分类Id桶对象，转换为分类对象
            List<CategoryEntity> categoryEntityList = categoryIdAggBuckets.stream().map(categoryIdAggBucket -> {
                // 3.5 创建分类对象
                CategoryEntity categoryEntity = new CategoryEntity();
                // 3.7.1 设置分类id，获取桶聚合Key，就是分类的id
                long categoryId = ((Terms.Bucket) categoryIdAggBucket).getKeyAsNumber().longValue();
                // 3.7.2 设置分类名称，获取子聚合attrValueAgg
                ParsedStringTerms categoryNameAgg =
                        (ParsedStringTerms) ((Terms.Bucket) categoryIdAggBucket).getAggregations().get("categoryNameAgg");
                // 3.7.3 获取子聚合attrValueAgg里面的桶聚合中第一个元素，也就是Key，也就是分类名称
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                // 3.6 返回分类对象到stream流，封装成新的List集合
                return categoryEntity;
            }).collect(Collectors.toList());
            // 3.8 将categoryEntityList设置到SearchResponseVo类的属性中
            searchResponseVo.setCategories(categoryEntityList);
        }

        // 4. 解析嵌套聚合结果集，获取嵌套规格参数
        // 4.1 通过嵌套聚合名称，从aggregationMap中获取嵌套聚合attrAgg
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 4.2 从attrAgg获取其子聚合attrIdAgg
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        // 4.3 获取attrIdAgg的桶
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        // 4.4 对attrIdAggBuckets进行判空
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            // 4.5 将桶对象，转换为规格参数对象
            List<SearchResponseAttrVo> searchResponseAttrVoList = attrIdAggBuckets.stream().map(attrIdAggBucket -> {
                // 4.5.1 创建规格参数对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 4.5.3 设置规格参数Id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) attrIdAggBucket).getKeyAsNumber().longValue());
                // 4.5.4 设置规格参数的名称
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) ((Terms.Bucket) attrIdAggBucket).getAggregations().get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                // 4.5.5 设置规格参数值
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) ((Terms.Bucket) attrIdAggBucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)) {
                    // 4.5.6 将桶中的Key，也就是规格参数值，转换为一个List数组，因为值有多个
                    List<String> attrValues = attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    searchResponseAttrVo.setAttrValues(attrValues);
                }
                //4.5.2 返回规格参数对象
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setFilters(searchResponseAttrVoList);
        }

        // 最终返回searchResponseVo对象，给前端页面进行显示
        return searchResponseVo;
    }
}
