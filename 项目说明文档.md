# Small-Cloud-Shop 电商微服务系统

## 项目简介

基于 Spring Cloud Alibaba 搭建的中小型电商微服务系统，采用前后端分离架构，实现用户登录、商品浏览、下单等核心功能，支持高并发场景下的服务治理与熔断保护。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2.1 |
| 微服务框架 | Spring Cloud | 2023.0.0 |
| 微服务套件 | Spring Cloud Alibaba | 2023.0.1.0 |
| 注册中心 | Nacos | 2.x |
| 配置中心 | Nacos Config | 2.x |
| 服务网关 | Spring Cloud Gateway | 4.1.x |
| 熔断降级 | Sentinel | 1.8.6 |
| 远程调用 | OpenFeign + LoadBalancer | 4.1.x |
| 数据库 | MySQL | 8.0 |
| ORM 框架 | MyBatis Plus | 3.5.5 |
| 缓存 | Redis | 6.x |
| 安全认证 | JWT (jjwt) | 0.12.3 |
| JSON 处理 | FastJSON2 | 2.0.43 |
| 开发工具 | Lombok | - |
| JDK | Java 17 | - |

## 项目架构

```
                    ┌─────────────┐
                    │   客户端      │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  shop-gateway │  端口: 8090
                    │  (API 网关)   │  鉴权 + 路由 + 跨域 + 限流
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
     ┌────────▼───┐ ┌──────▼──────┐ ┌──▼─────────┐
     │  shop-auth  │ │shop-product │ │ shop-order  │
     │  端口: 8081 │ │ 端口: 8082  │ │ 端口: 8083  │
     │  认证服务    │ │  商品服务   │ │  订单服务    │
     └──────┬─────┘ └──────┬──────┘ └──────┬──────┘
            │              │               │
            │              │    OpenFeign  │
            │              └───────────────┤
            │                              │
     ┌──────▼─────┐ ┌──────────┐  ┌───────▼──────┐
     │   MySQL    │ │  Redis   │  │LoadBalancer  │
     │  数据库     │ │  缓存    │  │  负载均衡     │
     └────────────┘ └──────────┘  └──────────────┘
```

### 服务注册与发现

所有微服务启动时自动向 Nacos 注册，Gateway 通过服务名（lb://shop-xxx）进行负载均衡路由。

## 模块说明

### shop-common（公共模块）

提供各服务共享的基础能力：

| 类 | 说明 |
|----|------|
| `Result<T>` | 统一返回结构，包含 code / message / data |
| `BusinessException` | 自定义业务异常，支持错误码 |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 全局异常拦截 |
| `UserContext` | ThreadLocal 用户上下文（网关 → 下游透传） |
| `JwtUtil` | JWT 生成、校验、解析工具 |

### shop-gateway（API 网关）

统一流量入口，负责：

- **路由转发**：按路径前缀分发到不同微服务
- **统一鉴权**：`AuthGlobalFilter` 校验 JWT Token，白名单放行 `/auth/login`、`/auth/register`
- **跨域处理**：`CorsConfig` 允许跨域请求
- **限流保护**：`SentinelGatewayConfig` 网关层限流，超限返回 429

路由规则：

| 路径 | 目标服务 |
|------|---------|
| `/auth/**` | shop-auth |
| `/product/**` | shop-product |
| `/order/**` | shop-order |

### shop-auth（认证服务）

用户注册与登录：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/auth/register` | POST | 用户注册（MD5 加密密码） |
| `/auth/login` | POST | 用户登录（返回 JWT Token） |

### shop-product（商品服务）

商品浏览与查询：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/product/list` | GET | 分页查询商品列表 |
| `/product/{id}` | GET | 查询商品详情（Redis 缓存） |
| `/product/stock/{id}` | GET | 查询商品库存 |

性能优化：

- **Cache-Aside 模式**：先查 Redis，未命中再查 MySQL，写入缓存 30 分钟
- **Sentinel 限流**：`@SentinelResource` 保护核心接口，降级返回兜底数据

### shop-order（订单服务）

订单创建与查询：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/order/create` | POST | 创建订单（需 Token） |
| `/order/{id}` | GET | 查询订单详情 |

下单流程：

1. 从请求头 `X-User-Id` 获取用户 ID（由网关 AuthGlobalFilter 注入）
2. 通过 Feign 调用 `shop-product` 查询商品信息与库存
3. 校验库存后创建订单
4. Sentinel 保护，限流时返回提示

## 数据库设计

```sql
-- 用户表
CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(128),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 商品表
CREATE TABLE `product` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    image VARCHAR(512),
    category VARCHAR(64),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE `order_info` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(256),
    price DECIMAL(10,2),
    quantity INT DEFAULT 1,
    total_amount DECIMAL(10,2),
    status VARCHAR(32) DEFAULT 'PENDING',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 环境准备

| 组件 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 执行 `shop-auth/src/main/resources/sql/init.sql` 建库建表 |
| Redis | 6379 | 缓存热点商品数据 |
| Nacos | 8848 | 服务注册发现与配置管理 |
| JDK | - | Java 17+ |
| Maven | - | 3.6+ |

## 启动方式

### 方式一：IDEA 启动

依次运行各模块的 `Application` 主类：

1. `shop-auth` → `AuthApplication`
2. `shop-product` → `ProductApplication`
3. `shop-order` → `OrderApplication`
4. `shop-gateway` → `GatewayApplication`

### 方式二：Maven 命令行

```bash
# 1. 安装公共依赖
mvn install -DskipTests

# 2. 依次启动（每个命令新开一个终端窗口）
mvn -pl shop-auth spring-boot:run -DskipTests
mvn -pl shop-product spring-boot:run -DskipTests
mvn -pl shop-order spring-boot:run -DskipTests
mvn -pl shop-gateway spring-boot:run -DskipTests
```

## 技术亮点

### 1. Nacos 服务注册发现与配置管理

所有服务启动时向 Nacos 注册，Gateway 通过服务名实现负载均衡路由，消除硬编码地址，降低服务耦合度。

### 2. Gateway 统一鉴权与路由

- `AuthGlobalFilter` 全局过滤器校验 JWT，白名单路径放行
- 解析 Token 后将 userId 通过 `X-User-Id` 请求头透传给下游
- CorsConfig 解决跨域问题

### 3. Sentinel 限流与熔断

- 网关层：`SentinelGatewayConfig` 配置限流回调，返回 429
- 业务层：`@SentinelResource` 注解 + fallback 方法，防止服务雪崩

### 4. OpenFeign + LoadBalancer 服务间通信

- `ProductClient` 接口声明式调用，自动负载均衡
- 结合 Nacos 服务发现，实现高可用远程调用

### 5. Redis 缓存热点商品

- Cache-Aside 模式，30 分钟过期
- 命中缓存时跳过数据库查询，大幅提升 QPS
- 使用 fastjson2 序列化，避免 Jackson 类型擦除问题

### 6. 全局异常处理与统一返回

- `GlobalExceptionHandler` 拦截所有异常
- `Result<T>` 统一 `{code, message, data}` 格式
- 所有接口返回值规范化，便于前端统一处理
