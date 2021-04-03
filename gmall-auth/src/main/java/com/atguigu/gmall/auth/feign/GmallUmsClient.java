package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
