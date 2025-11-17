# Docker 开发环境

本目录包含用于本地开发和测试的 Docker Compose 配置。

## 服务列表

- **Zookeeper**: Kafka 协调服务 (端口 2181)
- **Kafka**: 消息队列 (端口 9092)
- **Kafka Connect**: Debezium 连接器运行时 (端口 8083)
- **Metadata PostgreSQL**: 元数据存储 (端口 5432)
- **MySQL Source**: 测试用源数据库 (端口 3306)
- **PostgreSQL Target**: 测试用目标数据库 (端口 5433)
- **Prometheus**: 监控指标收集 (端口 9090)
- **Grafana**: 可视化仪表板 (端口 3000)

## 快速开始

### 启动所有服务

```bash
cd docker
docker-compose up -d
```

### 查看服务状态

```bash
docker-compose ps
```

### 查看日志

```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f kafka-connect
```

### 停止所有服务

```bash
docker-compose down
```

### 完全清理 (包括数据卷)

```bash
docker-compose down -v
```

## 服务访问

### Kafka Connect REST API

```bash
# 查看连接器状态
curl http://localhost:8083/

# 列出已安装的连接器
curl http://localhost:8083/connector-plugins
```

### 元数据数据库

```bash
psql -h localhost -p 5432 -U dbsyncer -d db_syncer_metadata
# 密码: dbsyncer_pass
```

### 测试数据库

**MySQL 源数据库:**
```bash
mysql -h 127.0.0.1 -P 3306 -u root -p
# 密码: mysql_root_pass
```

**PostgreSQL 目标数据库:**
```bash
psql -h localhost -p 5433 -U targetuser -d target_db
# 密码: targetpass
```

### Prometheus

访问: http://localhost:9090

### Grafana

访问: http://localhost:3000
- 用户名: admin
- 密码: admin

## 健康检查

所有数据库服务都配置了健康检查,可以通过以下命令查看:

```bash
docker-compose ps
```

healthy 状态表示服务正常运行。

## 故障排查

### Kafka Connect 无法启动

确保 Kafka 和 Zookeeper 已经完全启动:

```bash
docker-compose logs kafka zookeeper
```

### 连接器部署失败

检查 Kafka Connect 日志:

```bash
docker-compose logs -f kafka-connect
```

### 数据库连接问题

验证数据库容器是否健康:

```bash
docker-compose ps metadata-postgres mysql-source postgres-target
```

## 开发建议

1. **首次启动**: 等待所有服务健康检查通过 (约 1-2 分钟)
2. **数据持久化**: 数据卷确保容器重启后数据不丢失
3. **日志查看**: 使用 `docker-compose logs` 调试问题
4. **资源清理**: 定期运行 `docker system prune` 清理未使用的资源
