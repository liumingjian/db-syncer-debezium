# GitHub Issues 开发计划

本文档定义了项目的完整开发任务,将以 GitHub Issues 的形式进行跟踪。

## 里程碑 (Milestones)

### Phase 1: 项目基础设施 (2周)
### Phase 2: 元数据管理系统 (2周)
### Phase 3: CLI 命令行工具 (2周)
### Phase 4: Debezium Source Connector 集成 (2周)
### Phase 5: 数据转换层 (2周)
### Phase 6: JDBC Sink 与任务执行 (2周)
### Phase 7: 监控与可观测性 (1周)
### Phase 8: 测试与文档 (3周)

---

## Phase 1: 项目基础设施

### Issue #1: 项目初始化与仓库设置
**标签**: `task`, `phase-1`
**描述**: 初始化 Git 仓库,设置项目结构,配置 Maven 多模块项目

**验收标准**:
- [x] Git 仓库初始化完成
- [x] Maven 多模块项目结构创建(common, metadata-service, cli, connectors, transformations, monitoring)
- [x] 根 pom.xml 和各模块 pom.xml 配置完成
- [x] .gitignore 配置完成

### Issue #2: Docker Compose 开发环境配置
**标签**: `task`, `phase-1`
**描述**: 配置完整的 Docker Compose 本地开发环境

**验收标准**:
- [x] Zookeeper 和 Kafka 容器配置
- [x] Kafka Connect 容器配置
- [x] PostgreSQL 元数据库容器配置
- [x] MySQL 和 PostgreSQL 测试数据库容器配置
- [x] Prometheus 和 Grafana 监控容器配置
- [x] 所有容器可正常启动并通过健康检查

### Issue #3: CI/CD 流水线配置
**标签**: `task`, `phase-1`
**描述**: 设置 GitHub Actions 自动化构建和测试流程

**验收标准**:
- [x] 创建 .github/workflows/build.yml
- [x] 配置自动编译和单元测试
- [x] 配置代码质量检查 (Checkstyle, SpotBugs)
- [x] 配置测试覆盖率报告
- [x] PR 合并前自动运行检查

### Issue #4: 项目文档编写
**标签**: `documentation`, `phase-1`
**描述**: 编写项目基础文档

**验收标准**:
- [x] README.md 完成
- [x] CONTRIBUTING.md 完成
- [x] LICENSE 文件添加
- [x] docs/ARCHITECTURE.md 架构文档
- [x] docs/DEVELOPMENT.md 开发指南

### Issue #5: 代码规范和格式化配置
**标签**: `task`, `phase-1`
**描述**: 配置统一的代码格式和检查工具

**验收标准**:
- [x] 配置 Checkstyle (Google Java Style)
- [x] 配置 SpotBugs
- [x] 配置 Maven Formatter Plugin
- [x] 添加 EditorConfig 文件
- [x] 文档说明如何使用

### Issue #6: 基础日志框架配置
**标签**: `task`, `phase-1`
**描述**: 配置 SLF4J + Logback 日志框架

**验收标准**:
- [x] 各模块添加 logback.xml 配置
- [x] 配置日志级别和输出格式
- [x] 配置文件滚动策略
- [x] 区分开发和生产环境配置

---

## Phase 2: 元数据管理系统

### Issue #7: PostgreSQL 元数据 Schema 设计
**标签**: `task`, `phase-2`, `database`
**描述**: 设计并实现元数据数据库 Schema

**验收标准**:
- [x] 设计 migration_tasks 表结构
- [x] 设计 table_progress 表结构
- [x] 设计 debezium_offsets 表结构
- [x] 设计 schema_history 表结构
- [x] 设计 connector_configs 表结构
- [ ] ER 图文档

### Issue #8: Flyway 数据库迁移配置
**标签**: `task`, `phase-2`, `database`
**描述**: 集成 Flyway 实现数据库版本管理

**验收标准**:
- [x] 添加 Flyway 依赖
- [x] 创建初始化 SQL 脚本 (V1__initial_schema.sql)
- [x] 配置 Flyway 自动执行
- [x] 测试迁移脚本执行

### Issue #9: JPA 实体类定义
**标签**: `task`, `phase-2`
**描述**: 定义所有元数据表对应的 JPA 实体类

**验收标准**:
- [x] MigrationTask 实体
- [x] TableProgress 实体
- [ ] DebeziumOffset 实体
- [ ] SchemaHistory 实体
- [x] ConnectorConfig 实体
- [x] 添加必要的关联关系和索引

### Issue #10: Repository 层实现
**标签**: `task`, `phase-2`
**描述**: 实现 Spring Data JPA Repository 接口

