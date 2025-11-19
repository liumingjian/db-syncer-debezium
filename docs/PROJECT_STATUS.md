# DB-Syncer-Debezium 项目状态

> 最后更新: 2025-11-18

## 项目概览

**仓库**: https://github.com/liumingjian/db-syncer-debezium

**描述**: 基于 Debezium CDC 的异构数据库迁移工具,支持 Oracle、MySQL、PostgreSQL 等数据库之间的实时数据同步与迁移。

## 当前状态

### ✅ 已完成

1. **项目初始化**
   - Git 仓库创建和配置
   - Maven 多模块项目结构 (6 个模块)
   - 完整的依赖管理配置

2. **开发环境**
   - Docker Compose 配置完成
   - 包含 Kafka, Kafka Connect, PostgreSQL, MySQL, Prometheus, Grafana
   - 所有服务已验证可正常启动

3. **项目文档**
   - README.md - 项目介绍和快速开始
   - CONTRIBUTING.md - 贡献指南
   - LICENSE - Apache 2.0
   - ISSUES_PLAN.md - 完整开发计划
   - 本文档 - 项目状态跟踪

4. **GitHub 配置**
   - Issue 模板 (Bug Report, Feature Request, Task)
   - 自动化 Issue 创建脚本
   - 38 个详细的开发任务 Issues

5. **MVP 与运维文档（草稿）**
   - MVP 能力标准: `docs/MVP_CRITERIA.md`
   - 生产运行手册: `docs/OPERATIONS.md`

## GitHub Issues 统计

| 阶段 | 描述 | Issues 数量 | 状态 |
|------|------|------------|------|
| Phase 1 | 项目基础设施 | 6 | ✅ 全部完成 |
| Phase 2 | 元数据管理系统 | 8 | ✅ 全部完成 |
| Phase 3 | CLI 命令行工具 | 7 | ✅ 全部完成 |
| Phase 4 | Debezium Connector 集成 | 8 | ✅ 全部完成 |
| Phase 5 | 数据转换层 | 6 | 4 完成, 2 待办 |
| Phase 6 | 任务执行引擎 | 9 | 4 完成, 5 待办 |
| Phase 7 | 监控与可观测性 | 6 | 全部待办 |
| Phase 8 | 测试与文档 | 8 | 全部待办 |
| **总计** | | **58** | **29 完成, 29 待办** |

## 技术栈

### 核心框架
- **Java**: 17 (LTS)
- **构建工具**: Maven 3.8+
- **Spring Boot**: 3.2.5

### CDC 和消息队列
- **Debezium**: 2.6.2
- **Apache Kafka**: 3.7.0
- **Kafka Connect**: Debezium Connect Image 2.5

### 数据库
- **PostgreSQL**: 12+ (元数据存储)
- **MySQL**: 8.0 (源数据库支持)
- **Oracle**: 支持规划中

### CLI 和工具
- **Picocli**: 4.7.5 (CLI 框架)
- **Lombok**: 1.18.32
- **Jackson**: 2.17.1

### 监控
- **Prometheus**: Latest
- **Grafana**: Latest
- **Micrometer**: Spring Boot 集成

### 测试
- **JUnit**: 5.10.2
- **Mockito**: 5.11.0
- **Testcontainers**: 1.19.8

## 项目模块

```
db-syncer-debezium/
├── common/                 # 公共工具和模型
├── metadata-service/       # 元数据管理服务 (Spring Boot + JPA)
├── cli/                    # 命令行工具 (Picocli)
├── connectors/             # 自定义 Connector 扩展
├── transformations/        # 数据转换 SMT
├── monitoring/             # 监控指标收集
├── docker/                 # Docker 开发环境
├── docs/                   # 项目文档
└── scripts/                # 自动化脚本
```

## 开发路线图

### Phase 1: 项目基础设施 (2周) - ✅ 已完成

**已完成**:
- ✅ Issue #1: 项目初始化与仓库设置
- ✅ Issue #2: Docker Compose 开发环境配置
- ✅ Issue #3: CI/CD 流水线配置 (GitHub Actions)
- ✅ Issue #4: 项目文档编写 (架构文档、开发指南)
- ✅ Issue #5: 代码规范和格式化配置 (Checkstyle, SpotBugs, JaCoCo)
- ✅ Issue #6: 基础日志框架配置 (Logback)

