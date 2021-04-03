package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/24
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
