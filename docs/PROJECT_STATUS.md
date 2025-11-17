# DB-Syncer-Debezium 项目状态

> 最后更新: 2025-11-17

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

## GitHub Issues 统计

| 阶段 | 描述 | Issues 数量 | 状态 |
|------|------|------------|------|
| Phase 1 | 项目基础设施 | 6 | ✅ 全部完成 |
| Phase 2 | 元数据管理系统 | 8 | ✅ 全部完成 |
| Phase 3 | CLI 命令行工具 | 7 | ✅ 全部完成 |
| Phase 4 | Debezium Connector 集成 | 8 | ✅ 全部完成 |
| Phase 5 | 数据转换层 | 6 | 全部待办 |
| Phase 6 | 任务执行引擎 | 9 | 全部待办 |
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

### Phase 5: 数据转换层 (2周) - 未开始

**关键任务**:
- Issue #18-20: 类型映射、SMT、Schema 转换

**预计开始时间**: 2026-01-06
**预计完成时间**: 2026-01-19

### Phase 6: 任务执行引擎 (2周) - 未开始

**关键任务**:
- Issue #21-28: JDBC Sink、任务编排、进度跟踪

**预计开始时间**: 2026-01-20
**预计完成时间**: 2026-02-02

### Phase 7: 监控与可观测性 (1周) - 未开始

**关键任务**:
- Issue #29-33: JMX、Prometheus、健康检查、告警

**预计开始时间**: 2026-02-03
**预计完成时间**: 2026-02-09

### Phase 8: 测试与文档 (3周) - 未开始

**关键任务**:
- Issue #34-38: 文档编写、测试、发布准备

**预计开始时间**: 2026-02-10
**预计完成时间**: 2026-03-02

## 下一步行动

### 立即可做 (本周)

1. **开始 Phase 5: 数据转换层**
   - [ ] 设计类型映射框架
   - [ ] 实现 MySQL → PostgreSQL 类型映射
   - [ ] 实现 Oracle → PostgreSQL 类型映射
   - [ ] 创建自定义 SMT (Single Message Transform)
   - [ ] 实现 Schema 转换逻辑
   - [ ] 添加数据转换测试

2. **环境验证**
   - [x] 验证 Docker 环境所有服务正常运行
   - [x] 测试 Kafka Connect 可访问性
   - [x] 验证元数据 PostgreSQL 连接
   - [x] CI/CD 流水线验证通过
   - [x] Connector 模块编译和测试通过

3. **代码质量**
   - [x] Checkstyle 代码规范检查
   - [x] SpotBugs 静态分析
   - [x] Connector 模块 38 个测试通过
   - [ ] 提升单元测试覆盖率到 60%
   - [ ] 添加更多集成测试 (Docker 环境)

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