**验收标准**:
- [x] TaskRepository 接口
- [x] TableProgressRepository 接口
- [ ] DebeziumOffsetRepository 接口
- [x] 自定义查询方法定义
- [ ] Repository 单元测试

### Issue #11: Service 层实现
**标签**: `task`, `phase-2`
**描述**: 实现元数据管理的业务逻辑层

**验收标准**:
- [x] TaskService 实现(CRUD 操作)
- [x] ProgressTrackingService 实现
- [ ] OffsetManagementService 实现
- [x] 事务管理配置
- [x] Service 层单元测试

### Issue #12: REST API 接口实现
**标签**: `task`, `phase-2`
**描述**: 实现元数据管理的 REST API

**验收标准**:
- [x] TaskController (创建/查询/更新/删除任务)
- [x] ProgressController (查询进度)
- [x] 统一异常处理
- [ ] API 文档 (Swagger/OpenAPI)
- [ ] 集成测试

### Issue #13: 元数据服务配置管理
**标签**: `task`, `phase-2`
**描述**: 实现配置文件管理和环境配置

**验收标准**:
- [x] application.yml 基础配置
- [x] application-dev.yml 开发环境配置
- [x] application-prod.yml 生产环境配置
- [x] 数据库连接池配置 (HikariCP)
- [x] 敏感信息外部化

### Issue #14: 元数据服务集成测试
**标签**: `test`, `phase-2`
**描述**: 使用 Testcontainers 实现集成测试

**验收标准**:
- [x] Testcontainers PostgreSQL 配置
- [ ] Repository 集成测试
- [x] Service 层集成测试
- [ ] REST API 集成测试
- [ ] 测试覆盖率 > 80%

---

## Phase 3: CLI 命令行工具

### Issue #15: Picocli 框架集成
**标签**: `task`, `phase-3`
**描述**: 集成 Picocli CLI 框架

**验收标准**:
- [x] Picocli 依赖配置
- [x] 主命令类 DbSyncerCommand 创建
- [ ] 全局选项配置 (--verbose, --config)
- [x] 版本信息和帮助文档
- [x] Spring Boot 集成

### Issue #16: Task 管理命令实现
**标签**: `task`, `phase-3`
**描述**: 实现任务管理相关的 CLI 命令

**验收标准**:
- [x] `task create` 创建任务
- [x] `task list` 列出任务
- [x] `task show <task-id>` 查看任务详情
- [x] `task delete <task-id>` 删除任务
- [x] 参数验证和错误处理

### Issue #17: Task 执行控制命令实现
**标签**: `task`, `phase-3`
**描述**: 实现任务执行控制命令

**验收标准**:
- [x] `task start <task-id>` 启动任务
- [x] `task stop <task-id>` 停止任务
- [x] `task pause <task-id>` 暂停任务
- [x] `task resume <task-id>` 恢复任务
- [x] 状态验证逻辑

### Issue #18: 监控和日志命令实现
**标签**: `task`, `phase-3`
**描述**: 实现任务监控和日志查看命令

**验收标准**:
- [x] `task status <task-id>` 查看任务状态
- [x] `task logs <task-id>` 查看任务日志
- [x] `monitor <task-id>` 实时监控任务进度
- [x] 终端 UI 进度条实现

### Issue #19: 配置管理命令实现
**标签**: `task`, `phase-3`
**描述**: 实现配置管理命令

**验收标准**:
- [x] `config show` 显示当前配置
- [x] `config set <key> <value>` 设置配置项
- [ ] `config validate` 验证配置
- [x] 配置文件读写逻辑

### Issue #20: CLI 与元数据服务集成
**标签**: `task`, `phase-3`
**描述**: CLI 工具调用元数据服务 API

**验收标准**:
- [ ] HTTP 客户端配置 (RestTemplate/WebClient)
- [ ] API 调用封装
- [ ] 错误处理和重试逻辑
- [ ] 连接超时配置

### Issue #21: CLI 输出格式化
**标签**: `task`, `phase-3`
**描述**: 优化 CLI 输出格式和用户体验

**验收标准**:
- [x] 表格输出格式 (ASCII Table)
- [x] JSON 输出选项 (--output json)
- [ ] ANSI 颜色支持
- [ ] 进度条和 Spinner 动画
- [ ] 优雅降级 (非 TTY 环境)

---

## Phase 4: Debezium Source Connector 集成

### Issue #22: Kafka Connect REST API 客户端
**标签**: `task`, `phase-4`
**描述**: 实现 Kafka Connect REST API 客户端

