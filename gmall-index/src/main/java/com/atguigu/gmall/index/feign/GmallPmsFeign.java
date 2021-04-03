package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Description 远程调用pms微服务
 * @Author Austin
 * @Date 2021/3/20
 */
@FeignClient("pms-service")
public interface GmallPmsFeign extends GmallPmsApi {
}
