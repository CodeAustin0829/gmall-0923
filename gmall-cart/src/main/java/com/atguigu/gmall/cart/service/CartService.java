package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/27
 */
@Service
public class CartService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    // 设置购物车外层Map的Key的前缀："项目名:功能名:+Key"
    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:info:"; // 比价的价格前缀

    /**
     * 新增购物车：
     * 1.将购物车信息，新增到数据库
     * 主要携带的两个参数：在地址栏?skuId=40&count=2
     * (1)skuId：商品id
     * (2)count：商品数量
     * 2.新增成功后，重定向到新增购物车成功页面，给出添加成功提示
     */
    public void addCart(Cart cart) {
        // 1.第一步：获取用户登录信息，代码抽取成方法，供多个方法进行调用
        String userId = this.getUserId();
        // 为userId添加上前缀
        String key = KEY_PREFIX + userId;

        /*
        2.第二步：通过外层Map的key，获取内层Map
        也就是获取redis中该用户的购物车
        通过boundHashOps方法可以获取
        返回的hashOps，就内层的Map结构
        也就是可以通过hashOps.hasKey(内层MapKey)
        判断内层MapKey是否存在于内层MapValue中
        也就是判断cart中是否包含skuId
        -----------------------------------------------------
        如果返回true，说明该用户的购物车信息已包含了该商品
        如果返回false，说明该用户的购物车信息未包含了该商品
        -----------------------------------------------------
        购物车结构是一个双层Map：Map<String,Map<String,String>>
         - 第一层Map，Key是用户id
         - 第二层Map，Key是购物车中商品id，值是购物车数据
         也就是Map<userId/userKey,Map<skuId,cart>>
         */
        BoundHashOperations<String, Object, Object> hashOps =
                this.stringRedisTemplate.boundHashOps(key);

        // 3.1.获取商品id，也就是内层Map中的Key
        String skuId = cart.getSkuId().toString();
        // 3.2.获取用户添加购物车的商品数量
        BigDecimal count = cart.getCount();
        // 3.第三步：判断该用户的购物车信息是否已经包含了该商品，如果包含，就对购物车中商品的数量进行更新；如果不包含，就将商品添加到购物车
        if (hashOps.hasKey(skuId)) {

            // 4.第四步：如果包含，就对购物车中商品的数量进行更新
            // 4.1.hashOps.get(key)，可以获取到对应的value值，也就是根据skuId，获取到购物车cart，对cart进行更新
            String cartJson = hashOps.get(skuId).toString();
            // 4.2.对cartJson反序列化成cart对象【此处是变量复用，形参上的cart是前端传过来的】
            cart = JSON.parseObject(cartJson, Cart.class);
            /*
            4.3.将用户添加购物的商品数量count，设置到cart的count属性
            cart.getCount()是原本就存在与购物车的商品数量
            然后使用add添加上用户新添加的商品数量【BigDecimal使用add来加】
            然后将总的商品数量更新到数据库中
            这样，购物车的商品数量就可以进行更新了
             */
            cart.setCount(cart.getCount().add(count));
            /*
            4.4 更新redis中的购物车商品数量
            获取的时候，是从redis中获取到Map
            保存的时候，使用Map的put就可以保存回redis了
            保存之前，需要将cart反序列化回字符串来保存
             */
            hashOps.put(skuId, JSON.toJSONString(cart));
            /*
            4.5.更新Mysql中的购物车商品数量
            参数一：要更新的对象，也就是购物车
            参数二：更新的条件，根据用户Id和商品Id来对购物车进行更新，也就是通过外层MapKey和内层MapKey，更新内层MapValue
             */
//            this.cartMapper.update(cart, new QueryWrapper<Cart>()
//                    .eq("user_id", cart.getUserId())
//                    .eq("sku_id", cart.getSkuId()));
            //进行异步调用
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
        } else {
            // 6.第六步：除了skuId、count，还需要设置userId、check、defaultImage、title、saleAttrs、price、store、sales
            // 6.1.设置userId，在第一步时已经获取过
            cart.setUserId(userId);

            // 6.2.设置title、price、defaultImage，存在于pms_sku表中，需要远程调用pms进行查询
            ResponseVo<SkuEntity> skuEntityResponseVo =
                    this.gmallPmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }

            // 6.3.设置saleAttrs，存在于pms_sku_attr_value表中，需要远程调用pms进行查询
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo =
                    this.gmallPmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            // 将List<SkuAttrValueEntity>反序列化成字符串
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            // 6.4.设置sales，存在于sms微服务中
            ResponseVo<List<ItemSaleVo>> itemSaleVoResponseVo =
                    this.gmallSmsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemSaleVoResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            // 6.5.设置store，存在于wms微服务中
            ResponseVo<List<WareSkuEntity>> listResponseVo =
                    this.gmallWmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                /*
                  库存赋值
                  当数据库表wms_ware_sku中的 stock(库存数) 与 stock_locked(锁定库存) 相减大于0时，
                  说明真的有没有被锁定的库存，这样才不会出现超卖现象
                  使用stream表达式的方法anyMatch()
                  【anyMatch(Predicate p) 传入一个断言型函数，对流中所有的元素进行判断
                  只要有一个满足条件就返回true，都不满足返回false】
                 */
                boolean store =
                        wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                                wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0);
                cart.setStore(store);
            }

            // 6.6.设置check，商品刚加入购物车时，默认为选中状态
            cart.setCheck(true);



            /*
            5.第五步：如果不包含，就将商品添加到购物车
            保存到redis中和mysql中
            ----------------------------------------------------------------
            注意点：
            由于前端传递过来的cart，只有两个属性字段：skuId商品id、count商品数量
            所以，再进行新增操作的时候
            需要拿到其余的属性的数据，保存到对应的数据库表的字段当中
            ----------------------------------------------------------------
            而更新就不用，因为更新是先从redis查询出数据，再进行更新的
             */
            hashOps.put(skuId, JSON.toJSONString(cart)); // 保存到redis
