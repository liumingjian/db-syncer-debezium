#!/bin/bash

# Script to create all GitHub Issues for the project
# Run: ./scripts/create-issues.sh

REPO="liumingjian/db-syncer-debezium"

echo "Creating GitHub Issues for $REPO..."

# Phase 1 Issues
echo "Creating Phase 1 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 1] 项目初始化与仓库设置" \
  --body "**描述**: 初始化 Git 仓库,设置项目结构,配置 Maven 多模块项目

**验收标准**:
- [x] Git 仓库初始化完成
- [x] Maven 多模块项目结构创建(common, metadata-service, cli, connectors, transformations, monitoring)
- [x] 根 pom.xml 和各模块 pom.xml 配置完成
- [x] .gitignore 配置完成

**状态**: 已完成 ✅" \
  --label "task,phase-1"

gh issue create --repo "$REPO" \
  --title "[Phase 1] Docker Compose 开发环境配置" \
  --body "**描述**: 配置完整的 Docker Compose 本地开发环境

**验收标准**:
- [x] Zookeeper 和 Kafka 容器配置
- [x] Kafka Connect 容器配置
- [x] PostgreSQL 元数据库容器配置
- [x] MySQL 和 PostgreSQL 测试数据库容器配置
- [x] Prometheus 和 Grafana 监控容器配置
- [x] 所有容器可正常启动并通过健康检查

**状态**: 已完成 ✅" \
  --label "task,phase-1"

gh issue create --repo "$REPO" \
  --title "[Phase 1] CI/CD 流水线配置" \
  --body "**描述**: 设置 GitHub Actions 自动化构建和测试流程

**验收标准**:
- [ ] 创建 .github/workflows/build.yml
- [ ] 配置自动编译和单元测试
- [ ] 配置代码质量检查 (Checkstyle, SpotBugs)
- [ ] 配置测试覆盖率报告
- [ ] PR 合并前自动运行检查

**估时**: 1-2 天" \
  --label "task,phase-1"

gh issue create --repo "$REPO" \
  --title "[Phase 1] 项目文档编写" \
  --body "**描述**: 编写项目基础文档

**验收标准**:
- [x] README.md 完成
- [x] CONTRIBUTING.md 完成
- [x] LICENSE 文件添加
- [ ] docs/ARCHITECTURE.md 架构文档
- [ ] docs/DEVELOPMENT.md 开发指南

**估时**: 2-3 天" \
  --label "documentation,phase-1"

gh issue create --repo "$REPO" \
  --title "[Phase 1] 代码规范和格式化配置" \
  --body "**描述**: 配置统一的代码格式和检查工具

**验收标准**:
- [ ] 配置 Checkstyle (Google Java Style)
- [ ] 配置 SpotBugs
- [ ] 配置 Maven Formatter Plugin
- [ ] 添加 EditorConfig 文件
- [ ] 文档说明如何使用

**估时**: 1 天" \
  --label "task,phase-1"

gh issue create --repo "$REPO" \
  --title "[Phase 1] 基础日志框架配置" \
  --body "**描述**: 配置 SLF4J + Logback 日志框架

**验收标准**:
- [ ] 各模块添加 logback.xml 配置
- [ ] 配置日志级别和输出格式
- [ ] 配置文件滚动策略
- [ ] 区分开发和生产环境配置

**估时**: 1 天" \
  --label "task,phase-1"

# Phase 2 Issues
echo "Creating Phase 2 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 2] PostgreSQL 元数据 Schema 设计" \
  --body "**描述**: 设计并实现元数据数据库 Schema

**验收标准**:
- [ ] 设计 migration_tasks 表结构
- [ ] 设计 table_progress 表结构
- [ ] 设计 debezium_offsets 表结构
- [ ] 设计 schema_history 表结构
- [ ] 设计 connector_configs 表结构
- [ ] ER 图文档

**估时**: 2-3 天" \
  --label "task,phase-2,database"

gh issue create --repo "$REPO" \
  --title "[Phase 2] Flyway 数据库迁移配置" \
  --body "**描述**: 集成 Flyway 实现数据库版本管理

**验收标准**:
- [ ] 添加 Flyway 依赖
- [ ] 创建初始化 SQL 脚本 (V1__initial_schema.sql)
- [ ] 配置 Flyway 自动执行
- [ ] 测试迁移脚本执行

**估时**: 1 天" \
  --label "task,phase-2,database"

gh issue create --repo "$REPO" \
  --title "[Phase 2] JPA 实体类定义" \
  --body "**描述**: 定义所有元数据表对应的 JPA 实体类

**验收标准**:
- [ ] MigrationTask 实体
- [ ] TableProgress 实体
- [ ] DebeziumOffset 实体
- [ ] SchemaHistory 实体
- [ ] ConnectorConfig 实体
- [ ] 添加必要的关联关系和索引

**估时**: 2 天" \
  --label "task,phase-2"

gh issue create --repo "$REPO" \
  --title "[Phase 2] Repository 层实现" \
  --body "**描述**: 实现 Spring Data JPA Repository 接口

**验收标准**:
- [ ] TaskRepository 接口
- [ ] TableProgressRepository 接口
- [ ] DebeziumOffsetRepository 接口
- [ ] 自定义查询方法定义
- [ ] Repository 单元测试

**估时**: 2 天" \
  --label "task,phase-2"

gh issue create --repo "$REPO" \
  --title "[Phase 2] Service 层实现" \
  --body "**描述**: 实现元数据管理的业务逻辑层

**验收标准**:
- [ ] TaskService 实现(CRUD 操作)
- [ ] ProgressTrackingService 实现
- [ ] OffsetManagementService 实现
- [ ] 事务管理配置
- [ ] Service 层单元测试

**估时**: 3 天" \
  --label "task,phase-2"

echo "Created Phase 1 and 2 base issues. Create remaining issues manually or continue script..."
echo "Repository: https://github.com/$REPO/issues"
