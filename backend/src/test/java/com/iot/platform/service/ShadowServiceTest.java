package com.iot.platform.service;

import com.iot.platform.IotApplication;
import com.iot.platform.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备影子服务单元测试 — 演示商业项目测试规范
 *
 * @author 王恒
 */
@SpringBootTest(classes = IotApplication.class)
public class ShadowServiceTest {

    @Autowired
    private ShadowService shadowService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 测试影子读取 — 不存在的设备返回空结构 */
    @Test
    public void testGetShadow_NotExist() {
        Object shadow = shadowService.getShadow("DEV-NOT-EXIST");
        assertNotNull(shadow, "影子不应为null，应返回空结构");
    }

    /** 测试Redis连接可用性 */
    @Test
    public void testRedisConnection() {
        String testKey = "test:shadow:connection";
        redisTemplate.opsForValue().set(testKey, "1");
        String val = redisTemplate.opsForValue().get(testKey);
        assertEquals("1", val, "Redis连接异常");
        redisTemplate.delete(testKey);
    }

    /** 测试设备表可访问 */
    @Test
    public void testDeviceTableAccess() {
        long count = deviceMapper.selectCount(null);
        assertTrue(count > 0, "设备表应至少有一条记录");
    }
}
