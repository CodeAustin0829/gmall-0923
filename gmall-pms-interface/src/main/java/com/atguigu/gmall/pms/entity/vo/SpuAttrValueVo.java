package com.atguigu.gmall.pms.entity.vo;


import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/10
 */
@Data
public class SpuAttrValueVo extends SkuAttrValueEntity {

    //接收封装集合类型valueSelected数据
//    private List<String> valueSelected;

    //重写valueSelected的set方法，因为参数接收时的本质是调用setter方法接收的
    public void setValueSelected(List<String> valueSelected) {
        // 如果接受的集合为空，则不设置
        if (CollectionUtils.isEmpty(valueSelected)) {
            return;
        }
        //如果接收的集合类型不为空，转为以逗号分隔的字符串类型，直接赋值给AttrValue，这样就不用每次再使用的时候都得转换了
        this.setAttrValue(StringUtils.join(valueSelected, ","));
    }
}
