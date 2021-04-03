package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/27
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
