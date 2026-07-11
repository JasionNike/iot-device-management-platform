package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.ProductCommand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品指令Mapper
 *
 * @author 王恒
 */
@Mapper
public interface ProductCommandMapper extends BaseMapper<ProductCommand> {
}
