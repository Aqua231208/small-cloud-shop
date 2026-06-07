# Small-Cloud-Shop API 测试文档

## 环境信息

| 服务 | 端口 | 说明 |
|------|------|------|
| shop-gateway | 8090 | API 网关（统一入口） |
| shop-auth | 8081 | 认证服务 |
| shop-product | 8082 | 商品服务 |
| shop-order | 8083 | 订单服务 |
| Nacos | 8848 | 注册中心/配置中心 |
| MySQL | 3306 | 数据库 small_cloud_shop |
| Redis | 6379 | 缓存 |

## 启动步骤

按以下顺序启动服务（确保 Nacos、MySQL、Redis 已运行）：

```bash
# 1. 认证服务
cd shop-auth && mvn spring-boot:run -DskipTests

# 2. 商品服务
cd shop-product && mvn spring-boot:run -DskipTests

# 3. 订单服务
cd shop-order && mvn spring-boot:run -DskipTests

# 4. API 网关
cd shop-gateway && mvn spring-boot:run -DskipTests
```

---

## 一、认证模块 (shop-auth)

### 1.1 用户注册

```bash
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "123456",
    "phone": "13800138000",
    "email": "zhangsan@qq.com"
  }'
```

**预期响应：**
```json
{"code":200,"message":"success","data":null}
```

### 1.2 重复注册

```bash
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "123456",
    "phone": "13800138000",
    "email": "zhangsan@qq.com"
  }'
```

**预期响应：**
```json
{"code":500,"message":"用户名已存在","data":null}
```

### 1.3 用户登录

```bash
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "123456"
  }'
```

**预期响应（记录返回的 token，后续测试用）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 2,
    "username": "zhangsan",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

### 1.4 密码错误

```bash
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "wrong_password"
  }'
```

**预期响应：**
```json
{"code":500,"message":"用户名或密码错误","data":null}
```

---

## 二、商品模块 (shop-product)

### 2.1 商品列表（无需 Token 会被拦截）

```bash
curl -s -w "\nHTTP_CODE: %{http_code}" http://localhost:8090/product/list
```

**预期响应：** `HTTP_CODE: 401`

### 2.2 商品列表（携带 Token）

> 将 `{TOKEN}` 替换为登录接口返回的 token

```bash
curl http://localhost:8090/product/list \
  -H "Authorization: Bearer {TOKEN}"
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {"id": 1, "name": "iPhone 15", "price": 6999.00, "stock": 100},
      {"id": 2, "name": "MacBook Pro", "price": 14999.00, "stock": 50},
      {"id": 3, "name": "AirPods Pro", "price": 1899.00, "stock": 200}
    ],
    "total": 0,
    "size": 10,
    "current": 1
  }
}
```

### 2.3 商品详情

```bash
curl http://localhost:8090/product/1 \
  -H "Authorization: Bearer {TOKEN}"
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "iPhone 15",
    "description": "Apple iPhone 15 256GB",
    "price": 6999.00,
    "stock": 100,
    "category": "手机"
  }
}
```

### 2.4 商品不存在

```bash
curl http://localhost:8090/product/999 \
  -H "Authorization: Bearer {TOKEN}"
```

**预期响应：**
```json
{"code":404,"message":"商品不存在","data":null}
```

---

## 三、订单模块 (shop-order)

### 3.1 创建订单

```bash
curl -X POST http://localhost:8090/order/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "userId": 2,
    "productId": 1,
    "productName": "iPhone 15",
    "price": 6999.00,
    "quantity": 2,
    "totalAmount": 13998.00,
    "status": "PENDING"
  }
}
```

### 3.2 查询订单

```bash
curl http://localhost:8090/order/2 \
  -H "Authorization: Bearer {TOKEN}"
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "userId": 2,
    "productId": 1,
    "productName": "iPhone 15",
    "price": 6999.00,
    "quantity": 2,
    "totalAmount": 13998.00,
    "status": "PENDING",
    "createTime": "2026-06-06T12:00:00"
  }
}
```

### 3.3 订单不存在

```bash
curl http://localhost:8090/order/999 \
  -H "Authorization: Bearer {TOKEN}"
```

**预期响应：**
```json
{"code":404,"message":"订单不存在","data":null}
```

---

## 四、网关鉴权测试

### 4.1 无 Token 访问需认证接口

```bash
curl -s -w "\nHTTP_CODE: %{http_code}" http://localhost:8090/product/list
curl -s -w "\nHTTP_CODE: %{http_code}" http://localhost:8090/order/1
```

**预期：** 均返回 `HTTP_CODE: 401`

### 4.2 无效 Token

```bash
curl -s -w "\nHTTP_CODE: %{http_code}" http://localhost:8090/product/list \
  -H "Authorization: Bearer invalid_token_xxx"
```

**预期：** `HTTP_CODE: 401`

### 4.3 白名单路径（无需 Token 可访问）

```bash
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```

**预期：** 正常返回，不会被拦截

---

## 五、快速测试脚本

将以下内容保存为 `test.sh`，替换 `TOKEN` 后执行：

```bash
#!/bin/bash
BASE="http://localhost:8090"
TOKEN="将登录返回的 token 粘贴到这里"

echo "========== 1. 注册 =========="
curl -s -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test2","password":"123456","phone":"13900001111","email":"t@qq.com"}'
echo ""

echo "========== 2. 登录 =========="
curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test2","password":"123456"}'
echo ""

echo "========== 3. 商品列表 =========="
curl -s -H "Authorization: Bearer $TOKEN" $BASE/product/list
echo ""

echo "========== 4. 商品详情 =========="
curl -s -H "Authorization: Bearer $TOKEN" $BASE/product/1
echo ""

echo "========== 5. 创建订单 =========="
curl -s -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  $BASE/order/create -d '{"productId":3,"quantity":1}'
echo ""

echo "========== 6. 无 Token 访问 =========="
curl -s -w "HTTP_CODE: %{http_code}\n" $BASE/product/1
echo ""

echo "========== 测试完成 =========="
```
