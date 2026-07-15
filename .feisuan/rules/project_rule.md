
# 智能物联设备管理平台开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息
- **工作目录**：`D:\work\简历\智能物联设备管理平台`，所有项目文件统一存放于该目录下，禁止随意移动目录结构
- **代码作者**：wangheng
- **构建工具**：Maven，采用多模块父POM统一管理依赖版本，子模块继承父POM无需单独声明依赖版本
- **项目架构**：微服务架构，包含核心服务、网关、设备模拟、模拟桩等多个独立模块
- **当前环境说明**：开发环境Nacos注册中心、配置中心未启用，采用服务直连模式，后续上线时再开启Nacos相关配置

## 二、技术栈要求
| 类型         | 版本/说明                                                                 |
|--------------|--------------------------------------------------------------------------|
| **JDK版本**  | 1.8.0_171                                                                |
| **Spring Boot** | 2.7.18                                                                  |
| **Spring Cloud** | 2021.0.9                                                               |
| **Spring Cloud Alibaba** | 2021.0.6.0                                                         |
| **构建工具** | Maven                                                                    |
| **核心依赖** | `spring-boot-starter-web`、`spring-boot-starter-validation`、`mybatis-plus-boot-starter`、`spring-boot-starter-data-redis`、`redisson-spring-boot-starter`、`hutool-all`、`lombok`、`spring-cloud-starter-alibaba-nacos-discovery`、`spring-cloud-starter-alibaba-nacos-config`、`spring-cloud-starter-openfeign`、`spring-cloud-starter-loadbalancer`、`spring-cloud-starter-alibaba-sentinel`、`spring-boot-starter-security`、`jjwt`、`knife4j-openapi3-spring-boot-starter`、`jasypt-spring-boot-starter`、`flyway-core`、`flyway-mysql`、`spring-boot-starter-amqp`、`spring-integration-mqtt`、`xxl-job-core` |

## 三、项目目录结构
```plaintext
智能物联设备管理平台
├── .github
│   └── workflows                # GitHub Actions 工作流配置
├── backend                      # 核心业务服务模块（设备管理、影子、告警、OTA等）
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── com
│       │   │       └── iot
│       │   │           └── platform
│       │   │               ├── aspect          # 切面层（日志、权限、限流、事务等横切逻辑）
│       │   │               ├── common          # 通用组件（统一响应、异常定义、工具类、常量定义）
│       │   │               ├── config          # 配置类（Nacos、Redis、MQTT、Security、Swagger等）
│       │   │               ├── controller      # HTTP接口层，处理请求与响应
│       │   │               ├── dto             # 数据传输对象（DTO/VO/Query统一存放）
│       │   │               ├── entity          # 数据库实体类，映射表结构
│       │   │               ├── feign           # OpenFeign远程服务调用客户端
│       │   │               ├── job             # XXL-Job分布式定时任务
│       │   │               ├── mapper          # MyBatis-Plus数据访问层接口
│       │   │               ├── mq              # RabbitMQ消息生产者/消费者
│       │   │               ├── mqtt            # MQTT通信处理（设备连接、消息收发、解析）
│       │   │               ├── protocol        # 物联网设备协议解析
│       │   │               ├── security        # 安全认证（JWT、权限校验、登录限流）
│       │   │               ├── service         # 业务逻辑层（接口定义）
│       │   │               │   └── impl        # 业务逻辑实现类
│       │   │               └── simulator       # 设备模拟器（模拟设备上报数据、MQTT通信）
│       │   └── resources
│       │       ├── db
│       │       │   └── migration    # Flyway数据库版本迁移脚本
│       │       ├── mapper           # MyBatis-Plus XML映射文件
│       │       ├── application.yml  # 核心服务配置文件
│       │       └── ...              # 其他配置文件
│       └── test
│           └── java
│               └── com
│                   └── iot
│                       └── platform
│                           └── service    # 单元测试/集成测试类
├── frontend                      # 前端项目目录
├── gateway                       # 网关模块（路由、鉴权、限流等）
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── com
│       │   │       └── iot
│       │   │           └── gateway
│       │   │               ├── config    # 网关配置
│       │   │               ├── controller # 网关相关接口
│       │   │               └── filter    # 网关过滤器（鉴权、日志、限流等）
│       │   └── resources
│       │       └── static
├── mock-device                    # 设备模拟模块（模拟物联设备行为）
│   └── src
│       └── main
│           ├── java
│           │   └── com
│           │       └── iot
│           │           └── mock
│           │               ├── config    # 模拟设备配置
│           │               ├── device    # 设备模拟逻辑
│           │               └── mqtt      # 模拟设备MQTT通信
│           └── resources
├── mock-stubs                     # 模拟桩模块（测试用依赖服务模拟）
│   └── src
│       └── main
│           ├── java
│           │   └── com
│           │       └── iot
│           │           └── mock
│           │               └── notification
│           │                   └── controller # 通知服务模拟接口
│           └── resources
└── sql                            # 数据库初始化脚本
```

