package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.Firmware;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FirmwareMapper extends BaseMapper<Firmware> {
}
