package com.atguigu.gmall.pms.service.impl;


import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.SkuVo;
import com.atguigu.gmall.pms.entity.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.entity.vo.SpuVO;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescService spuDescService;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 查询商品列表
     */
    @Override
    public PageResultVo querySpuByCategoryIdOrKey(Long categoryId, PageParamVo pageParamVo) {

        //（2）第二步：创建查询条件
        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();

        //（4）第四步：往查询条件设定条件之一，设定分类id--category_id
        // 对categoryId进行判断，如果分类id不为0，要根据分类id查，否则查全部
        // 不能判断categoryId=0，然后设定到查询条件queryWrapper.eq("category_id",categoryId)，因为数据库中没有0这个值
        // 如果categoryId=0，就不用添加定查询条件中，直接让queryWrapper为空就好，查询出全部数据
        if (categoryId != 0) {
            queryWrapper.eq("category_id", categoryId);
        }

        //（3）第三步：往查询条件设定条件之一，设定搜索关键字key，存放在PageParamVo对象中
        //① 从PageParamVo对象中，获取key
        String key = pageParamVo.getKey();
        /*
        ② 对key进行判断，因为用户在查询的时候，不一定使用搜索关键字查询，
        有可能搜索关键字为空，就会查询全部数据
        判断字符串不为空的方法：
        StringUtils.isNotEmpty(key)，只判断判断null和0的情况，没有考虑空格的情况
        StringUtils.isNotBlank(key)，使用这个，因为在底层源码中，还对空格进行了判断
         */
        if (StringUtils.isNotEmpty(key)) {
            /**
             * 当检索条件key不为空的时候，也就是用户输入了检索条件，
             * 将检索条件key，设定到查询条件queryWrapper，
             * 根据检索条件查
             * ---------------------------------------------------------------------------
             * 而检索条件key是支持两种关键字——商品id、商品名name来查询的
             * 对应的SQL语句，是第三个：
             * （1）SELECT * FROM pms_spu WHERE id=7 OR `name` LIKE "%7%"
             * （2）SELECT * FROM pms_spu WHERE category_id=225 AND id=7 OR `name` LIKE "%7%"
             * （3）SELECT * FROM pms_spu WHERE category_id=225 AND (id=7 OR `name` LIKE "%7%")
             * 注意category_id跟key是and的关系，在第四步也对category_id进行判断
             * 而key里面的id跟name是or的关系
             * 要加上()，先or，再and
             * ---------------------------------------------------------------------------
             * id=7 OR `name` LIKE "%7%"转为对应的java代码：【中间加or()，不然默认是and()】
             * queryWrapper.eq("id",key).or().like("name",key)
             * 但是这样还达不到加()的效果，走的SQL就是第二种先and再or了
             * 要达到先or再and的效果，也就是加上()
             * 在java中，需要使用到消费型函数接口
             * queryWrapper.and(t -> t.eq("id",key).or().like("name",key))
             * 就可以达到在and后面加()的效果
             * 当作工具类来使用即可
             * 例如：SELECT * FROM pms_spu WHERE category_id=225 AND (id=7 OR (`name` LIKE "%7%"))
             * 如果是在or后面加()，也就是如此写法
             * queryWrapper.and(t -> t.eq("id",key).or(t1 -> t1.like("name",key)))
             * 也就是将条件语句扔到箭头函数里面即可
             */
            queryWrapper.and(t -> t.eq("id", key).or().like("name", key));
        }


        /*
        （1）第一步：
        调用IService的翻页查询方法：
        default <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper)
        当前类继承了IService，可以通过this来调用
        返回值得到的是泛型E，也就是IPage对象
        需要两个参数：
        （1）第一个参数：翻页对象：E类型的page对象，也就是IPage类型的page对象
        （2）第二个参数：查询条件：queryWrapper
        --------------------------------------------------------------------
        但是前端传过来的参数是PageParamVo pageParamVo，
        里面封装了前端传过来的请求参数pageNum、pageSize、key
        这时
        需要将PageParamVo对象，转为MyBatisPlus需要的IPage对象
        而在PageParamVo类中，创建了一个方法getPage()
        其中返回值就是return new Page<>(pageNum, pageSize);
        所以，就可以将PageParamVo对象，转为IPage对象
        --------------------------------------------------------------------
        但是，在pageParamVo里面，还有一个key[搜索关键字]没有用到
        还需要将key设定到查询条件queryWrapper中
        还有categoryId，也就是前端传过来的查询条件，也设定到查询条件queryWrapper中
        第一个参数搞定，进入第二步，提供第二个参数
         */
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                queryWrapper
        );

        /*
        （5）第五步：
        将封装好page对象和查询条件queryWrapper的IPage<SpuEntity> page
        转换回PageResultVo对象
        因为返回去给前端的是ResponseVo<PageResultVo>
        所以还需要转换一下
        而在PageResultVo类中，提供了一个构造函数来转换：
        public PageResultVo(IPage page)
        直接将IPage<SpuEntity> page放进去即可
         */
        return new PageResultVo(page);
    }

    // 大保存：保存spu、sku、营销相关信息
    /**
     * 底层源码：
     * Propagation propagation() default Propagation.REQUIRED;
     * 注解 @Transactional 不指定传播行为，默认使用的是REQUIRED
     * (propagation = Propagation.REQUIRES_NEW)
     * 可以指定传播行为
     * -----------------------------------------------------------------------
     * 注解 @Transactional 是spring提供的
     * 底层是 AOP 来实现的，而 AOP 的底层又是 动态代理【JDK、CGLIB】
     * 也就是：@Transactional →  AOP  →  动态代理
     * -----------------------------------------------------------------------
     * 而事务的传播行为是指：
     * 一个service的方法调用了另一个service的方法，两个service的方法事务之间的影响
     * -----------------------------------------------------------------------
     * （1）bigSave(SpuVO spu)方法上加上@Transactional，默认的传播行为是REQUIRED
     * （2）在bigSave(SpuVO spu)方法里面。在saveSpuDesc(spu, spuId)方法里面加上
     *  注解 @Transactional(propagation = Propagation.REQUIRES_NEW)
     *  也就是（1）方法跟（2）方法不是同一个传播行为
     *  按理来说，
     *  int i = 1 / 0; 是在（2）方法执行完后出现的异常，而又跟（1）不是同一传播行为
     *  是不会回滚的
     *  但是经过测试，（2）方法依然会回滚
     *  -----------------------------------------------------------------------
     *  为什么？
     *  是因为bigSave(SpuVO spu)方法（1）方法
     *  跟saveSpuDesc(spu, spuId)方法（2）方法
     *  是在同一个service方法中，也就是在同一个类中
     *  而事务的传播行为指的是：
     *  一个service的方法调用了另一个service的方法，两个service的方法事务之间的影响
     *  所以（2）方法的事务注解压根就没有生效
     *  因为在同一个Service方法里面，使用的就是同一个事务
     *  -------------------------------------------------------------------------
     *  将
     *  this.saveSpuDesc(spu, spuId)
     *  改为
     *  this.spuDescService.saveSpuDesc(spu, spuId);
     *  让两个方法都属于在不同的Service中
     *  这样两个方法之间的事务传播行为就会成效
     *  重新测试，查看数据库，（2）方法不会回滚了
     *  ---------------------------------------------------------------------------
     *  所有被注入到spring容器中的对象，都是代理对象
     *  也就是
     *      @Autowired
     *     private SpuDescService spuDescService;
     *  自动注入的不是SpuDescService本身，而是SpuDescService的代理对象【JDK或CGLIB】
     *  spring ioc默认的都是原生对象  只有通过aop增强的对象才是代理对象
     *  有@Transactional  注解或者配置文件
     *  有配置接口aop增强的类   得到的对象都是代理对象
     *  也就是spuDescService是代理对象
     *  只要是代理对象
     *  事务注解才会成效
     *  aop对@Transactional做了切面增强 ，注解才会生效
     */
    @Override
