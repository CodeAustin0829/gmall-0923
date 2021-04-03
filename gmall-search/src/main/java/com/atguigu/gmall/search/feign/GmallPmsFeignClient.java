package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
@FeignClient("pms-service")
public interface GmallPmsFeignClient extends GmallPmsApi {
}