//            this.cartMapper.insert(cart); // 保存到mysql
            // 异步调用保存到mysql
            this.cartAsyncService.insertCart(cart);

            // 在保存cart到redis的同时，也缓存一份实时的价格到redis，以进行查询比价，以skuId为键，以实时价格为值
            this.stringRedisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
        }
    }

    /**
     * 新增购物车成功页面：
     * 本质上，就是根据用户的登录信息和skuId来查询数据库，显示所添加商品信息
     */
    public Cart queryCartBySkuId(Long skuId) {
        // 1.第一步：获取用户的登录信息
        String userId = this.getUserId();
        // 为userId添加上前缀
        String key = KEY_PREFIX + userId;

        // 2.第二步：通过外层Map的key，获取内层Map，也就是获取redis中该用户的购物车
        BoundHashOperations<String, Object, Object> hashOps =
                this.stringRedisTemplate.boundHashOps(key);

        // 3.第三步：判断内层MapValue（购物车cart），是否包换当前用户添加的购物车商品数量（内层MapKey（skuId））
        if (hashOps.hasKey(skuId.toString())) {
            // 4.第四步：如果包含，则获取购物车
            String cartJson = hashOps.get(skuId.toString()).toString();
            // 5.第五步：将cartJson反序列化成cart对象，返回给controller
            return JSON.parseObject(cartJson, Cart.class);
        }

        // 6.第六步：如果不包含，抛出异常
        throw new CartException("您的购物车中没有该商品记录！");
    }

    /**
     * 获取用户名的代码提取
     *
     * @return
     */
    private String getUserId() {
        /*
         1.第一步：
         通过spring拦截器，获取用户的登录信息
         getUserInfo()属于静态方法，可以直接通过类名来调用，里面有ThreadLocal封装好的用户信息
         */
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        /*
        1.2.从用户的登录信息中，获取用户Id
         */
        // String userId = userInfo.getUserId().toString();
        // 假设用户没有登录，userInfo.getUserId()获取的值就是为空，所以通过【.】的方式去调用toString()，就会报空指针异常
        /*
         1.3.判断用户Id是否存在，如果不存在，说明用户没有登录
         userKey是游客id
         userId是登录用户id
         也就是说，
         当userId不为空的时候，以userId作为外层Map的Key
         当userId为空的时候，以userKey作为外层Map的Key
         -----------------------------------------------------
         购物车结构是一个双层Map：Map<String,Map<String,String>>
         - 第一层Map，Key是用户id
         - 第二层Map，Key是购物车中商品id，值是购物车数据
         也就是Map<userId/userKey,Map<skuId,cart>>
         ------------------------------------------------------
         为了防止空指针异常，先判断userInfo.getUserId()是否为空
         得到的是Long类型，是long的包装类，可以判断是否为null
         long类型则不行
         */
        if (userInfo.getUserId() == null) {
            // 如果userId为空，以userKey作为外层Map的Key
            return userInfo.getUserKey();
        }
        // 如果当userId不为空的时候，以userId作为外层Map的Key，要转为字符串
        return userInfo.getUserId().toString();
    }

    /**
     * 查询购物车
     * 查询成功后，返回视图名称，并将数据模型共享到request域中
     * 然后在页面进行渲染
     */
    public List<Cart> queryCarts() {
        /**
         * 1.第一步：先根据userKey【游客的userId】，来查询未登录状态的购物车
         */
        // 1.1.从自定义拦截器LoginInterceptor中，获取userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        // 1.2.为userKey添加上前缀
        String unLoginKey = KEY_PREFIX + userKey;
        /*
         1.3.userKey作为Map的外层key【redis的保存结构：Map<userId/userKey,Map<skuId,cart>>】，
         调用boundHashOps方法，可以获取到内层Map的操作对象
         也就是说unLoginHashOps就相当于拿到了Map<skuId,cart>
         而我们的目的，就是想要拿到value
         可以通过key来get到对应的value
         在不知道key的情况下，可以使用map的方法values()，也能获取到value
         */
        BoundHashOperations<String, Object, Object> unLoginHashOps =
                this.stringRedisTemplate.boundHashOps(unLoginKey);
        // 1.4.获取内层Map的value，也就是购物车cart集合，是Json格式的
        List<Object> cartJsons = unLoginHashOps.values();
        /*
        1.5.将Json格式的cart集合，反序列化成正常的cart集合
        也要进行对cartJsons进行判空，因为在反序列化的过程
        cartJsons会进行【.】调用方法
        如果不判空，有可能发生空指针异常
         */
        List<Cart> unLoginCarts = null; // 变量提取
        if (!CollectionUtils.isEmpty(cartJsons)) {
            // 反序列化转换后，得到一个未登录的Cart集合，将List<Cart> unLoginCarts提取到if之外，方便第二步使用
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                //cartJsons是Object类型的，所以使用parseObject，同时也需要.toString()
                // 反序列化成cart对象
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 在将cart返回之前，先从redis中查询出实时的价格，添加到cart中，这样在第二步时，返回未登录的购物车中，就已经包含了实时价格
                String unLoginCurrentPrice =
                        this.stringRedisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(unLoginCurrentPrice));
                // 返回cart对象到新的list集合(新的流)
                return cart;
            }).collect(Collectors.toList());
        }

        /**
         * 2.第二步：获取userId【用户的userId】，进行判断，为空则表示没有登录，直接返回第一步得到unLoginCarts
         */
        // 2.1.从自定义拦截器LoginInterceptor中，获取userId
        Long userId = userInfo.getUserId();
        String loginKey = KEY_PREFIX + userId;
        // 2.2.判断userId是否为空，为空则返回unLoginCarts，不为空执行第三步
        if (userId == null) {
            // 2.3.userId为空，返回未登录的购物车unLoginCarts
            return unLoginCarts;
        }

        /**
         * 3.第三步：userId不为空，说明已经登录，则将未登录时所加的购物车，合并到登录后的购物车中
         */
        // 3.1.通过外层key，获取内层Map的操作对象，也就是获取登录状态购物车操作对象
        BoundHashOperations<String, Object, Object> loginHashOps =
                this.stringRedisTemplate.boundHashOps(loginKey);
        // 3.2.合并之前，先判断是否存在未登录的购物车，因为游客可能没添加，有才遍历未登录的购物车，合并到已登录的购物车中去
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            // 3.3.未登录的购物车不为空，则进行遍历
            unLoginCarts.forEach(cart -> {
                // 3.5.获取用户未登录的购物车中的商品id（skuId）
                String unLoginSkuId = cart.getSkuId().toString();
                // 3.6.获取用户未登录的购物车中的商品数量（count）
                BigDecimal unLoginCount = cart.getCount();
                /**
                 * 合并之前，还需要再判断一下，判断用户未登录时所添加的购物车，在登录的购物车中有没有
                 * 因为有可能用户登录时已经添加过商品到购物车中了，而未登录时添加到购物车的，有可能是同一商品
                 * 如果是同一商品，则作数量的累加，也就是合并就行
                 * 只有不是同一商品，就需要新增进去了
                 */
                // 3.4.判断用户登录的购物车，有没有该商品记录，有则累加合并，无则新增
                // loginHashOps.hasKey(unLoginSkuId)表示用户登录的购物车，有没有包含未登录的购物车商品
                if (loginHashOps.hasKey(unLoginSkuId)) {
                    // 3.7.包含，则累加数量
                    // 3.7.1.获取登录的购物车，既然是包含关系，那么skuId都是一样的，可以通过unLoginSkuId，获取到内层Map的value，也就是登录的购物车
                    String loginCartJson = loginHashOps.get(unLoginSkuId).toString();
                    // 3.7.2.将登录购物车Json，反序列化成登录购物车
                    cart = JSON.parseObject(loginCartJson, Cart.class);
                    // 3.7.3.对登录购物车的商品数量进行累加，也就是合并[登录购物车的商量数量cart.getCount()，加上，未登录购物车的商品数量unLoginCount]
                    cart.setCount(cart.getCount().add(unLoginCount));
                    // 3.7.4.保存到redis，并异步保存到mysql
                    loginHashOps.put(unLoginSkuId, JSON.toJSONString(cart));
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(), cart);
                } else {
                    // 3.8.不包含，则新增到redis，并异步新增到mysql
                    loginHashOps.put(unLoginSkuId, JSON.toJSONString(cart));
                    cart.setUserId(userId.toString()); // 将登录的用户id更新过来，因为未登录的id是userKey，登录的id是userId
                    this.cartAsyncService.insertCart(cart);
                }
            });

            /**
             * 4.第四步：未登录购物车存在，且已经将其里面的商品，合并到登录购物车中了
             * 那么，接下来，就需要将未登录购物车删除清空了，因为已经没有用了
             * 注意，要写在if (!CollectionUtils.isEmpty(unLoginCarts))判断内部
             * 因为只有未登录购物车存在，才能进行删除
             */
            // 删除redis，注意userKey是拼接后的userKey
            this.stringRedisTemplate.delete(unLoginKey);
            //删除mysql，userKey
            this.cartAsyncService.deleteUnLoginCarts(userKey);
        }

        /**
         * 5.第五步：查询登录状态的购物车，并返回给Controller
         */
        // 5.1.获取登录状态的购物车集合，是Json格式的
        List<Object> loginCartsJsons = loginHashOps.values();
        // 5.2.判断loginCartsJsons是否为空，不为空，则反序列化成登录状态的购物车集合
        if (!CollectionUtils.isEmpty(loginCartsJsons)) {
            return loginCartsJsons.stream().map(cartJson -> {
                // 反序列化成cart
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 在返回cart之前，先从redis中查询出实时的价格，然后添加到cart中，这样返回给controller时就已经包含了实时价格，这样前端就可以进行比较了
                String loginCurrentPrice =
                        this.stringRedisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(loginCurrentPrice));
                // 返回cart到新的list集合（新的流）中
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    /**
     * 更新购物车
     * - 请求方式：Post
     * - 请求路径：/updateNum
     * - 请求参数：json格式 {skuId: 30, count: 3}
     * - 响应数据：ResponseVo<Object> 或者 ResponseVo
     */
    public void updateNum(Cart cart) {
        /**
         *  1.第一步：从拦截器LoginInterceptor中，获取登录用户的userId
         *  已经提取在当前类的getUserId()方法中
         */
        // 1.1.获取用户id，当用户登录的时候，userId就是key，未登录的时候，userKey就是Key，两个都是id，统称userId
        String userId = this.getUserId();
        // 1.2.为userId添加前缀，方便从redis中查询
        String key = KEY_PREFIX + userId;

        /**
         * 2.第二步：通过Map的外层Key，也就是userId，获取内层Map的操作类
         * redis的保存结构：Map<userId/userKey,Map<skuId,cart>>
         */
        BoundHashOperations<String, Object, Object> hashOps =
                this.stringRedisTemplate.boundHashOps(key);

        /**
         * 3.第三步：
         * 判断用户的购物车中（就是上面所获取到的hashOps），
         * 是否包含所要更新的商品（在参数中已经传递过来）
         * 【返回的hashOps，就内层的Map结构
         * 也就是可以通过hashOps.hasKey(内层MapKey)
         * 判断内层MapKey是否存在于内层MapValue中
         * 也就是判断cart中是否包含skuId】
         * 因为只有购物车中含有此商品，才能进行更新
         * 如果不包含，则抛出异常，或者不用管
         */
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            try {
                // 3.1.获取用户在前端传递过来的要更新的商品数量
                BigDecimal count = cart.getCount();
                // 3.2.获取用户的购物车（是JsON格式的），在hashOps中可以通过skuId（key）来获取cart（value）
                String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
                // 3.3.将Json格式的购物车，转换为正常的购物车
                cart = JSON.parseObject(cartJson, Cart.class);
                // 3.4.将前端传递过来的商品数量，更新到购物车中
                cart.setCount(count);

                /**
                 * 4.第四步：更新到redis（别忘了还要反序列化回字符串），更新到mysql
                 */
//                this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
// mysql无法更新，【难道是skuId的原因？2021.3.29】
// 因为是异步请求，不属于同一个线程，所以无法通过TreadLocal获取到值，也就是在CartAsyncService中，无法通过cart取出里面的skuId【day17第二个视频有讲】
// CartAsyncService另开了一个子线程来执行，跟CartService不是同一个线程
                this.cartAsyncService.updateCartBySkuId(userId, cart.getSkuId(), cart);
                hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart)); //保存到redis
            } catch (Exception e) {
                e.printStackTrace();
                // 当购物车不包含所要更新的商品时，抛出异常
                throw new CartException("购物车中没有您所要更新的商品！");
            }
        }
    }

    /**
     * 删除购物车
     * - 请求方式：Post
     * - 请求路径：/deleteCart?skuId=30
     * - 请求参数：skuId
     * - 返回结果：无
     */
    public void deleteCart(Long skuId) {
        /**
         * 1.第一步：从拦截器中，获取userId
         */
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        /**
         * 2.第二步：根据userId，从redis中获取内层Map<skuId,cart>结构操作对象
         */
        BoundHashOperations<String, Object, Object> hashOps =
                this.stringRedisTemplate.boundHashOps(key);

        /**
         * 3.第三步：判断用户的购物车中是否包含所要删除的商品信息
         */
        if (hashOps.hasKey(skuId.toString())) {
            // 如果包含，则删除redis和mysql
            hashOps.delete(skuId.toString());
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId, skuId);
        }
    }

    /**
     * 测试异步调用：子线程一
     */
    @Async // 开启spring-task的异步功能
    public void executor1() {
        try {
            System.out.println("executor1开始执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1执行结束！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试异步调用：子线程二
     */
    @Async // 开启spring-task的异步功能
    public void executor2() {
        try {
            System.out.println("executor2开始执行！");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2执行结束！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试异步调用：子线程三之获取子任务的返回结果集
     * 返回值要使用Future
     * 泛型是真正要返回的类型
     * 返回的内容要放到AsyncResult.forValue中
     */
    @Async // 开启spring-task的异步功能
    public Future<String> executor3() {
        try {
            System.out.println("executor3开始执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor3执行结束！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("executor3");
    }

    /**
     * 测试异步调用：子线程四之获取子任务的返回结果集
     * 返回值要使用Future
     * 泛型是真正要返回的类型
     */
    @Async // 开启spring-task的异步功能
    public Future<String> executor4() {
        try {
            System.out.println("executor4开始执行！");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor4执行结束！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("executor4");
    }

    /**
     * 测试异步调用：子线程五之捕获子任务的异常信息
     * 返回值要使用ListenableFuture
     * 泛型是真正要返回的类型
     * 返回的内容要放到AsyncResult.forValue中
     */
    @Async // 开启spring-task的异步功能
    public ListenableFuture<String> executor5() {
        try {
            System.out.println("executor5开始执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor5执行结束！");
            return AsyncResult.forValue("executor5"); // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e); // 异常响应
        }
    }

    /**
     * 测试异步调用：子线程六之之捕获子任务的异常信息
     * 返回值要使用ListenableFuture
     * 泛型是真正要返回的类型
     */
    @Async // 开启spring-task的异步功能
    public ListenableFuture<String> executor6() {
        try {
            System.out.println("executor6开始执行！");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor6执行结束！");
            int i = 1 / 0; // 制造异常
            return AsyncResult.forValue("executor6"); // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);// 异常响应
        }
    }

    /**
     * 测试异步调用：子线程七之异常的统一处理
     */
    @Async // 开启spring-task的异步功能
    public String executor7() {
        try {
            System.out.println("executor7开始执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor7执行结束！");
            return "executor7"; // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 测试异步调用：子线程八之之异常的统一处理
     * 返回值要使用ListenableFuture
     * 泛型是真正要返回的类型
     */
    @Async // 开启spring-task的异步功能
    public String executor8() {
        try {
            System.out.println("executor8开始执行！");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor8执行结束！");
            int i = 1 / 0; // 制造异常
            return "executor8"; // 正常响应

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 订单确认页所需接口：获取登录用户勾选的购物车
     * ---------------------------------------------------------------
     * 需要传递参数userId来进行，虽然在拦截中将请求拦截下拉，从Cookie中获取到用户信息
     * 但是前提是浏览器发过来的请求，浏览器是携带coolie的
     * 而gmall-order微服务远程调用，是没有coolie的，拦截器无法从coolie中获取用户信息
     * 所以还需要传递userId过来
     */
    public List<Cart> queryCheckedCarts(Long userId) {
        // 1.拼接key
        String key = KEY_PREFIX + userId;

        // 2.根据key，从redis中获取内层Map的操作对象
        BoundHashOperations<String, Object, Object> hashOps =
                this.stringRedisTemplate.boundHashOps(key);

        // 3.获取内层Map中的Value，也就是购物车集合，是Json格式的
        List<Object> cartJsons = hashOps.values();

        // 4.判断cartJsons是否为空，如果不为空，则将cartJsons转换为cart集合，并使用filter()从cart中获取选中状态的集合，也就是check集合
        if (!CollectionUtils.isEmpty(cartJsons)) {
            return cartJsons.stream().map(cartJson ->
                    // 将将cartJsons转换为cart集合
                    JSON.parseObject(cartJson.toString(), Cart.class)
            ).filter(cart ->
                    // 从cart中获取选中状态的集合
                    cart.getCheck()).collect(Collectors.toList());
        }
        // 5.cartJsons为空，则直接返回null
        return null;
    }
}