//    @Transactional
    @GlobalTransactional //全局事务
    public void bigSave(SpuVO spu) {
        //（1）第一步：保存spu信息
        // 1.1. 保存到pms_spu
        Long spuId = saveSpu(spu);

        // 1.2. 保存到pms_spu_attr_value
        saveBaseAttr(spu, spuId);


        // 1.3. 保存到pms_spu_desc
//        this.saveSpuDesc(spu, spuId); 为了测试事务的传播行为，该从SpuDescService中调用
        this.spuDescService.saveSpuDesc(spu, spuId);

        /**
         * 在saveSpuDesc方法添加跟bigSave不一样的事务传播行为
         * 然后在saveBaseAttr方法下面制造一个异常
         */
//        int i = 1 / 0;

        //（2）第二步：保存sku信息
        savaSku(spu, spuId);

        /*
        当完成了大保存，保存了spu后，向消息中间件MQ发送消息【向 RabbitTemplate 发送】
        第一个参数：交换机名称，一般是“项目名_功能名_EXCHANGE”
        第二个参数：路由键
        第三个参数：发送的消息内容，此处发spuId，然后根据spuId，查询出spu下面的所有的sku，更新ES的索引库即可
         */
        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);
    }

    //（2）第二步：保存sku信息
    private void savaSku(SpuVO spu, Long spuId) {
        // 1.1. 保存到pms_sku【保存顺序不能颠倒，因为要拿到sku_id，才能保存下面这两张表】
        // 1.1.1. sku的数据，保存在SpuVO的属性中private List<SkuVo> skus;
        List<SkuVo> skuVos = spu.getSkus();
        // 1.1.2. 对skuVos进行判断，如果的集合为空，直接return结束，不执行下面的保存了，因为下面的保存都是跟sku相关的
        if (CollectionUtils.isEmpty(skuVos)) {
            return;
        }
        /*
        1.1.3
        如果skuVos不为空，将sku数据保存到数据库中，但是下面的保存需要用到skuId
        所以不能使用IService中的批量保存方法saveBatch()来批量保存
        因为MyBatis-plus的批量保存，无法返回id
        所以要对skuVos进行遍历，一个一个对象进行保存
        遍历skuVos集合，取出每个对象，赋值给临时变量skuVo
         */
        skuVos.forEach(skuVo -> {
            // 1.1.4 数据库表pms_sku，对应的实体类是SkuEntity，所以要将skuVo的属性拷贝到skuEntity中
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(skuVo, skuEntity);
            // 除了对拷的属性，还要设定sku所属的spu的id、品牌和分类的id也需要从spu中获取，因为前端传过来的数据中没有
            skuEntity.setSpuId(spuId);
            //品牌id
            skuEntity.setBrandId(spu.getBrandId());
            //分类id
            skuEntity.setCategoryId(spu.getCategoryId());
            //pms_sku表的字段是默认图片default_image，而前端传过来的是images集合数组，所以还有获取一下，手动设置默认图片
            List<String> images = skuVo.getImages();
            //如果获取到的图片列表不为空，设置默认图片
            if (!CollectionUtils.isEmpty(images)) {
                //设置默认图片，如果用户在前端传了一张默认图片过来，则使用其当作默认图片；没有传，则取图片列表中的第一张图片为默认图片
                skuEntity.setDefaultImage(StringUtils.isNotBlank(skuEntity.getDefaultImage()) ? skuEntity.getDefaultImage() : images.get(0));
            }
            // 1.1.5 将skuEntity保存到数据库中
            this.skuMapper.insert(skuEntity);

            // 1.1.6 获取skuId，以便执行下面的保存操作
            Long skuId = skuEntity.getId();

            // 1.2. 保存到pms_sku_images
            // 上面已经获取图片列表List<String> images，可以进行批量插入到表中
            // 1.2.1 进行判断，当images不为空，才进行批量插入操作
            if (!CollectionUtils.isEmpty(images)) {
                // 1.2.3 使用Stream流，将List<String> images，转换为Collection <SkuImagesEntity>
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                            SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                            //根据数据库表来进行插入
                            skuImagesEntity.setSkuId(skuId);
                            //url实际上就是image，因为是图片地址
                            skuImagesEntity.setUrl(image);
                            //sort
                            skuImagesEntity.setSort(0);
                            //默认图片default_status，默认图[0 - 不是默认图，1 - 是默认图]
                            //进行比较，当skuEntity.getDefaultImage() 跟 image，则是默认图片，否则不是
                            skuImagesEntity.setDefaultStatus(StringUtils.equals(skuEntity.getDefaultImage(), image) ? 1 : 0);
                            return skuImagesEntity;
                        }
                ).collect(Collectors.toList());

                /**
                 * 1.2.2 将images批量插入
                 * 但是这么写：
                 * this.skuImagesService.saveBatch(images);
                 * 会报错
                 * 因为要保存的是Collection <SkuImagesEntity>
                 * 而images 是List<String>
                 * 需要使用stream流进行转换一下
                 */
                this.skuImagesService.saveBatch(skuImagesEntities);
            }


            // 1.3. 保存到pms_sku_attr_value
            //sku的规格参数（销售属性），保存在SkuVo的属性中private List<SkuAttrValueEntity> saleAttrs;
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            //前端数据中，并没有传递skuId过来，所以要手动设置一下，遍历集合saleAttrs，得到对象，依次添加skuId
            saleAttrs.forEach(saleAttr -> saleAttr.setSkuId(skuId));
            //数据库表中的数据已经准备完成，批量插入保存
            this.skuAttrValueService.saveBatch(saleAttrs);


            //（3）第三步：保存营销信息
            // 1.1. 保存到sms_sku_bounds【保存顺序可以颠倒】
            // 1.2. 保存到sms_sku_full_reduction
            // 1.3. 保存到sms_sku_ladder
            // 以上，放在gmall-sms微服务进行编写，然后在gmall-pms进行远程调用
            // 前端传过来的数据，会在这个gmall-pms微服务的service进行保存，所以需要在这里进行远程调用
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            // 将skuVo对象中的属性，对拷到skuSaleVo属性中
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            // 远程调用gmall-sms微服务的方法，保存到数据库当中
            this.gmallSmsClient.saveSkuSale(skuSaleVo);
        });
    }

    //（1）第一步：保存spu信息
