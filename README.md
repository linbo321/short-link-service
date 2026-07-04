# 短链接生成服务

一个基于 Spring Boot 的短链接项目，支持长链接转短链、短链 302 跳转、访问次数统计、过期时间、最近生成记录、删除短链、接口限流、Redis 缓存、MySQL 持久化和 Docker Compose 一键启动。

项目入口页面启动后可以直接在浏览器访问：

```text
http://localhost/
```

## 功能说明

- 生成短链接：输入原始 URL，生成类似 `http://localhost/000001` 的短链接。
- 短链接跳转：访问短码路径后返回 HTTP 302，跳转到原始 URL。
- 最近生成记录：页面刷新后仍能从数据库加载最近生成的短链接。
- 访问统计：访问短链时先写入 Redis 计数器，再定时同步到 MySQL。
- 过期控制：生成时可以设置有效期，过期后访问会返回失效状态。
- 删除短链：支持删除指定短码，并清理相关缓存。
- 防穿透：启动时加载已有短码到 Guava BloomFilter，减少无效短码查询数据库。
- 接口限流：生成短链接口按 IP 做 Redis ZSET 滑动窗口限流。
- Docker 部署：通过 Nginx + Spring Boot + MySQL + Redis 组成完整服务。

## 技术栈

- Java 17
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Redis 6.2
- Guava BloomFilter
- Knife4j 接口文档
- Vue 3 + Element Plus CDN
- Docker Compose + Nginx

## 目录结构

```text
short-link-service/
├── src/main/java/com/shortlink
│   ├── common/              # 统一返回、异常、日志
│   ├── config/              # Redis、异步线程池、Web 配置、BloomFilter
│   ├── controller/          # API 和短链跳转入口
│   ├── dto/                 # 请求和响应对象
│   ├── entity/              # 数据库实体
│   ├── filter/              # 生成接口限流拦截器
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── service/             # 业务接口
│   ├── service/impl/        # 业务实现
│   ├── task/                # 定时任务
│   └── util/                # Base62、URL 校验工具
├── src/main/resources
│   ├── application.yml      # 应用配置
│   └── static/index.html    # 前端页面
├── sql/schema.sql           # MySQL 建表脚本
├── docker-compose.yml       # 一键启动配置
├── Dockerfile               # 正常构建镜像
├── Dockerfile.local         # Docker Hub 不稳定时的本地构建备用文件
├── nginx.conf               # Nginx 反向代理配置
├── start.ps1                # Windows 一键启动脚本
├── stop.ps1                 # Windows 一键停止脚本
├── reset.ps1                # Windows 清空数据并重启脚本
└── pom.xml                  # Maven 项目配置
```

## 环境要求

推荐使用一键脚本运行，脚本内部会自动调用 Docker Compose。

需要安装：

- Docker Desktop
- JDK 17
- Maven 3.8+

如果只使用 `start.ps1` 启动，理论上只需要 Docker Desktop；如果要本地开发、跑测试、手动打包，则需要 JDK 和 Maven。

## 推荐启动方式：一键脚本

### 1. 进入项目目录

```powershell
cd short-link-service
```

### 2. 确认 Docker Desktop 已启动

先打开 Docker Desktop，等左下角或状态栏显示 Docker Engine 已运行。

### 3. 一键启动

```powershell
.\start.ps1
```

脚本会自动完成这些事：

- 检查 Docker 是否可用。
- 构建并启动 MySQL、Redis、Spring Boot 后端和 Nginx。
- 等待 `http://localhost/api/health` 健康检查通过。
- 启动成功后自动打开 `http://localhost/`。

如果 PowerShell 阻止脚本执行，可以用下面命令临时绕过执行策略：

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

### 4. 启动成功后访问

页面地址：

```text
http://localhost/
```

健康检查：

```text
http://localhost/api/health
```

返回下面内容说明后端正常：

```json
{"code":200,"message":"success","data":"ok"}
```

### 5. 脚本会启动的容器

| 容器名 | 作用 | 端口 |
| --- | --- | --- |
| `short-link-nginx` | 对外入口，代理前端和后端接口 | `80` |
| `short-link-app` | Spring Boot 后端服务 | Compose 内部 `8080` |
| `short-link-mysql` | MySQL 数据库 | 宿主机 `3307` -> 容器 `3306` |
| `short-link-redis` | Redis 缓存和计数 | `6379` |