**验收标准**:
- [x] 连接器部署 API
- [x] 连接器删除 API
- [x] 连接器状态查询 API
- [x] 连接器暂停/恢复 API
- [x] 错误处理

### Issue #23: MySQL Source Connector 配置生成
**标签**: `task`, `phase-4`, `mysql`
**描述**: 实现 MySQL Source Connector 配置生成器

**验收标准**:
- [x] 连接参数配置
- [x] Binlog 位置配置
- [x] 表白名单/黑名单配置
- [x] Snapshot 模式配置
- [x] 配置验证逻辑

### Issue #24: Oracle Source Connector 配置生成
**标签**: `task`, `phase-4`, `oracle`
**描述**: 实现 Oracle Source Connector 配置生成器

**验收标准**:
- [x] LogMiner 配置
- [x] SCN 位置配置
- [x] 表选择配置
- [x] Snapshot 配置
- [x] 权限验证

### Issue #25: PostgreSQL Source Connector 配置生成
**标签**: `task`, `phase-4`, `postgresql`
**描述**: 实现 PostgreSQL Source Connector 配置生成器

**验收标准**:
- [x] Logical Decoding 配置
- [x] Publication/Replication Slot 配置
- [x] 表选择配置
- [x] Snapshot 配置

### Issue #26: Connector 生命周期管理
**标签**: `task`, `phase-4`
**描述**: 实现 Connector 的完整生命周期管理

**验收标准**:
- [x] Connector 部署逻辑
- [x] Connector 删除逻辑
- [x] Connector 状态监控
- [x] Connector 错误处理和重启
- [x] Connector 配置更新

### Issue #27: Offset 存储集成
**标签**: `task`, `phase-4`
**描述**: 将 Debezium Offset 存储到 PostgreSQL

**验收标准**:
- [x] 自定义 OffsetBackingStore 实现
- [x] PostgreSQL 存储逻辑
- [x] Offset 读取/写入
- [x] 定期 Flush 机制
- [x] 并发控制

### Issue #28: Schema History 存储集成
**标签**: `task`, `phase-4`
**描述**: 将 Schema History 存储到 PostgreSQL

**验收标准**:
- [x] 自定义 SchemaHistory 实现
- [x] Schema 变更记录
- [x] Schema 查询逻辑
- [x] 版本管理

### Issue #29: Connector 集成测试
**标签**: `test`, `phase-4`
**描述**: Connector 部署和运行的集成测试

**验收标准**:
- [ ] Testcontainers Kafka 和 Connect 配置
- [ ] MySQL Connector 部署测试
- [ ] Oracle Connector 部署测试 (可选)
- [ ] CDC 数据捕获验证
- [ ] Offset 持久化验证

---

## Phase 5: 数据转换层

### Issue #30: 类型映射框架设计
**标签**: `task`, `phase-5`
**描述**: 设计可扩展的类型映射框架

**验收标准**:
- [x] TypeMapper 接口定义
- [x] TypeMappingRegistry 注册表
- [x] 类型转换器接口
- [x] 映射规则配置格式

### Issue #31: MySQL → PostgreSQL 类型映射
**标签**: `task`, `phase-5`, `mysql`
**描述**: 实现完整的 MySQL 到 PostgreSQL 类型映射

**验收标准**:
- [x] 数值类型映射 (INT, BIGINT, DECIMAL, etc.)
- [x] 字符串类型映射 (VARCHAR, TEXT, CHAR, etc.)
- [x] 日期时间类型映射 (DATETIME, TIMESTAMP, DATE, etc.)
- [x] 二进制类型映射 (BLOB, BINARY, etc.)
- [x] JSON 类型映射
- [x] 单元测试覆盖

### Issue #32: Oracle → PostgreSQL 类型映射
**标签**: `task`, `phase-5`, `oracle`
**描述**: 实现完整的 Oracle 到 PostgreSQL 类型映射

**验收标准**:
- [x] NUMBER 类型映射
- [x] VARCHAR2/CHAR 类型映射
- [x] DATE/TIMESTAMP 类型映射
- [x] CLOB/BLOB 类型映射
- [x] RAW 类型映射
- [x] 单元测试覆盖

### Issue #33: 自定义 SMT (Single Message Transform)
**标签**: `task`, `phase-5`
**描述**: 实现自定义 Kafka Connect SMT

**验收标准**:
- [ ] TypeConversionTransform 实现
- [ ] ColumnRenameTransform 实现
- [ ] ValueTransform 实现
- [ ] SMT 配置接口
- [ ] 单元测试

### Issue #34: Schema 转换逻辑
**标签**: `task`, `phase-5`
**描述**: 实现 Schema 定义的转换逻辑

