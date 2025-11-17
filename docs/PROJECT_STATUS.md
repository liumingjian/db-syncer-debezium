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
| Phase 1 | 项目基础设施 | 6 | 2 完成, 4 待办 |
| Phase 2 | 元数据管理系统 | 3 | 全部待办 |
| Phase 3 | CLI 命令行工具 | 7 | 全部待办 |
| Phase 4 | Debezium Connector 集成 | 2 | 全部待办 |
| Phase 5 | 数据转换层 | 3 | 全部待办 |
| Phase 6 | 任务执行引擎 | 8 | 全部待办 |
| Phase 7 | 监控与可观测性 | 5 | 全部待办 |
| Phase 8 | 测试与文档 | 4 | 全部待办 |
| **总计** | | **38** | **2 完成, 36 待办** |

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

### Phase 1: 项目基础设施 (2周) - 进行中

**已完成**:
- ✅ Issue #1: 项目初始化与仓库设置
- ✅ Issue #2: Docker Compose 开发环境配置

**待办**:
- ⏳ Issue #3: CI/CD 流水线配置
- ⏳ Issue #4: 项目文档编写 (架构文档、开发指南)
- ⏳ Issue #5: 代码规范和格式化配置
- ⏳ Issue #6: 基础日志框架配置

**预计完成时间**: 2025-11-24

### Phase 2: 元数据管理系统 (2周) - 未开始

**关键任务**:
- Issue #7: REST API 接口实现
- Issue #8: 元数据服务配置管理
- 数据库 Schema 设计 (需从脚本创建)
- JPA 实体和 Repository 层

**预计开始时间**: 2025-11-25
**预计完成时间**: 2025-12-08

### Phase 3: CLI 命令行工具 (2周) - 未开始

**关键任务**:
- Issue #9-15: Picocli 集成、命令实现、输出格式化

**预计开始时间**: 2025-12-09
**预计完成时间**: 2025-12-22

### Phase 4: Debezium Connector 集成 (2周) - 未开始

**关键任务**:
- Issue #16-17: Kafka Connect API、Connector 配置生成

**预计开始时间**: 2025-12-23
**预计完成时间**: 2026-01-05

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

1. **完成 Phase 1 剩余任务**
   - [ ] 配置 GitHub Actions CI/CD (#3)
   - [ ] 编写架构设计文档 (#4)
   - [ ] 配置代码格式化工具 (#5)
   - [ ] 配置 Logback 日志 (#6)

2. **环境验证**
   - [ ] 验证 Docker 环境所有服务正常运行
   - [ ] 测试 Kafka Connect 可访问性
   - [ ] 验证元数据 PostgreSQL 连接

3. **开发准备**
   - [ ] 设置 IDE 项目
   - [ ] 配置 Maven 本地仓库
   - [ ] 熟悉 Debezium 文档

### 短期目标 (2周内)

1. **启动 Phase 2 开发**
   - 设计元数据数据库 Schema
   - 实现 JPA 实体层
   - 开发 REST API

2. **团队协作**
   - 如有团队,分配 Issues
   - 建立代码审查流程
   - 设置开发分支策略

### 中期目标 (1个月内)

1. **完成核心框架**
   - Phase 1-2 全部完成
   - Phase 3 CLI 工具基本可用
   - 基础的任务创建和查询功能

2. **集成测试**
   - 元数据服务集成测试
   - CLI 命令集成测试

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