注意：MySQL 对宿主机暴露的是 `3307`，不是 `3306`。这是为了避免和你电脑本机已有的 MySQL 冲突。容器内部 Spring Boot 连接 MySQL 仍然使用 `mysql:3306`。

### 6. 查看容器状态

```powershell
docker compose ps
```

正常情况下应该看到：

- `short-link-mysql` 是 `healthy`
- `short-link-redis` 是 `healthy`
- `short-link-app` 是 `Up`
- `short-link-nginx` 是 `Up`

## 如何使用页面

1. 打开 `http://localhost/`。
2. 在输入框里填入长链接，例如：

```text
https://www.bilibili.com/
```

3. 设置有效期。如果不设置或设置为 0，表示永久有效。
4. 点击生成按钮。
5. 页面会显示短链接，例如：

```text
http://localhost/000006
```

6. 点击或复制这个短链接访问，会跳转到原始 URL。
7. 页面下方的最近生成记录来自 MySQL，刷新页面后仍然会显示最近生成的短链接。

如果某些外部网站在内置浏览器里显示“关闭了连接”，通常不是短链接服务没有返回跳转，而是目标网站拒绝了内置浏览器、跨环境访问或自动跳转。可以在系统浏览器里打开短链接再试。

## 常用命令

### 启动服务

```powershell
.\start.ps1
```

### 停止服务

```powershell
.\stop.ps1
```

这个命令只停止并删除容器，不会删除 MySQL 和 Redis 的数据卷。

### 清空数据并重新初始化

```powershell
.\reset.ps1
```

这个命令会删除 Docker 中的 MySQL 和 Redis 数据卷，所有已生成的短链接都会被清空。脚本会要求输入 `YES` 确认。

### 查看日志

查看后端日志：

```powershell
docker compose logs -f app
```

查看 Nginx 日志：

```powershell
docker compose logs -f nginx
```

查看 MySQL 日志：

```powershell
docker compose logs -f mysql
```

### 重启后端

```powershell
docker compose restart app
```

### 重新打包并刷新 Docker 后端

如果你改了 Java 代码，推荐这样执行：

```powershell
mvn test
.\start.ps1
```

脚本内部会重新执行 `docker compose up -d --build`，所以会自动重新构建后端镜像。

也可以直接使用 Docker Compose 原生命令：

```powershell
docker compose up -d --build
```

如果 Docker Hub 网络不稳定，项目里提供了 `Dockerfile.local` 备用构建方式：

```powershell
mvn package -DskipTests
docker build --pull=false -f Dockerfile.local -t short-link-service-app .
docker compose up -d --no-build app nginx
```

## 本地开发方式

如果不想用 Docker 跑后端，也可以本地启动 Spring Boot，但需要你自己准备 MySQL 和 Redis。

### 1. 准备 MySQL

创建数据库：

```sql
CREATE DATABASE short_link DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行：

```text
sql/schema.sql
```

默认连接配置在 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:short_link}
    username: ${MYSQL_USER:shortlink}
    password: ${MYSQL_PASSWORD:shortlink123}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

如果你的 MySQL 用户或端口不同，可以通过环境变量覆盖。

PowerShell 示例：

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3307"
$env:MYSQL_DATABASE="short_link"
$env:MYSQL_USER="shortlink"
$env:MYSQL_PASSWORD="shortlink123"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

### 2. 启动后端

```powershell
mvn spring-boot:run
```

本地直连 Spring Boot 时访问：

```text
http://localhost:8080/
```

使用 Docker Compose 时访问：

```text
http://localhost/
```

## API 使用说明

### 生成短链接

```http
POST /api/link/generate
Content-Type: application/json

