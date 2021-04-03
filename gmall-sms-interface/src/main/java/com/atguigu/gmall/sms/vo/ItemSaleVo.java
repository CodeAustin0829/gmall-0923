package com.atguigu.gmall.sms.vo;

import lombok.Data;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/23
 */
@Data
public class ItemSaleVo {
    private String type; // 营销活动：积分 满减 打折
    private String desc; // 营销的描述信息
}
