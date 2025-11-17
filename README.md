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

### Phase 6: 任务执行与 JDBC Sink（要点）

- 任务编排：CLI/REST 启动任务时，会自动生成/覆盖 Source 与 Sink 的 Connector 配置、部署到 Kafka Connect，并等待运行状态。
- Debezium 2.x 兼容：
  - MySQL Source 使用 `topic.prefix`（替代 legacy 的 `database.server.name`）。
  - 提供默认 `database.server.id`。
  - 使用 file-based schema history：`schema.history.internal=io.debezium.storage.file.history.FileSchemaHistory`，路径：`/kafka/connect/custom-connectors/schema-history/<task>.dat`。
  - 演示环境下默认 `snapshot.locking.mode=none`，避免 MySQL 用户缺少 RELOAD/LOCK TABLES 权限导致初始快照失败。
- 转换与路由（Sink）：
  - 启用 `key/value.converter.schemas.enable=true`（覆盖 worker 默认关闭 schemas）。
  - 注入 SMT：`ExtractNewRecordState`（解包 Debezium 信封）+ `RegexRouter`（将主题名路由为纯表名）。
  - `topics.regex` 仅匹配业务主题（排除 schema history 主题）。
- 容器内主机名：在 `docker-compose` 环境中，Source/Sink 连接数据库时请使用容器服务名（如 `mysql-source`、`postgres-target`），而非 `localhost`。

快速 E2E 验证（本地 docker 开发环境）：

```bash
# 1) 启动依赖环境
cd docker && docker-compose up -d

# 2) 创建任务（以 MySQL -> PostgreSQL 为例，使用容器主机名）
db-syncer task create \
  --name e2e-mysql-to-pg2 \
  --description "E2E MySQL to PostgreSQL (docker hostnames)" \
  --source-type MYSQL --source-host mysql-source --source-port 3306 \
  --source-db source_db --source-user dbuser --source-pass dbpass \
  --target-type POSTGRESQL --target-host postgres-target --target-port 5432 \
  --target-db target_db --target-user targetuser --target-pass targetpass

# 3) 启动任务（自动部署 Source/Sink）
db-syncer task start e2e-mysql-to-pg2

# 4) 写入源库数据（在主机）
docker exec -i db-syncer-mysql-source \
  mysql -udbuser -pdbpass -D source_db \
  -e "CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), email VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP); INSERT INTO customers(name,email) VALUES ('Alice','alice@example.com');"

# 5) 校验目标库
docker exec -i db-syncer-postgres-target \
  psql -U targetuser -d target_db \
  -c "\\dt" \
  -c "SELECT * FROM public.customers LIMIT 5;"
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

# 设置任务的类型映射规则文件（存入 source_properties）
db-syncer task props set <task-id-or-name> \
  --scope source \
  --key typeMapping.rulesPath \
  --value /path/to/rules.yaml
```

### 启用类型映射 SMT（可选）

生成带有类型映射的 Source Connector 配置模板（会注入 ApplyTypeMapping SMT）:

```bash
# 为任务生成 Source Connector 配置，并开启类型映射（以 MySQL 为例）
db-syncer config generate <task-id-or-name> --type source \
  --enable-type-mapping \
  --type-mapping-source-db mysql \
  --type-mapping-enable-time true \
  --type-mapping-enable-json true \
  -o source-config.json
```

对应的配置片段将包含:

```
transforms=applyTypeMapping
transforms.applyTypeMapping.type=com.dbsyncer.transformations.smt.ApplyTypeMapping
transforms.applyTypeMapping.source.db=mysql
transforms.applyTypeMapping.enable.time.mapping=true
transforms.applyTypeMapping.enable.json.mapping=true
transforms.applyTypeMapping.enable.decimal.mapping=true
transforms.applyTypeMapping.decimal.target=string
```

说明:
- `source.db`: 源数据库方言（mysql/oracle/postgresql），用于选择类型映射器。
- `enable.time.mapping`: 将 Debezium 时间类逻辑类型映射为 Kafka Connect 标准 Date/Time/Timestamp。
- `enable.json.mapping`: 将 Debezium Json 逻辑类型映射为普通字符串。
- `enable.decimal.mapping`: 将 Debezium VariableScaleDecimal 映射为目标类型。
- `decimal.target`: 目标类型，`string`（默认）或 `decimal`（Kafka Connect Decimal，注意可能引发频繁 Schema 变更）。

也可以在创建任务时通过元数据默认值控制（存储在 `source_properties`）:

```
typeMapping.enabled=true
typeMapping.sourceDb=mysql
typeMapping.enableTime=true
typeMapping.enableJson=true
```
当使用 `db-syncer config generate --type source` 且未显式传入 CLI 开关时，会读取上述默认值注入到配置模板。