**验收标准**:
- [x] Debezium Schema 解析
- [x] 目标 Schema 生成
- [x] DDL 语句生成
- [x] Schema 差异检测
- [x] 单元测试

### Issue #35: 数据转换集成测试
**标签**: `test`, `phase-5`
**描述**: 端到端的数据转换测试

**验收标准**:
- [ ] 各类型转换正确性验证
- [ ] 边界值测试
- [ ] NULL 值处理测试
- [ ] 大数据量转换性能测试

---

## Phase 6: JDBC Sink 与任务执行

### Issue #36: Debezium JDBC Sink Connector 集成
**标签**: `task`, `phase-6`
**描述**: 集成 Debezium JDBC Sink Connector

**验收标准**:
- [x] JDBC Sink Connector 依赖
- [x] PostgreSQL Sink 配置生成
- [x] Upsert 语义配置（record_key / delete.enabled / schema.evolution=basic）
- [x] 批处理配置（batch.size）
- [x] 转换与路由（ExtractNewRecordState + RegexRouter）

### Issue #37: 任务编排引擎
**标签**: `task`, `phase-6`
**描述**: 实现完整的任务编排和执行引擎

**验收标准**:
- [x] TaskOrchestrator 核心类（TaskExecutionService）
- [x] 任务状态机实现（STARTING/RUNNING/PAUSED/STOPPED）
- [x] Source Connector 部署
- [x] Sink Connector 部署
- [x] 任务协调逻辑（覆盖保存配置 → 部署 → 等待运行 → 状态回填）

### Issue #38: 任务启动逻辑
**标签**: `task`, `phase-6`
**描述**: 实现任务启动的完整流程

**验收标准**:
- [x] 参数验证
- [x] 前置检查（基础校验）
- [x] Schema 初始化（file-based history + schemas.enable 覆盖）
- [x] Connector 部署
- [x] 状态更新

### Issue #39: 任务停止和暂停逻辑
**标签**: `task`, `phase-6`
**描述**: 实现任务停止和暂停的逻辑

**验收标准**:
- [x] 优雅停止逻辑（删除 Connector + 状态 STOPPED）
- [x] Connector 删除/暂停/恢复
- [x] 状态清理与恢复能力
- [ ] Offset 保存（依赖 Connect Offset 存储，后续完善）

### Issue #40: 进度跟踪实现
**标签**: `task`, `phase-6`
**描述**: 实现实时进度跟踪功能

**验收标准**:
- [ ] Connector 指标采集
- [x] 表级进度计算
- [x] 记录数统计
- [x] ETA 估算
- [x] 数据库持久化

### Issue #41: 错误处理和重试机制
**标签**: `task`, `phase-6`
**描述**: 实现健壮的错误处理和重试逻辑

**验收标准**:
- [x] 异常分类(可重试/不可重试)
- [x] 指数退避重试
- [ ] 失败记录持久化
- [ ] Dead Letter Queue
- [x] 告警通知接口

### Issue #42: 增量快照支持
**标签**: `task`, `phase-6`
**描述**: 支持 Debezium 增量快照功能

**验收标准**:
- [x] 增量快照配置
- [x] 分块大小配置
- [ ] 并行度配置
- [x] 进度跟踪
- [ ] 断点续传

### Issue #43: 多表并行迁移
**标签**: `task`, `phase-6`
**描述**: 支持同一任务内多表并行迁移

**验收标准**:
- [ ] 表分组逻辑
- [x] 并行度控制
- [ ] 资源限制
- [x] 进度汇总
- [ ] 部分失败处理

### Issue #44: 任务执行集成测试
**标签**: `test`, `phase-6`
**描述**: 端到端的任务执行测试

**验收标准**:
- [ ] 完整迁移流程测试
- [ ] 数据一致性验证
- [ ] 故障恢复测试
- [ ] 性能基准测试

---

## Phase 7: 监控与可观测性

### Issue #45: JMX 指标暴露
**标签**: `task`, `phase-7`
**描述**: 暴露 JMX 监控指标

**验收标准**:
- [ ] Connector 指标采集
- [ ] 任务执行指标
- [ ] 资源使用指标
- [ ] JMX MBean 注册
- [ ] 指标文档

### Issue #46: Prometheus 集成
**标签**: `task`, `phase-7`
**描述**: 集成 Prometheus 监控

**验收标准**:
- [x] Micrometer 依赖
- [x] 自定义指标定义
- [x] Prometheus Endpoint 暴露
- [ ] Grafana Dashboard 定义