//    @Transactional(propagation = Propagation.REQUIRES_NEW)

    /**
     * 注解 @Transactional 只能应用到 public 方法才有效
     * 原因跟 AOP 相关
     * ----------------------------------------------
     * 使用 @Transactional 的三个条件
     * (1)有接口
     * (2)接口有抽象方法
     * 【所以此处，还需要在当前类所继承的接口SpuService上，
     * 加上当前方法的抽象方法】
     * (3)类继承接口后，实现接口的抽象方法必须是public的
     * 因为如果类的方法是私有的private，接口的抽象方法就不能访问到
     * 【 @Transactional 都一般都放置在类的方法上】
     */
    /*
    public void saveSpuDesc(SpuVO spu, Long spuId) {
        // 1.3. 保存到pms_spu_desc
        // 1.3.1. 图片信息保存在SpuVO的属性private List<String> spuImages;
        List<String> spuImages = spu.getSpuImages();
        // 1.3.2. 对获取到的图片集合进行判断，如果不为空，才执行保存操作
        if (!CollectionUtils.isEmpty(spuImages)) {
            // 1.3.3. pms_spu_desc对应的实体类是SpuDescEntity
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            //注意：spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
            spuDescEntity.setSpuId(spuId);
            // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            // 1.3.4. 将spuDescEntity保存到对应的数据库表中
            this.spuDescMapper.insert(spuDescEntity);
        }
    }
*/
    //（1）第一步：保存spu信息
    private void saveBaseAttr(SpuVO spu, Long spuId) {
        // 1.2. 保存到pms_spu_attr_value
        /*
        pms_spu_attr_value字段对应的数据，都封装在了SpuVO的baseAttrs属性中，也就是SpuAttrValueVo类
        attr_value字段，也在SpuAttrValueVo类作了转换赋值，所以该表所需的数据，都在SpuAttrValueVo类
        1.2.1. 先从SpuVO类中，获取baseAttrs属性
        */
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        /*
        1.2.2. 得到的是SpuAttrValueVo是一个数组
        使用BaseMapper的insert()，只能一条条数据保存，而使用IService的saveBatch()，可以批量保存
        但是表对应的类是SpuAttrValueEntity，而获取的是SpuAttrValueVo
        所以无法写成：
        this.spuAttrValueService.saveBatch(baseAttrs);
        需要将SpuAttrValueVo转换为SpuAttrValueEntity
        使用Java8新特性中的stream表达式，使用Demo见最下main的测试
         */
        /*
         * (A)初始化stream流，将List<SpuAttrValueVo> baseAttrs放入到stream流中
         * baseAttrs.stream()
         * (B)调用转换方法map()
         * 被转换的对象：SpuAttrValueVo
         * ① SpuAttrValueVo -> {}
         * --------------------------------
         * 转换成的对象：SpuAttrValueEntity
         * ② spuAttrValueVo -> {
         * 创建SpuAttrValueEntity对象
         * 将SpuAttrValueVo对象的属性，拷贝到SpuAttrValueEntity对象
         * 返回SpuAttrValueEntity对象，放入到流中
         * }
         * */
        /**
         * 判断List<SpuAttrValueVo> baseAttrs是否为空，不为空，才进行转换
         */
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntityList = baseAttrs.stream().map(
                    spuAttrValueVo -> {
                        SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                        BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                        spuAttrValueEntity.setSpuId(spuId);
                        return spuAttrValueEntity;
                    }).collect(Collectors.toList());
            //转换完得到SpuAttrValueEntity的集合后，就可以执行批量保存到数据库的操作了
            this.spuAttrValueService.saveBatch(spuAttrValueEntityList);
        }
    }

    //（1）第一步：保存spu信息
    private Long saveSpu(SpuVO spu) {
        // 1.1. 保存到pms_spu【保存顺序不能颠倒，因为要拿到spu_id，才能保存下面这两张表】
        // 1.1.2. 设置上架状态[0 - 下架，1 - 上架]
        spu.setPublishStatus(1);
        /*
        1.1.3. 设置时间
        为了保证新增时，更新时间和创建时间一致
        直接先保存CreateTime，再获取CreateTime保存到UpdateTime中
        如果两次都new Date()的话，保存的数据可能不一样
         */
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        // 1.1.1. 将封装前端收集的信息SpuVO spu，直接保存到数据库表pms_spu中
        this.save(spu);
        // 1.1.4. 获取spu_id，以便下面的保存操作
        return spu.getId();
    }
    /**
     * 测试stream表达式
     */
