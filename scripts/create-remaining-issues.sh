#!/bin/bash

# Script to create remaining GitHub Issues (Phase 2-8)
# Run: ./scripts/create-remaining-issues.sh

REPO="liumingjian/db-syncer-debezium"

echo "Creating remaining GitHub Issues for $REPO..."

# Phase 2 remaining issues
echo "Creating Phase 2 remaining issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 2] REST API 接口实现" \
  --body "**描述**: 实现元数据管理的 REST API

**验收标准**:
- [ ] TaskController (创建/查询/更新/删除任务)
- [ ] ProgressController (查询进度)
- [ ] 统一异常处理
- [ ] API 文档 (Swagger/OpenAPI)
- [ ] 集成测试

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 2] 元数据服务配置管理" \
  --body "**描述**: 实现配置文件管理和环境配置

**验收标准**:
- [ ] application.yml 基础配置
- [ ] application-dev.yml 开发环境配置
- [ ] application-prod.yml 生产环境配置
- [ ] 数据库连接池配置 (HikariCP)
- [ ] 敏感信息外部化

**估时**: 1 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 2] 元数据服务集成测试" \
  --body "**描述**: 使用 Testcontainers 实现集成测试

**验收标准**:
- [ ] Testcontainers PostgreSQL 配置
- [ ] Repository 集成测试
- [ ] Service 层集成测试
- [ ] REST API 集成测试
- [ ] 测试覆盖率 > 80%

**估时**: 2 天" \
  --label "test"

# Phase 3 issues
echo "Creating Phase 3 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 3] Picocli 框架集成" \
  --body "**描述**: 集成 Picocli CLI 框架

**验收标准**:
- [ ] Picocli 依赖配置
- [ ] 主命令类 DbSyncerCommand 创建
- [ ] 全局选项配置 (--verbose, --config)
- [ ] 版本信息和帮助文档
- [ ] Spring Boot 集成

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] Task 管理命令实现" \
  --body "**描述**: 实现任务管理相关的 CLI 命令

