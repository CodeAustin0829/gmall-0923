package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/27
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