## 四、分层架构规范
| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 1. 不得直接访问数据库，必须通过 Service 层调用<br>2. 入参加`@Valid`校验，统一返回统一响应结构<br>3. 接口加Swagger注解（@Tag、@Operation）生成API文档 |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 1. 必须通过 Mapper 层访问数据库<br>2. 禁止返回Entity实体，需转换为DTO/VO返回<br>3. 接口定义与实现分离，接口放`service`包，实现放`impl`子包<br>4. `@Transactional`仅加在实现类方法上，禁止加在接口上 |
| **Mapper**    | 数据库访问与持久化操作             | 1. 继承`BaseMapper<T>`，禁止手动编写简单CRUD<br>2. 复杂查询用XML映射文件，存放于`resources/mapper`下，与Mapper接口命名一致<br>3. 用`@EntityGraph`或关联查询解决N+1问题 |
| **Entity**     | 映射数据库表结构                   | 1. 不得直接返回给前端，需转换为DTO/VO<br>2. 加MyBatis-Plus注解：`@TableName`、`@TableId`、`@TableField`等<br>3. 统一存放于`entity`包 |

### 接口与实现分离规则
- 所有业务接口定义在`service`包下，实现类必须放在同包下的`impl`子包中
- 所有Feign远程调用接口定义在`feign`包下，降级实现类放在同包下的`impl`子包中

## 五、安全与性能规范
### 输入校验
- Spring Boot 2.7.x 校验注解位于`javax.validation.constraints.*`包下，使用`@NotBlank`、`@Size`、`@Pattern`等注解，Controller层入参加`@Valid`触发校验
- 禁止手动拼接SQL字符串，防止SQL注入，MyBatis-Plus参数绑定用`#{}`，禁止用`${}`拼接用户输入

### 安全规范
- 敏感配置（数据库密码、JWT密钥、MQTT密码等）必须用Jasypt加密，禁止明文写入配置文件，生产环境通过JVM参数传入加密密钥
- 用户密码存储必须用BCrypt加密，禁止明文存储
- 接口权限校验用Spring Security+JWT实现，禁止越权访问，敏感接口加权限注解
- 登录限流：同一IP 5分钟内最多5次失败尝试，超过则锁定账号
- Sentinel配置限流、降级、熔断规则，防止服务雪崩，规则持久化到Nacos

### 性能规范
- 分布式锁统一用Redisson实现，禁止自行实现分布式锁逻辑
- Redis缓存注意处理缓存穿透、击穿、雪崩问题，热点数据设置合理过期时间
- RabbitMQ消息手动确认，避免消息丢失，异步处理非核心业务（如告警、通知）
- MQTT通信默认QoS为1，保证消息至少到达一次，避免重复消费需做幂等
- 数据库连接池用Hikari，配置合理参数，避免连接泄漏，慢SQL及时优化

## 六、代码风格规范
### 命名规范
| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`、`DeviceMqttHandler` |
| 方法/变量  | lowerCamelCase       | `saveUser()`、`deviceStatus` |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`、`HEARTBEAT_INTERVAL` |
| 包名       | 全小写，名词复数      | `com.iot.platform.mqtt` |

