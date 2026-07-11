package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Select("SELECT * FROM t_device WHERE product_key = #{productKey}")
    List<Device> selectByProductKey(@Param("productKey") String productKey);

    @Select("SELECT * FROM t_device WHERE device_id = #{deviceId}")
    Device selectByDeviceId(@Param("deviceId") String deviceId);

    @Select("SELECT * FROM t_device WHERE heartbeat_miss_count >= #{missCount}")
    List<Device> selectOfflineDevices(@Param("missCount") int missCount);

    /** 批量更新MOCK设备的在线状态 */
    @Update("UPDATE t_device SET online_status = #{status}, heartbeat_miss_count = 0, last_online_time = NOW() WHERE device_sn LIKE CONCAT(#{prefix}, '%')")
    int updateOnlineStatusByPrefix(@Param("prefix") String prefix, @Param("status") int status);

    /** 随机选取N台在线MOCK设备 */
    @Select("SELECT * FROM t_device WHERE device_sn LIKE 'MOCK-%' AND online_status = 1 ORDER BY RAND() LIMIT #{limit}")
    List<Device> selectRandomOnlineMockDevices(@Param("limit") int limit);

    /** 选取所有在线MOCK设备 */
    @Select("SELECT * FROM t_device WHERE device_sn LIKE 'MOCK-%' AND online_status = 1")
    List<Device> selectAllOnlineMockDevices();
}
