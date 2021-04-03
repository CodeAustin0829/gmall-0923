package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/24
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
