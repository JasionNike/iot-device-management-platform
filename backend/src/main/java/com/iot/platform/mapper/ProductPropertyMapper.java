package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.ProductProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品属性Mapper
 *
 * @author 王恒
 */
@Mapper
public interface ProductPropertyMapper extends BaseMapper<ProductProperty> {
}
