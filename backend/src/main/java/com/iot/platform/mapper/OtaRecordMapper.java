package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.OtaRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OtaRecordMapper extends BaseMapper<OtaRecord> {

    @Select("SELECT * FROM t_ota_record WHERE device_id = #{deviceId}")
    List<OtaRecord> selectByDeviceId(@Param("deviceId") String deviceId);
}