### 快速生成目标表 DDL（预览）

当你只有源库的列定义时，可快速预览映射后的 PostgreSQL 表结构:

```bash
# 基于列定义快速生成 PostgreSQL DDL（也可用 --task 自动读 source-db 与 rulesPath）
db-syncer transform ddl --source-db mysql --schema public --table orders \
  "id:bigint unsigned not null" \
  "flag:tinyint(1)" \
  "amount:decimal(18,2)" \
  "payload:json" \
  "created_at:timestamp" \
  --pk id \
  --if-not-exists \
  --rules rules.yaml

# 从文件加载列定义（每行一个 COLUMN 规范，支持 # 注释）
db-syncer transform ddl --task mytask --table orders \
  --columns-file columns.txt --mode create

# 基于 Debezium/Connect JSON Schema 直接生成 DDL（自动选择 envelope.after 或顶层 struct）
db-syncer transform schema --task mytask --file ./value-schema.json \
  --table orders --mode create --format text --pk id

# 以 ALTER 形式输出（逐列 ADD COLUMN），便于演进已有表
db-syncer transform ddl --task mytask --table orders \
  "new_col:varchar(100)" --mode alter --if-not-exists --timestamp-tz without

# 仅输出列定义（便于嵌入到其它模板），可控制时间戳是否带时区
db-syncer transform ddl --task mytask --table orders \
  "ts_col:timestamp" --schema-only --timestamp-tz with

# 以 JSON 格式输出结构（便于自动化处理）
db-syncer transform ddl --task mytask --table orders \
  "id:bigint unsigned not null" "name:varchar(255)" \
  --format json

# JSON 输出示例
{
  "schema" : "public",
  "table" : "orders",
  "mode" : "create",
  "columns" : [ {
    "name" : "id",
    "type" : "numeric(20,0)",
    "notNull" : true
  }, {
    "name" : "name",
    "type" : "varchar(255)",
    "notNull" : false
  } ],
  "primaryKey" : [ "id" ]
}

# 类型映射预览（JSON 输出）
db-syncer transform map --task mytask --format json \
  "id:bigint unsigned" "name:varchar(255)" "payload:varbinary(1024)"

# 从文件加载列定义进行类型映射预览
db-syncer transform map --task mytask --columns-file columns.txt --format json

# 生成规则模板（YAML），用于自定义类型映射
db-syncer transform rules --source-db mysql -o rules.yaml
db-syncer task props set <task-id-or-name> --scope source --key typeMapping.rulesPath --value /path/to/rules.yaml

# 预览时将二进制列映射为 text（而非 bytea）
db-syncer transform ddl --task mytask --table files \
  "content:varbinary(4096)" --binary-as text
```

输出示例:

```
CREATE TABLE "public"."orders" (
    "id" numeric(20,0) NOT NULL,
    "flag" boolean,
    "amount" numeric(18,2),
    "payload" jsonb,
    "created_at" timestamp without time zone
);
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
    ├── MAPPING_REFERENCE.md  # 类型映射参考
    └── rules/                # 规则模板
```

## 开发路线图

查看 [GitHub Issues](https://github.com/your-username/db-syncer-debezium/issues) 了解详细的开发计划。

### 里程碑

- [x] Phase 1: 项目基础设施
- [x] Phase 2: 元数据管理系统
- [x] Phase 3: CLI 命令行工具
- [x] Phase 4: Debezium Source Connector 集成
- [ ] Phase 5: 数据转换层
- [ ] Phase 6: JDBC Sink 与任务执行（核心流转与 E2E 已完成，其余优化进行中）
- [ ] Phase 7: 监控与可观测性
- [ ] Phase 8: 测试与文档

## 贡献

欢迎贡献!请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细信息。

## 许可证

Apache License 2.0 - 详见 [LICENSE](LICENSE) 文件

## 支持

- 提交 Issue: [GitHub Issues](https://github.com/your-username/db-syncer-debezium/issues)
- 文档: [Wiki](https://github.com/your-username/db-syncer-debezium/wiki)
也可以仅做类型映射预览，并加载规则覆盖（或用 --task 自动读取默认 rules）:

```bash
# rules.yaml 示例:
# sourceDb: mysql
# overrides:
#   - name: tinyint
#     lengthEquals: 1
#     target: boolean
#   - name: bigint
#     unsigned: true
#     target: numeric(20,0)

db-syncer transform map --source-db mysql --rules rules.yaml \
  "id:bigint unsigned" "flag:tinyint(1)" "name:varchar(255)"
```
# 示例规则文件
- MySQL: docs/rules/mysql-defaults.yaml
- Oracle: docs/rules/oracle-defaults.yaml
