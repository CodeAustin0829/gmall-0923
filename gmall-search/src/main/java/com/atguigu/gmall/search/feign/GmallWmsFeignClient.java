package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
@FeignClient("wms-service")
public interface GmallWmsFeignClient extends GmallWmsApi {
}