//    public static void main(String[] args) {
//        List<User> users = Arrays.asList(
//                new User(10, "李白", 18),
//                new User(11, "杜甫", 19),
//                new User(12, "王维", 20),
//                new User(13, "李贺", 21),
//                new User(14, "李可", 22),
//                new User(15, "李寅", 23)
//        );
//
//        /*
//         * Stream 常用方法一：过滤
//         * 需求：
//         * 从List<User> users中，筛选出年龄大于18岁的人
//         */
//        /*
//         * （1）第一步：先将集合List<User> users，放置到Stream流中
//         * 初始化Stream
//         * ① 对象名.stream()，集合常用
//         * ② Stream.of()，数组常用
//         * users.stream()，获取到stream流
//         * （2）第二步：执行中间操作，过滤使用的中间操作方法是filter()
//         * （3）第三步：在中间操作方法filter()中，执行过滤操作
//         * 被执行对象是 user
//         * 要过滤的条件是 user.getAge() >18
//         * filter()返回值依然是Stream流，
//         * 所以，筛选结果user -> user.getAge() >18会被放到流中
//         * （4）第四步：将筛选结果转为一个新的List集合
//         * collect(Collectors.toList())
//         * */
//        System.out.println(users.stream().filter(user -> user.getAge() > 18).collect(Collectors.toList()));
//
//
//        /*
//         * Stream 常用方法二：转换
//         * 需求：
//         * 从List<User> users中，获取用户名集合
//         * */
//        /*
//         * （2）第二步：执行中间操作，（集合）转换使用的中间操作方法是map()
//         * （3）第三步：在中间操作方法map()中，执行转换操作
//         * 被执行对象是 user
//         * 要转换为什么 user.getName()
//         * map()返回值就是user.getName()
//         * 所以，转换结果会被放到流中
//         * （4）第四步：将转换结果转为一个新的List集合
//         * collect(Collectors.toList())
//         * */
//        System.out.println(users.stream().map(user -> user.getName()).collect(Collectors.toList()));
//        /*
//         * 需求：
//         * 将List<User> users，转换为新的集合List<Person> persons
//         * (1)users.stream().map().collect(Collectors.toList())
//         * (2)然后在map()中进行转换
//         * */
//        List<Person> collect = users.stream().map(user -> {
//                    //（1）第一步，先创建一个Person对象
//                    Person person = new Person();
//                    //（3）第三步：将旧流User对象的属性，放入到新流Person对象中
//                    person.setPersonId(user.getId());
//                    person.setPersonName(user.getName());
//                    person.setPersonAge(user.getAge());
//                    //（2）第二步：将person对象放入到流中【也就是放入到新的集合】
//                    return person;
//                }
//        ).collect(Collectors.toList());
//        System.out.println("collect = " + collect);
//
//        //Stream 常用方法三：求总和
//    }
}

/**
 * 测试stream表达式
 */
//@Data
//@AllArgsConstructor //全参构造方法
//@NoArgsConstructor
//        //无参构造方法
//class User {
//    Integer id;
//    String name;
//    Integer age;
//}
//
//@Data
//class Person {
//    Integer personId;
//    String personName;
//    Integer personAge;
//}