### 类型命名规范
| 后缀 | 用途说明                     | 示例         | 存放位置 |
|------|------------------------------|--------------|----------|
| DTO  | 数据传输对象（接口入参/出参） | `DeviceDTO`  | dto包    |
| Entity | 数据库实体对象               | `DeviceEntity` | entity包 |
| BO   | 业务逻辑封装对象             | `DeviceBO`   | dto包    |
| VO   | 视图展示对象（前端返回）     | `DeviceVO`   | dto包    |
| Query| 查询参数封装对象             | `DeviceQuery`| dto包    |

### 注释规范
- 所有类、方法、字段必须添加**Javadoc注释**，使用中文编写
- 类注释说明类用途、作者（@author wangheng）、创建时间
- 方法注释说明功能、参数、返回值、抛出的异常
- 复杂业务逻辑、SQL语句必须添加行内注释说明

### 工具类使用规范
- 实体类用Lombok注解替代手动编写getter/setter/构造方法：`@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`
- 日志用`@Slf4j`注解，禁止使用`System.out.println`输出日志
- 通用工具用Hutool实现，禁止重复造轮子

## 七、扩展性与日志规范
### 接口优先原则
- 所有业务逻辑通过接口定义，具体实现放在`impl`子包中，便于扩展和单元测试
- 远程服务调用通过OpenFeign接口定义，配置Sentinel降级逻辑，禁止硬编码HTTP调用

### 日志规范
- 日志级别规范：
  - `error`：系统错误、异常堆栈，需及时告警
  - `warn`：业务异常、接口超时、限流降级等警告信息
  - `info`：核心业务链路日志（如设备上下线、消息收发、告警触发）
  - `debug`：开发调试信息，生产环境关闭
- 日志必须包含`traceId`链路标识，方便微服务链路排查
- 禁止打印敏感信息（密码、密钥、身份证号、手机号等）
- 日志输出符合结构化JSON格式，统一输出到`./logs/iot-platform.log`，按大小（100MB）和时间（30天）滚动保留

### API文档规范
- 所有对外接口必须加Swagger注解，Knife4j自动生成可交互API文档，地址为`http://服务IP:端口/doc.html`
- 接口变更必须同步更新文档注释

## 八、业务专项规范（物联网平台专属）
### 设备通信规范
- MQTT通信统一用`spring-integration-mqtt`实现，设备连接鉴权、消息解析、转发统一处理
- 设备心跳间隔30秒，连续3次心跳 missed 则标记设备离线，同步更新设备影子状态
- 设备上报数据必须做合法性校验，防止恶意数据注入

### 设备影子规范
- 设备影子数据TTL为7天，设备上线自动同步影子配置，状态变更实时更新影子
- 影子数据查询优先走Redis缓存，缓存未命中再查数据库

### 告警规范
- 告警收敛时间10分钟，同一设备同一告警类型10分钟内仅触发一次，避免告警风暴
- 告警消息通过RabbitMQ异步发送，不阻塞核心业务链路

### OTA升级规范
- OTA升级任务最多重试3次，升级状态实时同步到设备影子和数据库
- 升级包存储到对象存储，记录升级日志便于排查问题

### 定时任务规范
- 分布式定时任务用XXL-Job实现，任务配置必须加失败重试和告警机制
- 任务逻辑要幂等，避免重复执行导致数据异常

### 数据库迁移规范
- 所有数据库结构变更必须通过Flyway脚本实现，脚本存放于`resources/db/migration`目录
- 脚本命名格式：`V{版本号}__{描述}.sql`，例如`V3__add_device_ota_table.sql`
- 当前Flyway基线版本为2，后续脚本版本号从3开始递增
- 禁止手动修改线上数据库结构，所有变更必须走Flyway版本管理

## 九、依赖管理规范
- 所有依赖版本统一在父POM`dependencyManagement`中管理，子模块禁止单独声明依赖版本
- 禁止引入与业务无关的依赖，避免依赖冲突和包体积过大
- 依赖升级必须先在测试环境验证，再同步到所有子模块

## 十、测试规范
- 单元测试、集成测试统一放在`test`目录下，包结构与主代码保持一致
- 核心业务逻辑单元测试覆盖率不低于80%
- 集成测试用H2内存数据库，避免依赖真实数据库
- 测试用例要覆盖正常场景、异常场景、边界场景

## 十一、编码原则总结
| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
| **微服务兼容性** | 接口升级要兼容旧版本，配置统一走Nacos配置中心 |
