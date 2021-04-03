package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/24
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
