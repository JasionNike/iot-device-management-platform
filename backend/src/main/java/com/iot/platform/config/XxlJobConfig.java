package com.iot.platform.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 分布式调度配置
 *
 * 生产环境通过 XXL-Job Admin 管理所有定时任务：
 * - notificationMonitorJob：每10分钟检查 DLQ 积压
 * - notificationCleanupJob：每天凌晨清理7天前的 SENT 记录
 *
 * @author 王恒
 */
@Configuration
public class XxlJobConfig {

    private static final Logger log = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses:http://localhost:8080/xxl-job-admin}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname:iot-platform-executor}")
    private String appName;

    @Value("${xxl.job.executor.port:9997}")
    private int port;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    /**
     * 初始化 XXL-Job Spring 执行器
     * <p>
     * 创建并配置 XXL-Job 执行器 Bean，负责与 XXL-Job Admin 通信、接收调度任务并执行。
     * 
     * 配置参数说明：
     * - adminAddresses：XXL-Job Admin 地址，多个地址用逗号分隔
     * - appName：执行器名称，在 Admin 控制台可见，用于标识不同的执行器实例
     * - port：执行器服务端口，用于接收 Admin 的调度请求
     * - accessToken：请求令牌，用于与 Admin 之间的通信安全校验
     *
     * 本平台使用 XXL-Job 管理的定时任务：
     * - notificationMonitorJob：每10分钟检查通知发送失败队列(DLQ)积压情况
     * - notificationCleanupJob：每天凌晨清理7天前已发送成功的通知记录
     *
     * @return XXL-Job Spring 执行器实例
     */
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        // 记录初始化日志，便于排查问题
        log.info("XXL-Job 执行器初始化: admin={}, app={}, port={}", adminAddresses, appName, port);
        
        // 创建执行器实例
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        
        // 设置 Admin 地址（执行器需连接到 Admin 注册自己并接收任务）
        executor.setAdminAddresses(adminAddresses);
        
        // 设置执行器名称（用于在 Admin 控制台识别）
        executor.setAppname(appName);
        
        // 设置执行器端口（Admin 通过此端口调用执行器执行任务）
        executor.setPort(port);
        
        // 设置访问令牌（可选，用于安全校验）
        executor.setAccessToken(accessToken);
        
        // 返回配置完成的执行器实例
        return executor;
    }
}
