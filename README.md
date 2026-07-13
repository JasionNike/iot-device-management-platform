# 智能物联设备管理平台

[![CI/CD](https://github.com/JasionNike/iot-device-management-platform/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/JasionNike/iot-device-management-platform/actions)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-8-orange)](https://adoptium.net/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

> 🏭 面向金融、能源、工业与消费电子行业的智能终端统一接入与运营平台。  
> 📡 支持 MQTT/HTTP 双协议设备接入，设备影子、OTA升级、告警中心、能力模型管理。  
> 🚀 **公网演示**: http://47.108.86.221:8080/ （admin/admin123）

---

## 技术栈

| 分类 | 技术 | 用途 |
|------|------|------|
| 基础框架 | Spring Boot 2.7.18 + Spring Cloud 2021.0.9 | 微服务基础 |
| 微服务治理 | Spring Cloud Alibaba 2021.0.6.0 (Nacos + Sentinel) | 服务发现/配置中心/熔断限流 |
| API网关 | Spring Cloud Gateway | 统一入口/认证/限流/路由 |
| 服务调用 | OpenFeign + LoadBalancer | 声明式HTTP调用+负载均衡 |
| 安全认证 | Spring Security + JWT + BCrypt | RBAC三角色(管理员/运维/只读) + 登录限流 |
| ORM | MyBatis-Plus | 分页/条件构造器/代码生成 |
| 数据库 | MySQL 8.0 + Flyway | 持久化 + 数据库版本迁移 |
| 缓存 | Redis + Redisson | 设备影子缓存 + 分布式锁(Watchdog自动续期) |
| 消息队列 | RabbitMQ (DLX + TTL延迟重试) | 设备事件削峰 + 通知指数退避补发 |
| 设备协议 | MQTT + HTTP | 设备双协议接入 |
| 定时任务 | XXL-Job | 分布式任务调度 |
| API文档 | SpringDoc OpenAPI 3.0 + Knife4j | 自动生成API文档 |
| 配置加密 | Jasypt | 配置文件密码加密 |
| 容器化 | Docker + Docker Compose | 一键部署 |
| CI/CD | GitHub Actions | 自动编译→Docker构建→部署到阿里云ECS |
| 前端 | Vue 2 + Element UI + ECharts | 7模块SPA单页应用 |
| 构建 | Maven (多模块聚合) | 依赖管理+打包 |

---

## 项目架构

```
                    ┌─────────────────────────┐
                    │    Nacos Server (:8848)   │  服务注册/配置中心
                    └─────────────────────────┘
                              ↑ 注册/发现
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼──────┐   ┌─────────▼────────┐   ┌───────▼──────────┐
│   Gateway    │   │    Backend       │   │  Notification    │
│    :8080     │──→│    :8081         │──→│  Stub :8082      │
│              │   │                  │   │                  │
│ 认证/限流    │   │ 设备管理/影子    │   │ 短信/邮件/钉钉   │
│ 路由转发     │   │ 告警/OTA/产品    │   │ 推送模拟         │
│ TraceId透传  │   │ Sentinel防护     │   │ 60%成功率        │
└──────────────┘   └──────────────────┘   └──────────────────┘
        │                    │
        │         ┌──────────┼──────────┐
        │         │          │          │
        │    ┌────▼───┐ ┌───▼────┐ ┌───▼──────┐
        │    │ MySQL  │ │ Redis  │ │ RabbitMQ │
        │    └────────┘ └────────┘ └──────────┘
        │
   ┌────▼─────┐
   │  前端SPA  │  Vue2 + Element UI + ECharts
   │  7模块    │  登录/工作台/设备/影子/告警/OTA/产品
   └──────────┘
```

**调用链路**: 用户请求 → Gateway(认证+限流) → Backend(业务处理) → Feign → Notification Stub(通知发送)

---

## 功能模块

### 📟 设备管理
- 自动注册(MQTT首次上线) + 手动注册(REST API)
- 分页列表(按产品/状态筛选 + 关键字搜索)
- 设备详情(遥测数据 + 历史趋势图)
- 设备SN唯一性校验 + 注册幂等(Redis SETNX)

### 🔄 设备影子
- reported/desired 双状态模型，端云协同
- Redis缓存(7天TTL) + MySQL持久化，三级读取降级
- Redisson分布式锁(设备ID粒度) + version乐观锁
- JSON影子文档，metadata记录每个属性时间戳

### 🚨 告警中心
- 6条预置告警规则(阈值/状态/事件类型)
- JSON条件表达式引擎(支持 `>` `>=` `<` `<=` `==` `!=`)
- 30分钟收敛窗口防告警风暴
- P2→P1超时自动升级(@Scheduled 5分钟)
- 告警等级分布饼图 + 通知补发状态饼图
- RabbitMQ异步消费设备事件→匹配规则→生成告警

### 📡 OTA升级
- 固件版本管理(上传/列表)
- 灰度发布(百分比分批推进 +10%/次)
- 失败自动重试(最多3次)
- 升级进度全程追踪(下载→安装→重启→成功/失败)

### 🗂️ 产品管理
- 产品CRUD(创建/编辑/删除，级联删除能力模型)
- 能力模型三要素: 属性(Property) / 事件(Event) / 指令(Command)
- 17个REST API端点，前端弹窗内独立增删改查
- 多厂商协议适配(策略模式 + 工厂模式): 传感器/采集器/终端/网关

### 📊 运行监控
- 设备统计大屏(总数/在线/离线/在线率/类型分布)
- 遥测数据图表(ECharts动态切换属性)
- 心跳检测(@Scheduled 60s扫描) + 离线自动标记

### 🔐 安全认证
- Spring Security + JWT(2小时过期 + HMAC-SHA256签名)
- BCrypt密码加密 + RBAC三角色(ADMIN/OPERATOR/VIEWER)
- 登录限流: 同IP 5分钟失败5次→锁定5分钟(Redis计数器)
- Gateway全局过滤器: TraceId生成 + Bearer Token透传
- @PreAuthorize方法级鉴权
- AOP审计日志: 所有POST/PUT/DELETE自动记录(t_audit_log)

### 🔔 通知闭环(商业级)
- 7通道枚举: SMS/EMAIL/APP_PUSH/PHONE/DINGTALK/WECOM/MANUAL
- Sentinel熔断 → FallbackFactory(DB+MQ双保障)
- RabbitMQ DLX + Per-Message TTL: 指数退避(1min→2min→4min→...→30min封顶)
- 通道降级: SMS失败3次→PHONE→失败→MANUAL人工
- XXL-Job监控: 10分钟巡检DLQ深度 + 每天清理7天前记录

---

## 快速启动

### 前置条件
- JDK 8+ / Maven 3.6+
- MySQL 8.0 / Redis 5.0+ / RabbitMQ 3.12+

### 本地开发

```bash
# 1. 初始化数据库
mysql -u root -p123456 < sql/schema.sql
mysql -u root -p123456 < sql/data.sql

# 2. 编译打包
mvn clean package -DskipTests

# 3. 启动 Backend (:8081)
cd backend && java -Dfile.encoding=UTF-8 -jar target/iot-platform-service-1.0.0-SNAPSHOT.jar

# 4. 启动 Gateway (:8080)
cd gateway && java -Dfile.encoding=UTF-8 -jar target/iot-gateway-1.0.0-SNAPSHOT.jar

# 5. 启动 Notification Stub (:8082)
cd mock-stubs && java -Dfile.encoding=UTF-8 -jar target/iot-notification-stub-1.0.0-SNAPSHOT.jar
```

或者一键启动: 双击 `start.bat` (Windows)

### Docker Compose

```bash
docker-compose up -d
```

### 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8080 | 前端首页(登录: admin/admin123) |
| http://localhost:8081/doc.html | API文档(Knife4j) |
| http://localhost:8081/actuator/health | Backend健康检查 |
| http://47.108.86.221:8080 | 公网演示环境 |

---

## API 速查

| 功能 | 方法 | 路径 |
|------|------|------|
| 登录 | POST | `/api/auth/login` |
| 设备列表 | GET | `/api/device/list` |
| 设备注册 | POST | `/api/device/register` |
| 设备影子 | GET | `/api/device/shadow/{deviceId}` |
| 遥测历史 | GET | `/api/device/telemetry/{deviceId}` |
| 统计大盘 | GET | `/api/device/dashboard` |
| 告警列表 | GET | `/api/alert/list` |
| 告警统计 | GET | `/api/alert/stats` |
| 告警处理 | POST | `/api/alert/handle` |
| 产品列表 | GET | `/api/product/list` |
| 产品属性 | GET | `/api/product/{productKey}/properties` |
| 产品事件 | GET | `/api/product/{productKey}/events` |
| 产品指令 | GET | `/api/product/{productKey}/commands` |
| 固件列表 | GET | `/api/ota/firmwares` |
| 固件上传 | POST | `/api/ota/firmware` |
| OTA任务 | POST | `/api/ota/task` |
| 灰度推进 | POST | `/api/ota/gray/{taskId}` |

完整文档: http://localhost:8081/doc.html

---

## 数据库设计

17张业务表:

| 表名 | 说明 |
|------|------|
| t_product | 产品型号 |
| t_product_property | 产品属性定义 |
| t_product_event | 产品事件定义 |
| t_product_command | 产品指令定义 |
| t_device | 设备信息(核心表) |
| t_device_shadow | 设备影子(JSON) |
| t_device_telemetry | 遥测历史数据 |
| t_device_event | 设备事件记录 |
| t_firmware | 固件版本 |
| t_ota_task | OTA升级任务 |
| t_ota_record | OTA升级记录 |
| t_alert_rule | 告警规则 |
| t_alert_record | 告警记录 |
| t_notification_retry | 通知补发记录 |
| t_audit_log | 操作审计日志 |
| t_user | 平台用户 |
| t_sys_dict | 系统字典 |

数据库版本管理: Flyway (V1初始化 + V2数据 + V3补充字段)

---

## 项目结构

```
智能物联设备管理平台/
├── pom.xml                     # 聚合父POM
├── Dockerfile                  # Backend Docker镜像
├── docker-compose.yml          # 本地Docker编排
├── start.bat                   # Windows一键启动脚本
├── .github/workflows/          # CI/CD Pipeline
├── sql/                        # 数据库脚本
├── frontend/                   # 前端SPA (Vue2 + Element UI)
├── backend/                    # IoT 核心服务 (:8081)
│   ├── controller/             # REST API (5个)
│   ├── service/impl/           # 业务逻辑 (6个)
│   ├── entity/                 # 数据实体 (14个)
│   ├── mapper/                 # MyBatis Mapper
│   ├── config/                 # 配置类 (6个)
│   ├── security/               # Spring Security + JWT
│   ├── protocol/               # 多厂商协议适配 (7个)
│   ├── feign/                  # OpenFeign客户端
│   ├── mq/                     # RabbitMQ生产者+消费者
│   ├── mqtt/                   # MQTT消息处理
│   ├── job/                    # XXL-Job定时任务
│   ├── simulator/              # 设备模拟器(200台)
│   ├── aspect/                 # AOP审计切面
│   ├── dto/                    # 数据传输对象
│   └── common/                 # 公共类(Result/ErrorCode)
├── gateway/                    # API网关 (:8080)
└── mock-stubs/                 # 通知服务挡板 (:8082)
```

---

## 商业级设计亮点

1. **三级影子读取**: Redis → MySQL → 空结构，逐级降级兜底
2. **设备级分布式锁**: Redisson RLock + Watchdog自动续期，锁粒度精确到设备ID
3. **通知双保障**: Sentinel熔断→FallbackFactory(DB审计+MQ延迟重试)
4. **指数退避重试**: RabbitMQ DLX + Per-Message TTL (1min→2min→4min→...→30min封顶)
5. **通道降级链**: SMS→PHONE→MANUAL，确保告警必达
6. **策略+工厂模式**: 多厂商协议适配，新增厂商零侵入
7. **AOP审计**: 所有写操作自动记录操作人/IP/耗时/结果
8. **RBAC三角色**: ADMIN/OPERATOR/VIEWER + @PreAuthorize方法级鉴权
9. **登录限流**: Redis计数器 + IP锁定
10. **设备注册幂等**: Redis SETNX + DB唯一约束双重保障

---

## CI/CD

推送代码到 `main`/`master` 分支自动触发:

```
GitHub Actions:
  Job1: mvn compile (编译)
  Job2: mvn package + docker build (构建镜像)
  Job3: SSH → 阿里云ECS → docker-compose up -d (自动部署)
```

---

## 联系方式

- **作者**: 王恒
- **GitHub**: [JasionNike/iot-device-management-platform](https://github.com/JasionNike/iot-device-management-platform)
- **公网演示**: http://47.108.86.221:8080/

---

*📅 2026.07 | 西安润和软件 | 面试作品*
