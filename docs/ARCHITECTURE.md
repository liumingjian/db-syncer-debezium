# DB-Syncer-Debezium 架构设计

> 版本: 1.0.0-SNAPSHOT
> 最后更新: 2025-11-17

## 目录

- [概述](#概述)
- [系统架构](#系统架构)
- [核心组件](#核心组件)
- [数据流](#数据流)
- [技术选型](#技术选型)
- [部署架构](#部署架构)
- [安全设计](#安全设计)
- [性能优化](#性能优化)

## 概述

DB-Syncer-Debezium 是一个基于 Debezium CDC (Change Data Capture) 的异构数据库迁移工具,支持 Oracle、MySQL、PostgreSQL 等数据库之间的实时数据同步与迁移。

### 设计目标

- **可靠性**: 提供事务级别的数据一致性保证
- **可扩展性**: 支持水平扩展,处理大规模数据迁移
- **易用性**: 简单的命令行界面,最小化配置
- **可观测性**: 完整的监控、日志和告警体系
- **容错性**: 自动故障恢复和断点续传

### 核心特性

1. **基于 CDC 的实时数据捕获**: 利用 Debezium 捕获数据库变更日志
2. **异构数据库支持**: 支持不同数据库类型之间的迁移
3. **Schema 自动映射**: 自动处理数据类型和 Schema 转换
4. **增量快照**: 高效处理大表数据
5. **分布式架构**: 基于 Kafka Connect 的分布式部署

## 系统架构

### 整体架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                          用户层                                    │
│  ┌────────────────┐           ┌──────────────────┐               │
│  │   CLI Tool     │           │   Web UI (未来)  │               │
│  │   (Picocli)    │           │                  │               │
│  └────────┬───────┘           └────────┬─────────┘               │
└───────────┼──────────────────────────────┼────────────────────────┘
            │                              │
            │  REST API                    │  REST API
            ▼                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                       管理服务层                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │          Metadata Service (Spring Boot)                   │   │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────────────────┐  │   │
│  │  │Task Mgmt │  │Progress  │  │Connector Config Mgmt  │  │   │
│  │  │Service   │  │Tracking  │  │Service                │  │   │
│  │  └──────────┘  └──────────┘  └───────────────────────┘  │   │
│  └────────────────────┬─────────────────────────────────────┘   │
│                       │                                          │
│                       │ JPA/JDBC                                 │
│                       ▼                                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │          PostgreSQL Metadata Store                        │   │
│  │  - migration_tasks      - table_progress                  │   │
│  │  - connector_configs    - debezium_offsets                │   │
│  │  - schema_history       - task_logs                       │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────────┘
                           │ REST API
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    数据处理层 (Kafka Ecosystem)                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Kafka Connect Cluster                        │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │         Debezium Source Connectors                 │  │   │
│  │  │  ┌────────────┐  ┌────────────┐  ┌────────────┐   │  │   │
│  │  │  │   MySQL    │  │   Oracle   │  │ PostgreSQL │   │  │   │
│  │  │  │ Connector  │  │ Connector  │  │ Connector  │   │  │   │
│  │  │  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘   │  │   │
│  │  └─────────┼────────────────┼────────────────┼─────────┘  │   │
│  │            │                │                │            │   │
│  │            └────────────────┼────────────────┘            │   │
│  │                             ▼                             │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │              Kafka Topics                          │  │   │
│  │  │  - dbserver.database.table (per table)            │  │   │
│  │  │  - schema-changes                                 │  │   │
│  │  └──────────────────────┬─────────────────────────────┘  │   │
│  │                         │                                │   │
│  │                         ▼                                │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │        Transformation Layer (SMT)                  │  │   │
│  │  │  ┌──────────────┐  ┌────────────────────────────┐ │  │   │
│  │  │  │Type Mapping  │  │Schema Transformation       │ │  │   │
│  │  │  │SMT           │  │SMT                         │ │  │   │
│  │  │  └──────────────┘  └────────────────────────────┘ │  │   │
│  │  └──────────────────────┬─────────────────────────────┘  │   │
│  │                         │                                │   │
│  │                         ▼                                │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │         Debezium JDBC Sink Connector               │  │   │
│  │  └──────────────────────┬─────────────────────────────┘  │   │
│  └─────────────────────────┼──────────────────────────────────┘   │
└────────────────────────────┼──────────────────────────────────────┘
                             │ JDBC
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                        目标数据库                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │PostgreSQL  │  │   MySQL    │  │   Oracle   │                 │
│  └────────────┘  └────────────┘  └────────────┘                 │
└──────────────────────────────────────────────────────────────────┘

                             ▲
                             │ Metrics
                             │
┌──────────────────────────────────────────────────────────────────┐
│                      监控与可观测层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  Prometheus  │  │   Grafana    │  │  Alert Mgr   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└──────────────────────────────────────────────────────────────────┘
```

### 模块划分

项目采用 Maven 多模块结构:

```
db-syncer-debezium/
├── common/                 # 公共模块
│   ├── model/              # 通用数据模型
│   ├── exception/          # 异常定义
│   └── utils/              # 工具类
│
├── metadata-service/       # 元数据管理服务
│   ├── entity/             # JPA 实体
│   ├── repository/         # 数据访问层
│   ├── service/            # 业务逻辑层
│   ├── controller/         # REST API 控制器
│   └── config/             # 配置类
│
├── cli/                    # 命令行工具
│   ├── command/            # Picocli 命令
│   ├── client/             # API 客户端
│   └── formatter/          # 输出格式化
│
├── connectors/             # Connector 扩展
│   ├── offset/             # 自定义 Offset 存储
│   ├── history/            # 自定义 Schema History
│   └── config/             # Connector 配置生成器
│
├── transformations/        # 数据转换层
│   ├── smt/                # Single Message Transforms
│   ├── mapper/             # 类型映射器
│   └── schema/             # Schema 转换
│
└── monitoring/             # 监控模块
    ├── metrics/            # 指标收集
    └── health/             # 健康检查
```

## 核心组件

### 1. Metadata Service (元数据管理服务)

**职责**:
- 管理迁移任务的元数据
- 跟踪任务执行进度
- 存储 Connector 配置
- 持久化 Debezium Offset 和 Schema History

**关键类**:
- `TaskService`: 任务 CRUD 操作
- `ProgressTrackingService`: 进度跟踪
- `ConnectorConfigService`: Connector 配置管理
- `OffsetManagementService`: Offset 管理

**数据模型**:

```sql
-- 迁移任务表
CREATE TABLE migration_tasks (
    task_id UUID PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_config JSONB NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_config JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 表级进度跟踪
CREATE TABLE table_progress (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES migration_tasks(task_id),
    table_name VARCHAR(255) NOT NULL,
    total_rows BIGINT,
    processed_rows BIGINT,
    status VARCHAR(50),
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Connector 配置
CREATE TABLE connector_configs (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES migration_tasks(task_id),
    connector_name VARCHAR(255) NOT NULL,
    connector_type VARCHAR(50) NOT NULL,
    config JSONB NOT NULL,
    deployed BOOLEAN DEFAULT FALSE
);

-- Debezium Offset 存储
CREATE TABLE debezium_offsets (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES migration_tasks(task_id),
    partition_key VARCHAR(255) NOT NULL,
    offset_value JSONB NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Schema 变更历史
CREATE TABLE schema_history (
    id SERIAL PRIMARY KEY,
    task_id UUID REFERENCES migration_tasks(task_id),
    position VARCHAR(255),
    source_schema TEXT,
    target_schema TEXT,
    ddl_statement TEXT,
    timestamp TIMESTAMP NOT NULL
);
```

### 2. CLI Tool (命令行工具)

**职责**:
- 提供用户友好的命令行界面
- 调用 Metadata Service API
- 格式化输出结果
- 实时监控任务进度

**命令结构**:

```bash
db-syncer
├── task
│   ├── create      # 创建迁移任务
│   ├── list        # 列出所有任务
│   ├── show        # 查看任务详情
│   ├── start       # 启动任务
│   ├── stop        # 停止任务
│   ├── pause       # 暂停任务
│   ├── resume      # 恢复任务
│   ├── delete      # 删除任务
│   ├── status      # 查看任务状态
│   └── logs        # 查看任务日志
├── monitor         # 实时监控任务
└── config
    ├── show        # 显示配置
    ├── set         # 设置配置项
    └── validate    # 验证配置
```

**关键类**:
- `DbSyncerCommand`: 主命令入口
- `TaskCommand`: 任务管理命令组
- `MonitorCommand`: 监控命令
- `ConfigCommand`: 配置管理命令

### 3. Connectors (连接器扩展)

**职责**:
- 封装 Kafka Connect REST API 调用
- 生成 Debezium Source Connector 配置
- 生成 JDBC Sink Connector 配置
- 自定义 Offset 和 Schema History 存储

**Source Connector 配置生成器**:

```java
public interface SourceConnectorConfigGenerator {
    /**
     * 生成 Connector 配置
     */
    Map<String, String> generateConfig(MigrationTask task);

    /**
     * 验证源数据库连接和权限
     */
    ValidationResult validate(DatabaseConfig config);
}
```

**实现类**:
- `MySQLSourceConfigGenerator`: MySQL Source Connector 配置
- `OracleSourceConfigGenerator`: Oracle Source Connector 配置
- `PostgreSQLSourceConfigGenerator`: PostgreSQL Source Connector 配置

**自定义存储**:

```java
// 自定义 Offset 存储到 PostgreSQL
public class PostgreSQLOffsetBackingStore implements OffsetBackingStore {
    @Override
    public void configure(WorkerConfig config) { ... }

    @Override
    public void start() { ... }

    @Override
    public Future<Map<ByteBuffer, ByteBuffer>> get(
        Collection<ByteBuffer> keys) { ... }

    @Override
    public Future<Void> set(
        Map<ByteBuffer, ByteBuffer> values,
        Callback<Void> callback) { ... }
}

// 自定义 Schema History 存储到 PostgreSQL
public class PostgreSQLSchemaHistory extends AbstractSchemaHistory {
    @Override
    protected void storeRecord(HistoryRecord record) { ... }

    @Override
    protected void recoverRecords(Consumer<HistoryRecord> records) { ... }

    @Override
    public boolean exists() { ... }
}
```

### 4. Transformations (数据转换层)

**职责**:
- 实现类型映射规则
- 提供自定义 SMT (Single Message Transform)
- Schema 转换和 DDL 生成

**类型映射**:

```java
public interface TypeMapper {
    /**
     * 映射源数据库类型到目标数据库类型
     */
    TargetType mapType(SourceType sourceType);

    /**
     * 获取支持的源和目标数据库类型
     */
    DatabaseTypePair getSupportedTypes();
}
```

**MySQL → PostgreSQL 类型映射示例**:

| MySQL Type | PostgreSQL Type | 注释 |
|-----------|-----------------|------|
| INT | INTEGER | |
| BIGINT | BIGINT | |
| VARCHAR(n) | VARCHAR(n) | |
| TEXT | TEXT | |
| DATETIME | TIMESTAMP | |
| JSON | JSONB | 使用 JSONB 性能更好 |
| BLOB | BYTEA | |

**SMT 实现**:

```java
// 类型转换 SMT
public class TypeConversionTransform<R extends ConnectRecord<R>>
    implements Transformation<R> {

    @Override
    public R apply(R record) {
        Schema schema = record.valueSchema();
        Struct value = (Struct) record.value();

        Schema transformedSchema = transformSchema(schema);
        Struct transformedValue = transformValue(value, schema, transformedSchema);

        return record.newRecord(
            record.topic(),
            record.kafkaPartition(),
            record.keySchema(),
            record.key(),
            transformedSchema,
            transformedValue,
            record.timestamp()
        );
    }
}
```

### 5. Monitoring (监控模块)

**职责**:
- 收集系统和业务指标
- 暴露 Prometheus 端点
- 健康检查
- 告警规则定义

**指标类型**:

1. **任务级指标**:
   - 任务状态分布
   - 任务成功/失败率
   - 任务执行时间

2. **Connector 指标**:
   - Connector 状态
   - 吞吐量 (records/sec)
   - 延迟 (lag)
   - 错误计数

3. **系统指标**:
   - JVM 内存使用
   - GC 统计
   - 线程池状态
   - 数据库连接池

**Prometheus 指标示例**:

```java
// 任务计数器
Counter taskCounter = Counter.builder("dbsyncer.tasks.total")
    .description("Total number of migration tasks")
    .tag("status", "completed")
    .register(registry);

// 处理记录数
Counter recordsProcessed = Counter.builder("dbsyncer.records.processed")
    .description("Total number of records processed")
    .tag("task_id", taskId)
    .tag("table", tableName)
    .register(registry);

// Connector 延迟
Gauge connectorLag = Gauge.builder("dbsyncer.connector.lag",
    () -> calculateLag())
    .description("Connector lag in milliseconds")
    .tag("connector", connectorName)
    .register(registry);
```

## 数据流

### 1. 完整数据流程

```
1. 用户通过 CLI 创建迁移任务
   ↓
2. CLI 调用 Metadata Service REST API
   ↓
3. Metadata Service 保存任务元数据到 PostgreSQL
   ↓
4. 用户启动任务
   ↓
5. Metadata Service 生成 Source Connector 配置
   ↓
6. 通过 Kafka Connect REST API 部署 Source Connector
   ↓
7. Debezium Source Connector 开始捕获源数据库变更
   - 初始快照 (Snapshot Phase)
   - 持续 CDC (Streaming Phase)
   ↓
8. 变更事件写入 Kafka Topic
   ↓
9. SMT 处理消息进行类型转换和 Schema 映射
   ↓
10. JDBC Sink Connector 将数据写入目标数据库
   ↓
11. Progress Tracking Service 持续更新进度
   ↓
12. 用户通过 CLI 监控任务进度
```

### 2. Snapshot 阶段数据流

```
Source DB → Debezium Connector (Snapshot Reader)
            ↓
            读取全表数据 (SELECT * FROM table)
            ↓
            分批处理 (Chunking)
            ↓
            生成 INSERT 事件
            ↓
            → Kafka Topic
            ↓
            → SMT 转换
            ↓
            → JDBC Sink
            ↓
            → Target DB
```

### 3. Streaming 阶段数据流

```
Source DB Binlog/WAL → Debezium Connector
                       ↓
                       解析变更日志
                       ↓
                       生成 CDC 事件 (INSERT/UPDATE/DELETE)
                       ↓
                       → Kafka Topic
                       ↓
                       → SMT 转换
                       ↓
                       → JDBC Sink
                       ↓
                       → Target DB (UPSERT)
```

## 技术选型

### 核心技术栈

| 技术 | 版本 | 用途 | 选型理由 |
|-----|------|------|---------|
| Java | 17 LTS | 开发语言 | 长期支持,性能优秀 |
| Maven | 3.8+ | 构建工具 | 成熟的依赖管理 |
| Spring Boot | 3.2.5 | 应用框架 | 简化配置,快速开发 |
| Debezium | 2.6.2 | CDC 引擎 | 成熟的 CDC 解决方案 |
| Apache Kafka | 3.7.0 | 消息队列 | 高吞吐,持久化 |
| Kafka Connect | 3.7.0 | 连接器框架 | 分布式,可扩展 |
| PostgreSQL | 12+ | 元数据存储 | 功能强大,JSONB 支持 |
| Picocli | 4.7.5 | CLI 框架 | 注解驱动,易用 |
| Logback | 1.5.6 | 日志框架 | 高性能,灵活配置 |
| Prometheus | Latest | 监控 | 时序数据库,强大查询 |
| Grafana | Latest | 可视化 | 丰富的 Dashboard |

### 数据库驱动

| 数据库 | 驱动 | 版本 |
|-------|------|------|
| PostgreSQL | postgresql | 42.7.3 |
| MySQL | mysql-connector-j | 8.3.0 |
| Oracle | ojdbc11 | 23.3.0.23.09 |

## 部署架构

### 单机部署

适用于开发和小规模迁移:

```
┌─────────────────────────────────────┐
│         Single Host                 │
│  ┌─────────────────────────────┐   │
│  │   Metadata Service          │   │
│  │   (Spring Boot App)         │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │   PostgreSQL                │   │
│  │   (Metadata Store)          │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │   Kafka + Zookeeper         │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │   Kafka Connect             │   │
│  │   (Single Worker)           │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │   Prometheus + Grafana      │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 生产环境部署 (推荐)

```
┌─────────────────────────────────────────────────────────────┐
│                      Load Balancer                          │
└──────────────────┬──────────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐    ┌───────▼────────┐
│ Metadata Svc 1 │    │ Metadata Svc 2 │
└───────┬────────┘    └───────┬────────┘
        │                     │
        └──────────┬──────────┘
                   │
        ┌──────────▼──────────┐
        │  PostgreSQL Cluster │
        │  (Primary+Replica)  │
        └─────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Kafka Cluster (3 nodes)                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ Broker 1 │    │ Broker 2 │    │ Broker 3 │              │
│  └──────────┘    └──────────┘    └──────────┘              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Kafka Connect Cluster (3+ workers)             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ Worker 1 │    │ Worker 2 │    │ Worker 3 │              │
│  └──────────┘    └──────────┘    └──────────┘              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   Monitoring Stack                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Prometheus  │  │   Grafana    │  │ Alert Manager│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

## 安全设计

### 1. 认证与授权

- Metadata Service API 使用 OAuth 2.0 / JWT 认证
- 数据库连接密码加密存储
- Kafka Connect 使用 SASL/SSL

### 2. 数据安全

- 敏感配置信息使用 Spring Cloud Config 加密
- 传输层使用 TLS 加密
- 数据库连接使用 SSL

### 3. 审计日志

- 记录所有 API 调用
- 记录配置变更历史
- 记录任务状态变更

## 性能优化

### 1. Snapshot 优化

- **增量快照**: 使用 Debezium Incremental Snapshot 特性
- **分块大小调整**: 根据表大小动态调整 `snapshot.fetch.size`
- **并行读取**: 配置多个 snapshot reader 线程

### 2. Streaming 优化

- **批量处理**: 配置 JDBC Sink 的 `batch.size`
- **并行度**: 增加 Kafka Topic 的 partition 数量
- **压缩**: 启用 Kafka 消息压缩 (LZ4/Snappy)

### 3. 资源优化

- **内存配置**: 调整 Kafka Connect worker 堆内存
- **连接池**: 优化 HikariCP 连接池配置
- **GC 调优**: 使用 G1GC 或 ZGC

### 4. 监控指标

关键性能指标:

- **吞吐量**: records/sec
- **延迟**: end-to-end latency
- **资源使用**: CPU, Memory, Network I/O
- **错误率**: failed tasks / total tasks

## 扩展性设计

### 1. 水平扩展

- Metadata Service 可部署多实例
- Kafka Connect 支持动态增加 worker
- Kafka 支持增加 broker

### 2. 插件化架构

- 自定义 TypeMapper 实现
- 自定义 SMT 实现
- 自定义 Connector 配置生成器

### 3. 多租户支持 (未来)

- 任务隔离
- 资源配额管理
- 权限控制

## 参考资料

- [Debezium Documentation](https://debezium.io/documentation/)
- [Kafka Connect Documentation](https://kafka.apache.org/documentation/#connect)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Picocli Documentation](https://picocli.info/)