**完成时间**: 2025-11-17

### Phase 2: 元数据管理系统 (2周) - ✅ 已完成

**已完成**:
- ✅ Issue #7: PostgreSQL Schema 设计和 Flyway 迁移
- ✅ Issue #8: JPA 实体层 (MigrationTask, TableProgress, ConnectorConfig)
- ✅ Issue #9: Repository 层实现 (Spring Data JPA)
- ✅ Issue #10: 服务层业务逻辑 (TaskService, ProgressTrackingService)
- ✅ Issue #11: REST API 控制器 (TaskController, ProgressController)
- ✅ Issue #12: DTO 层 (Request/Response 对象)
- ✅ Issue #13: 全局异常处理 (GlobalExceptionHandler)
- ✅ Issue #14: 配置管理 (application.yml for dev/prod/test)

**完成时间**: 2025-11-17

### Phase 3: CLI 命令行工具 (2周) - ✅ 已完成

**已完成**:
- ✅ Issue #15: Picocli 框架集成和主入口
- ✅ Issue #16: task 命令组 (create, list, show, delete)
- ✅ Issue #17: start/stop/pause/resume 命令
- ✅ Issue #18: config 命令组 (connector 配置管理)
- ✅ Issue #19: status 命令 (任务状态和进度查询)
- ✅ Issue #20: 输出格式化 (Table, JSON, YAML)
- ✅ Issue #21: 配置文件支持 (.dbsyncer.yml)

**完成时间**: 2025-11-17

### Phase 4: Debezium Connector 集成 (2周) - ✅ 已完成

**已完成**:
- ✅ Issue #22: Kafka Connect REST API 客户端
- ✅ Issue #23: MySQL Source Connector 配置生成器
- ✅ Issue #24: Oracle Source Connector 配置生成器
- ✅ Issue #25: PostgreSQL Source Connector 配置生成器
- ✅ Issue #26: Connector 生命周期管理
- ✅ Issue #27: PostgreSQL-based Offset 存储
- ✅ Issue #28: PostgreSQL-based Schema History 存储
- ✅ Issue #29: Connector 集成测试 (38个测试通过)

**完成时间**: 2025-11-17

### Phase 5: 数据转换层 (2周) - 部分完成

**已完成（代码已落地）**:
- ✅ Issue #30: 类型映射框架设计（TypeMapper/TypeMappingRegistry/规则格式）
- ✅ Issue #31: MySQL → PostgreSQL 类型映射（含单元测试）
- ✅ Issue #32: Oracle → PostgreSQL 类型映射（含单元测试）
- ✅ Issue #34: Schema 转换逻辑（Debezium Schema 解析、DDL 生成、差异检测）

**进行中/待办**:
- ⏳ Issue #33: 自定义 SMT 组合（TypeConversion/ColumnRename/ValueTransform 全量实现）
- ⏳ Issue #35: 数据转换端到端集成测试（正确性/边界值/性能）

### Phase 6: 任务执行引擎 (2周) - 进行中

**已完成**:
- ✅ Issue #36: JDBC Sink Connector 集成（upsert/批处理/schema/SMT/regex 路由）
- ✅ Issue #37: 任务编排引擎（TaskExecutionService，覆盖保存配置/部署/等待/回填）
- ✅ Issue #38: 任务启动流程（参数校验、history 初始化、部署、状态更新）
- ✅ Issue #39: 停止/暂停/恢复（删除/暂停/恢复 Connector，状态更新）

**进行中/待办**:
- ⏳ Issue #40: 进度跟踪（指标采集、表级进度、ETA、持久化）
- ⏳ Issue #41: 错误处理与重试（异常分类、指数退避、DLQ、告警）
- ⏳ Issue #42: 增量快照支持（配置、分块、并行度、进度与断点续传）
- ⏳ Issue #43: 多表并行迁移（分组、并行度、资源与汇总）
- ⏳ Issue #44: 任务执行集成测试（端到端/一致性/恢复/性能）

### Phase 7: 监控与可观测性 (1周) - 部分完成

**已完成（基础能力）**:
- ✅ Issue #46: Prometheus 集成（Micrometer 依赖、自定义指标、/actuator/prometheus 暴露）
- ✅ Issue #47: 健康检查接口（/health、/ready、DB 与 Kafka Connect 健康检查）
- ✅ Issue #48: CLI 任务监控 Dashboard（实时进度条、多表进度、速率、ETA）
- ✅ Issue #49: 日志增强（结构化 JSON 日志、任务关联日志、日志查询 API）
- ✅ Issue #50: 告警机制骨架（AlertRule/AlertEvent 实体、任务失败触发告警事件）

