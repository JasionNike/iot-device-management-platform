package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.DeviceTelemetry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceTelemetryMapper extends BaseMapper<DeviceTelemetry> {

    @Select("SELECT * FROM t_device_telemetry WHERE device_id = #{deviceId} AND property_name = #{propertyName}")
    List<DeviceTelemetry> selectByDeviceId(@Param("deviceId") String deviceId,
                                           @Param("propertyName") String propertyName);
}