{
  "url": "https://example.com/very/long/path",
  "expireHours": 24
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `url` | string | 是 | 原始长链接，必须是 `http://` 或 `https://` |
| `expireHours` | number | 否 | 过期小时数，小于等于 0 表示永久有效 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortCode": "000001",
    "shortUrl": "http://localhost/000001",
    "originalUrl": "https://example.com/very/long/path",
    "expireTime": "2026-07-05T12:00:00",
    "createTime": "2026-07-04T12:00:00"
  }
}
```

### 查询短链接信息

```http
GET /api/link/info/{code}
```

示例：

```text
http://localhost/api/link/info/000001
```

### 查询最近生成记录

```http
GET /api/link/recent
```

页面刷新后展示的最近生成记录就是从这个接口读取的。

### 访问短链接并跳转

```http
GET /{code}
```

示例：

```text
http://localhost/000001
```

正常情况下返回 HTTP 302，并跳转到原始 URL。

### 删除短链接

```http
DELETE /api/link/{code}
```

示例：

```powershell
Invoke-RestMethod -Method Delete http://localhost/api/link/000001
```

## 数据存在哪里

MySQL 和 Redis 都使用 Docker volume 持久化。

| 数据 | Docker volume | 容器内路径 | 说明 |
| --- | --- | --- | --- |
| MySQL 短链数据 | `short-link-service_mysql-data` | `/var/lib/mysql` | 保存短码、原始 URL、访问次数、过期时间 |
| Redis 缓存数据 | `short-link-service_redis-data` | `/data` | 保存 URL 缓存、访问计数、限流数据 |

查看 volume：

```powershell
docker volume ls
```

进入 MySQL 查看短链表：

```powershell
docker exec -it short-link-mysql mysql -uroot -proot123 short_link
```

进入后可以执行：

```sql
SELECT id, short_code, original_url, visit_count, create_time, expire_time
FROM short_link
ORDER BY id DESC
LIMIT 10;
```

注意：`docker compose down` 不会删除 volume，所以重新打开项目后数据还在。如果执行下面命令才会删除数据：

```powershell
docker compose down -v
```

## 项目核心流程

### 生成短链流程

```text
用户提交 URL
-> 校验 URL
-> 查询是否已有未过期短链
-> 写入 MySQL 获取自增 ID
-> ID 转 Base62 固定 6 位短码
-> 更新 MySQL 短码
-> 写入 Redis URL 缓存
-> 返回短链接
```

### 访问短链流程

```text
用户访问 /{code}
-> 校验短码格式
-> BloomFilter 判断短码是否可能存在
-> 优先查 Redis 缓存
-> 缓存未命中则查 MySQL
-> 检查是否过期
-> 回写 Redis 缓存
-> 异步增加 Redis 访问计数
-> 返回 302 跳转
```

### 访问次数同步

访问次数不会每次都直接写 MySQL，而是：

```text
短链被访问
-> Redis link:count:{code} 自增
-> 定时任务每 10 分钟扫描计数 key
-> 批量同步到 MySQL visit_count
-> 删除已同步的 Redis 计数 key
```

## 测试和打包

运行全部测试：

```powershell
mvn test
```

打包：

```powershell
mvn package -DskipTests
```

生成的 jar：

```text
target/short-link-service-1.0.0.jar
```

## 常见问题

### 1. Docker 提示无法连接 Docker API

错误类似：

```text
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

原因通常是 Docker Desktop 没启动，或者 Docker Engine 还没启动完成。

处理方式：

1. 打开 Docker Desktop。
2. 等待 Docker Engine 状态变为 Running。
3. 重新执行 `.\start.ps1`。

### 2. 端口 80 被占用

Nginx 需要占用宿主机 `80` 端口。如果端口被占用，可以修改 `docker-compose.yml`：

```yaml
nginx:
  ports:
    - "8081:80"
```

然后访问：

```text
http://localhost:8081/
```

### 3. 端口 3306 被本机 MySQL 占用

当前项目已经把 MySQL 映射成：

```yaml
ports:
  - "3307:3306"
```

所以不会占用你电脑上的 `3306`。如果要从宿主机连接 Docker MySQL，请连接 `localhost:3307`。

### 4. 页面打开了，但是最近生成记录为空

先检查接口：

```text
http://localhost/api/link/recent
```

如果接口返回空数组，说明数据库里还没有短链记录，先生成一条即可。

如果接口有数据但页面不显示，可能是浏览器缓存或前端资源加载问题，可以强制刷新页面。

### 5. 短链接打开后目标网站显示关闭连接

短链接服务本身只负责返回 302 跳转。某些网站可能拒绝内置浏览器、自动化环境、非标准 UA 或跨环境访问。

可以用命令检查短链服务是否正常返回 302：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost/000001 -MaximumRedirection 0
```

只要响应状态是 `302`，并且 `地址` 是原始 URL，说明短链接服务正常。

### 6. Docker Hub 拉镜像失败

如果网络导致基础镜像拉取失败，可以先确认 Docker Desktop 的代理或镜像源设置。配置好后重新运行：

```powershell
.\start.ps1
```

也可以使用项目已有本地基础镜像配合 `Dockerfile.local` 构建后端镜像：

```powershell
mvn package -DskipTests
docker build --pull=false -f Dockerfile.local -t short-link-service-app .
docker compose up -d --no-build app nginx
```

