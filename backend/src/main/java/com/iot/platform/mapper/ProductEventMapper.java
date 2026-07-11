package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.ProductEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品事件Mapper
 *
 * @author 王恒
 */
@Mapper
public interface ProductEventMapper extends BaseMapper<ProductEvent> {
}
