# DB-Syncer-Debezium

基于 Debezium CDC 的异构数据库迁移工具,支持 Oracle、MySQL、PostgreSQL 等数据库之间的实时数据同步与迁移。

## 特性

- 基于 Kafka Connect 的分布式架构,支持高可用和水平扩展
- 实时 CDC (Change Data Capture) 数据同步
- 支持异构数据库迁移:
  - MySQL → PostgreSQL
  - Oracle → PostgreSQL
  - 更多组合持续添加中
- 命令行管理界面,操作简单直观
- PostgreSQL 元数据存储,可靠的状态管理
- 完整的数据类型映射和转换
- 增量快照支持,高效处理大表
- 实时进度监控和任务管理
- 故障自动恢复机制

## 架构

```
┌─────────────────┐
│  CLI Tool       │  命令行管理工具
│  (Picocli)      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Metadata Service (PostgreSQL 12)       │
│  - 任务定义与配置                         │
│  - 进度跟踪                              │
│  - 映射规则                              │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│      Kafka Connect Cluster              │
│  ┌─────────────────────────────────┐   │
│  │  Debezium Source Connectors     │   │
│  │  - MySQL Connector              │   │
│  │  - Oracle Connector             │   │
│  └──────────┬──────────────────────┘   │
│             │                           │
│             ▼                           │
│  ┌─────────────────────────────────┐   │
│  │  Kafka Topics                   │   │
│  │  (Change Events)                │   │
│  └──────────┬──────────────────────┘   │
│             │                           │
│             ▼                           │
│  ┌─────────────────────────────────┐   │
│  │  Transformation Layer           │   │
│  │  - 类型映射                      │   │
│  │  - Schema 转换                  │   │
│  └──────────┬──────────────────────┘   │
│             │                           │
│             ▼                           │
│  ┌─────────────────────────────────┐   │
│  │  Debezium JDBC Sink Connector   │   │
│  └──────────┬──────────────────────┘   │
└─────────────┼───────────────────────────┘
              │
              ▼
     ┌────────────────┐
     │ Target Database│
     │  (PostgreSQL)  │
     └────────────────┘
```

## 技术栈

- **语言**: Java 17+
- **构建工具**: Maven 3.8+
- **CDC 引擎**: Debezium 2.6+
- **消息队列**: Apache Kafka 3.x
- **元数据存储**: PostgreSQL 12+
- **CLI 框架**: Picocli 4.x
- **监控**: JMX + Prometheus + Grafana
- **容器化**: Docker + Docker Compose

## 快速开始

### 前置要求

- JDK 17 或更高版本
- Maven 3.8+
- Docker & Docker Compose (用于本地开发环境)
- PostgreSQL 12+ (元数据存储)

### 本地开发环境

1. 克隆仓库
```bash
git clone https://github.com/your-username/db-syncer-debezium.git
cd db-syncer-debezium
```

2. 启动开发环境 (Kafka, Zookeeper, PostgreSQL)
```bash
cd docker
docker-compose up -d
```

3. 编译项目
```bash
mvn clean install
```

4. 运行 CLI 工具
```bash
java -jar cli/target/db-syncer-cli.jar --help
```

## 使用示例

### 创建迁移任务

```bash
# 创建 MySQL 到 PostgreSQL 的迁移任务
db-syncer task create \
  --name mysql-to-pg-migration \
  --source-type mysql \
  --source-host mysql.example.com \
  --source-port 3306 \
  --source-database mydb \
  --source-user root \
  --target-type postgresql \
  --target-host postgres.example.com \
  --target-port 5432 \
  --target-database targetdb \
  --target-user postgres
```

### 启动任务

```bash
db-syncer task start <task-id>
```

### 监控进度

```bash
db-syncer task status <task-id>
db-syncer monitor <task-id>  # 实时监控
```

### 管理任务

```bash
db-syncer task list                    # 列出所有任务
db-syncer task pause <task-id>         # 暂停任务
db-syncer task resume <task-id>        # 恢复任务
db-syncer task stop <task-id>          # 停止任务
db-syncer task delete <task-id>        # 删除任务
```

## 项目结构

```
db-syncer-debezium/
├── cli/                    # 命令行工具模块
├── metadata-service/       # 元数据管理服务
├── connectors/             # 自定义 Connector 扩展
├── transformations/        # 数据转换 SMT
├── common/                 # 公共模块
├── monitoring/             # 监控模块
├── docker/                 # Docker 编排文件
└── docs/                   # 文档
```

## 开发路线图

查看 [GitHub Issues](https://github.com/your-username/db-syncer-debezium/issues) 了解详细的开发计划。

### 里程碑

- [x] Phase 1: 项目基础设施
- [ ] Phase 2: 元数据管理系统
- [ ] Phase 3: CLI 命令行工具
- [ ] Phase 4: Debezium Source Connector 集成
- [ ] Phase 5: 数据转换层
- [ ] Phase 6: JDBC Sink 与任务执行
- [ ] Phase 7: 监控与可观测性
- [ ] Phase 8: 测试与文档

## 贡献

欢迎贡献!请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细信息。

## 许可证

Apache License 2.0 - 详见 [LICENSE](LICENSE) 文件

## 支持

- 提交 Issue: [GitHub Issues](https://github.com/your-username/db-syncer-debezium/issues)
- 文档: [Wiki](https://github.com/your-username/db-syncer-debezium/wiki)