### Issue #47: 健康检查接口
**标签**: `task`, `phase-7`
**描述**: 实现健康检查和就绪检查

**验收标准**:
- [x] /health endpoint
- [x] /ready endpoint
- [x] 数据库连接检查
- [x] Kafka 连接检查
- [ ] Connector 状态检查

### Issue #48: 任务监控 Dashboard
**标签**: `task`, `phase-7`
**描述**: 实现 CLI 任务监控终端 UI

**验收标准**:
- [x] 实时进度条
- [x] 多表进度显示
- [x] 速率统计
- [x] ETA 显示
- [ ] 终端 UI 库选择 (Lanterna/Jexer)

### Issue #49: 日志增强
**标签**: `task`, `phase-7`
**描述**: 增强日志记录和查询功能

**验收标准**:
- [x] 结构化日志 (JSON)
- [x] 任务关联日志
- [ ] 日志级别动态调整
- [x] 日志查询 API
- [ ] 日志聚合配置 (ELK/Loki)

### Issue #50: 告警机制
**标签**: `task`, `phase-7`
**描述**: 实现告警通知机制

**验收标准**:
- [x] 告警规则定义
- [ ] 邮件通知实现
- [ ] Webhook 通知
- [x] 告警历史记录
- [ ] 告警配置界面

---

## Phase 8: 测试与文档

### Issue #51: 单元测试覆盖率提升
**标签**: `test`, `phase-8`
**描述**: 提升整体单元测试覆盖率到 80%+

**验收标准**:
- [ ] 所有模块覆盖率 > 80%
- [ ] 核心逻辑覆盖率 > 90%
- [ ] 测试报告生成
- [ ] Jacoco 集成

### Issue #52: 集成测试套件
**标签**: `test`, `phase-8`
**描述**: 完善集成测试套件

**验收标准**:
- [ ] Testcontainers 完整配置
- [ ] MySQL → PostgreSQL 端到端测试
- [ ] Oracle → PostgreSQL 端到端测试 (可选)
- [ ] 故障注入测试
- [ ] 性能回归测试

### Issue #53: 性能测试和优化
**标签**: `test`, `phase-8`, `performance`
**描述**: 进行性能测试并优化

**验收标准**:
- [ ] 大数据量测试 (亿级记录)
- [ ] 吞吐量测试
- [ ] 延迟测试
- [ ] 资源使用分析
- [ ] 性能优化实施

### Issue #54: 用户文档编写
**标签**: `documentation`, `phase-8`
**描述**: 编写完整的用户文档

**验收标准**:
- [ ] 快速开始指南
- [ ] 安装部署文档
- [ ] 配置参考手册
- [ ] 命令行使用文档
- [ ] FAQ 常见问题

### Issue #55: 开发者文档编写
**标签**: `documentation`, `phase-8`
**描述**: 编写开发者文档

**验收标准**:
- [ ] 架构设计文档
- [ ] 模块设计文档
- [ ] API 文档
- [ ] 扩展开发指南
- [ ] 代码贡献指南

### Issue #56: 示例和教程
**标签**: `documentation`, `phase-8`
**描述**: 提供实用示例和教程

**验收标准**:
- [ ] MySQL → PostgreSQL 迁移示例
- [ ] Oracle → PostgreSQL 迁移示例
- [ ] 自定义转换示例
- [ ] 故障排查教程
- [ ] 视频教程 (可选)

### Issue #57: 部署指南
**标签**: `documentation`, `phase-8`
**描述**: 编写生产环境部署指南

**验收标准**:
- [ ] 系统要求文档
- [ ] 单机部署指南
- [ ] 集群部署指南
- [ ] Docker 部署指南
- [ ] Kubernetes 部署指南 (可选)
- [ ] 安全配置指南

### Issue #58: 发布准备
**标签**: `task`, `phase-8`
**描述**: 准备首个正式版本发布

**验收标准**:
- [ ] 版本号确定
- [ ] CHANGELOG.md 编写
- [ ] Release Notes 准备
- [ ] 二进制分发包构建
- [ ] Docker 镜像构建和推送
- [ ] GitHub Release 发布

---

## 标签说明

- `task`: 开发任务
- `bug`: Bug 修复
- `enhancement`: 功能增强
- `documentation`: 文档相关
- `test`: 测试相关
- `phase-1` 到 `phase-8`: 对应各开发阶段
- `mysql`, `oracle`, `postgresql`: 特定数据库相关
- `database`: 数据库相关
- `performance`: 性能相关
- `p0`: 最高优先级
- `p1`: 高优先级
- `p2`: 中等优先级
- `p3`: 低优先级