**验收标准**:
- [ ] \`task create\` 创建任务
- [ ] \`task list\` 列出任务
- [ ] \`task show <task-id>\` 查看任务详情
- [ ] \`task delete <task-id>\` 删除任务
- [ ] 参数验证和错误处理

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] Task 执行控制命令实现" \
  --body "**描述**: 实现任务执行控制命令

**验收标准**:
- [ ] \`task start <task-id>\` 启动任务
- [ ] \`task stop <task-id>\` 停止任务
- [ ] \`task pause <task-id>\` 暂停任务
- [ ] \`task resume <task-id>\` 恢复任务
- [ ] 状态验证逻辑

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] 监控和日志命令实现" \
  --body "**描述**: 实现任务监控和日志查看命令

**验收标准**:
- [ ] \`task status <task-id>\` 查看任务状态
- [ ] \`task logs <task-id>\` 查看任务日志
- [ ] \`monitor <task-id>\` 实时监控任务进度
- [ ] 终端 UI 进度条实现

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] 配置管理命令实现" \
  --body "**描述**: 实现配置管理命令

**验收标准**:
- [ ] \`config show\` 显示当前配置
- [ ] \`config set <key> <value>\` 设置配置项
- [ ] \`config validate\` 验证配置
- [ ] 配置文件读写逻辑

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] CLI 与元数据服务集成" \
  --body "**描述**: CLI 工具调用元数据服务 API

**验收标准**:
- [ ] HTTP 客户端配置 (RestTemplate/WebClient)
- [ ] API 调用封装
- [ ] 错误处理和重试逻辑
- [ ] 连接超时配置

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 3] CLI 输出格式化" \
  --body "**描述**: 优化 CLI 输出格式和用户体验

**验收标准**:
- [ ] 表格输出格式 (ASCII Table)
- [ ] JSON 输出选项 (--output json)
- [ ] ANSI 颜色支持
- [ ] 进度条和 Spinner 动画
- [ ] 优雅降级 (非 TTY 环境)

**估时**: 2 天" \
  --label "task"

# Phase 4 issues
echo "Creating Phase 4 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 4] Kafka Connect REST API 客户端" \
  --body "**描述**: 实现 Kafka Connect REST API 客户端

**验收标准**:
- [ ] 连接器部署 API
- [ ] 连接器删除 API
- [ ] 连接器状态查询 API
- [ ] 连接器暂停/恢复 API
- [ ] 错误处理

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 4] MySQL Source Connector 配置生成" \
  --body "**描述**: 实现 MySQL Source Connector 配置生成器

**验收标准**:
- [ ] 连接参数配置
- [ ] Binlog 位置配置
- [ ] 表白名单/黑名单配置
- [ ] Snapshot 模式配置
- [ ] 配置验证逻辑

**估时**: 3 天" \
  --label "task,mysql"

gh issue create --repo "$REPO" \
  --title "[Phase 4] Oracle Source Connector 配置生成" \
  --body "**描述**: 实现 Oracle Source Connector 配置生成器

**验收标准**:
- [ ] LogMiner 配置
- [ ] SCN 位置配置
- [ ] 表选择配置
- [ ] Snapshot 配置
- [ ] 权限验证

**估时**: 3 days" \
  --label "task,oracle"

gh issue create --repo "$REPO" \
  --title "[Phase 4] PostgreSQL Source Connector 配置生成" \
  --body "**描述**: 实现 PostgreSQL Source Connector 配置生成器

**验收标准**:
- [ ] Logical Decoding 配置
- [ ] Publication/Replication Slot 配置
- [ ] 表选择配置
- [ ] Snapshot 配置

**估时**: 2 天" \
  --label "task,postgresql"

gh issue create --repo "$REPO" \
  --title "[Phase 4] Connector 生命周期管理" \
  --body "**描述**: 实现 Connector 的完整生命周期管理

**验收标准**:
- [ ] Connector 部署逻辑
- [ ] Connector 删除逻辑
- [ ] Connector 状态监控
- [ ] Connector 错误处理和重启
- [ ] Connector 配置更新

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 4] Offset 存储集成" \
  --body "**描述**: 将 Debezium Offset 存储到 PostgreSQL

**验收标准**:
- [ ] 自定义 OffsetBackingStore 实现
- [ ] PostgreSQL 存储逻辑
- [ ] Offset 读取/写入
- [ ] 定期 Flush 机制
- [ ] 并发控制

**估时**: 3 天" \
  --label "task,database"

gh issue create --repo "$REPO" \
  --title "[Phase 4] Schema History 存储集成" \
  --body "**描述**: 将 Schema History 存储到 PostgreSQL

**验收标准**:
- [ ] 自定义 SchemaHistory 实现
- [ ] Schema 变更记录
- [ ] Schema 查询逻辑
- [ ] 版本管理

**估时**: 2 天" \
  --label "task,database"

gh issue create --repo "$REPO" \
  --title "[Phase 4] Connector 集成测试" \
  --body "**描述**: Connector 部署和运行的集成测试

**验收标准**:
- [ ] Testcontainers Kafka 和 Connect 配置
- [ ] MySQL Connector 部署测试
- [ ] Oracle Connector 部署测试 (可选)
- [ ] CDC 数据捕获验证
- [ ] Offset 持久化验证

**估时**: 3 天" \
  --label "test"

# Phase 5 issues
echo "Creating Phase 5 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 5] 类型映射框架设计" \
  --body "**描述**: 设计可扩展的类型映射框架

**验收标准**:
- [ ] TypeMapper 接口定义
- [ ] TypeMappingRegistry 注册表
- [ ] 类型转换器接口
- [ ] 映射规则配置格式

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 5] MySQL → PostgreSQL 类型映射" \
  --body "**描述**: 实现完整的 MySQL 到 PostgreSQL 类型映射

**验收标准**:
- [ ] 数值类型映射 (INT, BIGINT, DECIMAL, etc.)
- [ ] 字符串类型映射 (VARCHAR, TEXT, CHAR, etc.)
- [ ] 日期时间类型映射 (DATETIME, TIMESTAMP, DATE, etc.)
- [ ] 二进制类型映射 (BLOB, BINARY, etc.)
- [ ] JSON 类型映射
- [ ] 单元测试覆盖

**估时**: 3 天" \
  --label "task,mysql,postgresql"

gh issue create --repo "$REPO" \
  --title "[Phase 5] Oracle → PostgreSQL 类型映射" \
  --body "**描述**: 实现完整的 Oracle 到 PostgreSQL 类型映射

**验收标准**:
- [ ] NUMBER 类型映射
- [ ] VARCHAR2/CHAR 类型映射
- [ ] DATE/TIMESTAMP 类型映射
- [ ] CLOB/BLOB 类型映射
- [ ] RAW 类型映射
- [ ] 单元测试覆盖

**估时**: 3 天" \
  --label "task,oracle,postgresql"

gh issue create --repo "$REPO" \
  --title "[Phase 5] 自定义 SMT (Single Message Transform)" \
  --body "**描述**: 实现自定义 Kafka Connect SMT

**验收标准**:
- [ ] TypeConversionTransform 实现
- [ ] ColumnRenameTransform 实现
- [ ] ValueTransform 实现
- [ ] SMT 配置接口
- [ ] 单元测试

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 5] Schema 转换逻辑" \
  --body "**描述**: 实现 Schema 定义的转换逻辑

**验收标准**:
- [ ] Debezium Schema 解析
- [ ] 目标 Schema 生成
- [ ] DDL 语句生成
- [ ] Schema 差异检测
- [ ] 单元测试

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 5] 数据转换集成测试" \
  --body "**描述**: 端到端的数据转换测试

**验收标准**:
- [ ] 各类型转换正确性验证
- [ ] 边界值测试
- [ ] NULL 值处理测试
- [ ] 大数据量转换性能测试

**估时**: 2 天" \
  --label "test"

# Phase 6 issues
echo "Creating Phase 6 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 6] Debezium JDBC Sink Connector 集成" \
  --body "**描述**: 集成 Debezium JDBC Sink Connector

**验收标准**:
- [ ] JDBC Sink Connector 依赖
- [ ] PostgreSQL Sink 配置生成
- [ ] Upsert 语义配置
- [ ] 批处理配置
- [ ] 错误处理配置

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 任务编排引擎" \
  --body "**描述**: 实现完整的任务编排和执行引擎

**验收标准**:
- [ ] TaskOrchestrator 核心类
- [ ] 任务状态机实现
- [ ] Source Connector 部署
- [ ] Sink Connector 部署
- [ ] 任务协调逻辑

**估时**: 4 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 任务启动逻辑" \
  --body "**描述**: 实现任务启动的完整流程

**验收标准**:
- [ ] 参数验证
- [ ] 前置检查 (数据库连接、权限等)
- [ ] Schema 初始化
- [ ] Connector 部署
- [ ] 状态更新

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 任务停止和暂停逻辑" \
  --body "**描述**: 实现任务停止和暂停的逻辑

**验收标准**:
- [ ] 优雅停止逻辑
- [ ] Connector 删除/暂停
- [ ] Offset 保存
- [ ] 状态清理
- [ ] 恢复能力

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 进度跟踪实现" \
  --body "**描述**: 实现实时进度跟踪功能

**验收标准**:
- [ ] Connector 指标采集
- [ ] 表级进度计算
- [ ] 记录数统计
- [ ] ETA 估算
- [ ] 数据库持久化

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 错误处理和重试机制" \
  --body "**描述**: 实现健壮的错误处理和重试逻辑

**验收标准**:
- [ ] 异常分类(可重试/不可重试)
- [ ] 指数退避重试
- [ ] 失败记录持久化
- [ ] Dead Letter Queue
- [ ] 告警通知接口

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 增量快照支持" \
  --body "**描述**: 支持 Debezium 增量快照功能

**验收标准**:
- [ ] 增量快照配置
- [ ] 分块大小配置
- [ ] 并行度配置
- [ ] 进度跟踪
- [ ] 断点续传

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 多表并行迁移" \
  --body "**描述**: 支持同一任务内多表并行迁移

**验收标准**:
- [ ] 表分组逻辑
- [ ] 并行度控制
- [ ] 资源限制
- [ ] 进度汇总
- [ ] 部分失败处理

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 6] 任务执行集成测试" \
  --body "**描述**: 端到端的任务执行测试

**验收标准**:
- [ ] 完整迁移流程测试
- [ ] 数据一致性验证
- [ ] 故障恢复测试
- [ ] 性能基准测试

**估时**: 4 天" \
  --label "test"

# Phase 7 issues
echo "Creating Phase 7 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 7] JMX 指标暴露" \
  --body "**描述**: 暴露 JMX 监控指标

**验收标准**:
- [ ] Connector 指标采集
- [ ] 任务执行指标
- [ ] 资源使用指标
- [ ] JMX MBean 注册
- [ ] 指标文档

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 7] Prometheus 集成" \
  --body "**描述**: 集成 Prometheus 监控

**验收标准**:
- [ ] Micrometer 依赖
- [ ] 自定义指标定义
- [ ] Prometheus Endpoint 暴露
- [ ] Grafana Dashboard 定义

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 7] 健康检查接口" \
  --body "**描述**: 实现健康检查和就绪检查

**验收标准**:
- [ ] /health endpoint
- [ ] /ready endpoint
- [ ] 数据库连接检查
- [ ] Kafka 连接检查
- [ ] Connector 状态检查

**估时**: 1 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 7] 任务监控 Dashboard" \
  --body "**描述**: 实现 CLI 任务监控终端 UI

**验收标准**:
- [ ] 实时进度条
- [ ] 多表进度显示
- [ ] 速率统计
- [ ] ETA 显示
- [ ] 终端 UI 库选择 (Lanterna/Jexer)

**估时**: 3 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 7] 日志增强" \
  --body "**描述**: 增强日志记录和查询功能

**验收标准**:
- [ ] 结构化日志 (JSON)
- [ ] 任务关联日志
- [ ] 日志级别动态调整
- [ ] 日志查询 API
- [ ] 日志聚合配置 (ELK/Loki)

**估时**: 2 天" \
  --label "task"

gh issue create --repo "$REPO" \
  --title "[Phase 7] 告警机制" \
  --body "**描述**: 实现告警通知机制

**验收标准**:
- [ ] 告警规则定义
- [ ] 邮件通知实现
- [ ] Webhook 通知
- [ ] 告警历史记录
- [ ] 告警配置界面

**估时**: 3 天" \
  --label "task"

# Phase 8 issues
echo "Creating Phase 8 issues..."

gh issue create --repo "$REPO" \
  --title "[Phase 8] 单元测试覆盖率提升" \
  --body "**描述**: 提升整体单元测试覆盖率到 80%+

**验收标准**:
- [ ] 所有模块覆盖率 > 80%
- [ ] 核心逻辑覆盖率 > 90%
- [ ] 测试报告生成
- [ ] Jacoco 集成

**估时**: 3 天" \
  --label "test"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 集成测试套件" \
  --body "**描述**: 完善集成测试套件

**验收标准**:
- [ ] Testcontainers 完整配置
- [ ] MySQL → PostgreSQL 端到端测试
- [ ] Oracle → PostgreSQL 端到端测试 (可选)
- [ ] 故障注入测试
- [ ] 性能回归测试

**估时**: 4 天" \
  --label "test"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 性能测试和优化" \
  --body "**描述**: 进行性能测试并优化

**验收标准**:
- [ ] 大数据量测试 (亿级记录)
- [ ] 吞吐量测试
- [ ] 延迟测试
- [ ] 资源使用分析
- [ ] 性能优化实施

**估时**: 5 天" \
  --label "test,performance"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 用户文档编写" \
  --body "**描述**: 编写完整的用户文档

**验收标准**:
- [ ] 快速开始指南
- [ ] 安装部署文档
- [ ] 配置参考手册
- [ ] 命令行使用文档
- [ ] FAQ 常见问题

**估时**: 4 天" \
  --label "documentation"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 开发者文档编写" \
  --body "**描述**: 编写开发者文档

**验收标准**:
- [ ] 架构设计文档
- [ ] 模块设计文档
- [ ] API 文档
- [ ] 扩展开发指南
- [ ] 代码贡献指南

**估时**: 4 天" \
  --label "documentation"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 示例和教程" \
  --body "**描述**: 提供实用示例和教程

**验收标准**:
- [ ] MySQL → PostgreSQL 迁移示例
- [ ] Oracle → PostgreSQL 迁移示例
- [ ] 自定义转换示例
- [ ] 故障排查教程
- [ ] 视频教程 (可选)

**估时**: 3 天" \
  --label "documentation"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 部署指南" \
  --body "**描述**: 编写生产环境部署指南

**验收标准**:
- [ ] 系统要求文档
- [ ] 单机部署指南
- [ ] 集群部署指南
- [ ] Docker 部署指南
- [ ] Kubernetes 部署指南 (可选)
- [ ] 安全配置指南

**估时**: 3 天" \
  --label "documentation"

gh issue create --repo "$REPO" \
  --title "[Phase 8] 发布准备" \
  --body "**描述**: 准备首个正式版本发布

**验收标准**:
- [ ] 版本号确定
- [ ] CHANGELOG.md 编写
- [ ] Release Notes 准备
- [ ] 二进制分发包构建
- [ ] Docker 镜像构建和推送
- [ ] GitHub Release 发布

**估时**: 2 天" \
  --label "task"

echo ""
echo "✅ All GitHub Issues have been created!"
echo "View them at: https://github.com/$REPO/issues"
echo ""
echo "Summary:"
echo "- Phase 2: 3 remaining issues"
echo "- Phase 3: 7 issues"
echo "- Phase 4: 8 issues"
echo "- Phase 5: 6 issues"
echo "- Phase 6: 9 issues"
echo "- Phase 7: 6 issues"
echo "- Phase 8: 8 issues"
echo "Total new issues: 47"