**进行中/待办**:
- ⏳ Issue #45: JMX 指标暴露（按 Connector/任务维度的 MBean）
- ⏳ Issue #46: Grafana Dashboard 模板与运维落地
- ⏳ Issue #49: 日志级别动态调整与日志聚合平台配置
- ⏳ Issue #50: 外部邮件/Webhook 发送实现与配置界面

### Phase 8: 测试与文档 (3周) - 未开始

**关键任务**:
- Issue #34-38: 文档编写、测试、发布准备

**预计开始时间**: 2026-02-10
**预计完成时间**: 2026-03-02

## 下一步行动

### 立即可做 (本周)

> 说明：本节侧重“总体开发路线”。若要评估是否具备生产 MVP 能力，请结合 `docs/MVP_CRITERIA.md`。

1. **面向生产 MVP 的优先事项**
   - [ ] 完成任务进度跟踪与断点续传（对应 Issue #40，参考 MVP_CRITERIA 2.x）
   - [ ] 完成错误分类与重试策略（对应 Issue #41，参考 MVP_CRITERIA 2.x）
   - [ ] 实现至少一套自动化 E2E 测试（MySQL → PostgreSQL，参考 MVP_CRITERIA 6.x）
   - [ ] 落地基础监控指标与告警（对应 Phase 7 核心 Issue，参考 MVP_CRITERIA 3.x）
   - [ ] 完成首版生产运维文档并回收试点反馈（`docs/OPERATIONS.md`）

2. **Phase 6 收尾**
   - [ ] 进度跟踪指标（任务级/表级/ETA）
   - [ ] 错误处理与重试（分类/退避/DLQ/告警）
   - [ ] 增量快照/多表并行能力
   - [ ] 任务执行端到端集成测试

3. **Phase 5: 数据转换层**
   - [ ] 类型映射框架与默认规则完善
   - [ ] 时间/JSON/Decimal 细节回归测试

4. **环境与质量**
   - [x] Docker 环境验证
   - [x] Kafka Connect 可访问性
   - [x] 元数据 PostgreSQL 连接验证
   - [x] Connector 模块编译与测试
   - [ ] 覆盖率提升到 60%+
   - [ ] E2E 集成测试补齐

### 短期目标 (2周内)

1. **完成 Phase 5-6**
   - 数据转换层完成
   - JDBC Sink 集成
   - 任务编排引擎
   - 完整的 CDC 数据同步功能

2. **端到端测试**
   - CLI → Metadata Service → Kafka Connect 集成
   - MySQL → PostgreSQL 同步测试
   - 数据类型转换验证

### 中期目标 (1个月内)

1. **完成核心框架**
   - Phase 5-8 全部完成
   - 完整的数据迁移流程
   - 任务执行和进度跟踪
   - 监控与可观测性

2. **集成测试**
   - 端到端迁移测试
   - 性能和压力测试
   - 文档完善

## 资源链接

### 仓库
- **主仓库**: https://github.com/liumingjian/db-syncer-debezium
- **Issues**: https://github.com/liumingjian/db-syncer-debezium/issues

### 文档
- **Debezium 官方文档**: https://debezium.io/documentation/
- **Kafka Connect 文档**: https://kafka.apache.org/documentation/#connect
- **Picocli 文档**: https://picocli.info/

### 本地环境
- **Kafka Connect UI**: http://localhost:8083
- **Grafana**: http://localhost:3005 (admin/admin)
- **Prometheus**: http://localhost:9090
- **PostgreSQL 元数据**: localhost:5432 (dbsyncer/dbsyncer_pass)
- **MySQL 测试源**: localhost:3306 (root/mysql_root_pass)
- **PostgreSQL 测试目标**: localhost:5433 (targetuser/targetpass)

## 贡献指南

参见 [CONTRIBUTING.md](../CONTRIBUTING.md)

## 许可证

Apache License 2.0 - 详见 [LICENSE](../LICENSE)

---

**项目发起时间**: 2025-11-17
**预计首个版本发布**: 2026-03-02 (v1.0.0)
