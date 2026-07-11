package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.DeviceEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceEventMapper extends BaseMapper<DeviceEvent> {
}
