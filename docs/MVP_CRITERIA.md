# DB-Syncer-Debezium MVP 标准（草稿）

> 目标：定义“首个可在受控生产环境中使用”的最小可行能力（MVP），用于评估当前实现是否具备上线条件，并指导后续迭代优先级。

最后更新：2025-11-18  
适用范围：本仓库（`cli/`、`metadata-service/`、`connectors/`、`transformations/`、`monitoring/`、`docker/` 等模块。

---

## 1. 功能闭环（Functionality）

**目标**：完整打通从源库到目标库的 CDC 迁移链路，在明确边界内稳定工作。

- [ ] **单向迁移链路打通**
  - CLI → Metadata Service → Kafka Connect → Debezium Source → Kafka → Transform → JDBC Sink → 目标数据库。
  - 验收：在本地 docker 环境中，完成一次 MySQL → PostgreSQL 的端到端同步（含初始快照 + 实时增量）。

- [ ] **支持场景明确且收敛**
  - 首个生产 MVP 仅支持：MySQL → PostgreSQL 单向迁移（单库/有限表数量），Oracle 标记为“未支持/实验性”。
  - 验收：README / 文档中清晰列出“支持/不支持/实验性”场景。

- [ ] **基础 Schema / 类型转换可用**
  - transformations 模块提供默认类型映射（包含常见整数、浮点、字符串、时间、JSON、Decimal 等）。
  - ApplyTypeMapping SMT 在生产链路中可选启用，行为稳定。
  - 验收：对常见类型组合（参见 `docs/MAPPING_REFERENCE.md`）的回归测试通过。

- [ ] **DDL 兼容与演进的基础策略**
  - 目标表不存在时能自动创建（或生成 DDL 供 DBA 审核执行）。
  - 目标表已存在时，支持“列新增/类型放宽”的基本演进（至少不因新增可空列导致任务整体失败）。
  - 验收：通过 CLI `transform ddl/schema` 命令 + 端到端测试覆盖典型 DDL 场景。

---

## 2. 一致性与容错（Reliability & Consistency）

**目标**：在受控故障与业务变动下，任务能合理处理错误、支持断点续传，确保数据在预期语义下最终一致。

- [ ] **任务生命周期管理健壮**
  - 支持 `start / pause / resume / stop`，并校验非法状态转换（例如重复 start）。
  - 状态变更在元数据库中持久化（如：PENDING、STARTING、RUNNING、PAUSED、STOPPED、FAILED 等）。
  - 验收：TaskExecutionService 单元/集成测试覆盖正常与异常状态流转。

- [ ] **进度跟踪与断点续传**
  - 任务级、表级进度持久化：当前 offset、已迁移行数、最近事件时间戳等（如 `migration_tasks` / `table_progress`）。
  - 在任务/服务重启后，可从最近一致位置恢复，不发生不可接受的数据丢失或重复。
  - 验收：端到端测试覆盖“进程重启后继续同步”的场景。

- [ ] **错误分类与重试策略**
  - 对典型错误进行分类：网络抖动、数据库暂时性不可用、权限问题、目标库约束冲突、Schema 不兼容等。
  - 对暂时性错误采用有限次数 + 指数退避重试，对永久性错误快速失败并标记任务为 FAILED。
  - 验收：针对不同错误类型的模拟测试（可使用 Testcontainers 或手工编排）行为符合预期。

- [ ] **大表 / 多表场景的基础策略**
  - 支持对大表的增量快照（Chunking）与限流，避免对源/目标库造成过大压力。
  - 支持多表迁移的并行度配置（任务级/连接器级），并在文档中给出推荐值。
  - 验收：在测试环境中，对包含大表和多表的库进行迁移演练，确认资源占用可控。

---

## 3. 可观测性（Observability）

**目标**：运维和开发人员能够实时了解任务状态、发现异常并定位问题。

- [ ] **Metrics 指标暴露**
  - 至少暴露以下 Prometheus 指标（按任务维度）：  
    - `dbsyncer_task_state`（任务状态）  
    - `dbsyncer_task_processed_records_total`（已处理记录数）  
    - `dbsyncer_task_failed_records_total`（失败记录数）  
    - `dbsyncer_task_lag_seconds`（延迟）  
    - `dbsyncer_task_last_event_timestamp`（最后事件时间）
  - 验收：在本地 docker 环境中，Prometheus 能抓取上述指标，Grafana 有基础看板。

- [ ] **日志与追踪信息完整**
  - 所有关键操作（Connector 创建/更新/删除、任务状态变化、严重错误）都有结构化日志。
  - 日志中统一包含 `taskId`、`connectorName`、source/target 标识，便于关联排查。
  - 验收：日志能够完整还原一次任务从创建到停止的生命周期。

- [ ] **告警机制（最小版本）**
  - 至少支持基于 Prometheus 规则或简单 Webhook 的告警机制：
    - 任务长时间处于 FAILED/ERROR 状态；
    - 任务 lag 超过阈值；
    - 关键服务（metadata-service、Kafka Connect）不可用。
  - 验收：在测试环境中模拟异常，能触发告警并被运维/开发接收。

---

## 4. 运维与操作性（Operability）

**目标**：生产环境下，迁移任务可被方便地配置、运行、停机和排障。

- [ ] **CLI 操作闭环**
  - CLI 支持完整的任务管理命令：`task create/list/show/start/stop/pause/resume/delete`。
  - 提供 `status` 和 `monitor` 命令，用于查看任务状态和基础进度。
  - 验收：运维人员可以仅通过 CLI 完成一次任务的完整管理流程。

- [ ] **配置管理与环境隔离**
  - 支持 `.dbsyncer.yml` 配置文件 + 环境变量覆盖，便于 dev/stage/prod 环境切换。
  - 提供生产环境推荐配置示例（包括超时、重试、并行度、连接池大小等）。
  - 验收：使用不同配置文件能在多套环境中稳定运行同一版本二进制。

- [ ] **失败与回滚策略**
  - 明确一次迁移失败后的处理方式：  
    - 是通过 truncate 目标表重新执行；  
    - 还是手工清理不一致数据后重试。
  - 在文档中列出典型故障场景及推荐处理步骤。
  - 验收：故障演练中，运维人员可以按照文档完成安全回滚或恢复。

- [ ] **升级策略与兼容性说明**
  - 明确工具版本升级时，对现有任务、offset、schema history 的影响。
  - 需要时提供简单的数据迁移/升级脚本或操作指引。
  - 验收：从一个小版本升级到下一个小版本时，现有任务在可接受的中断/切换策略下保持可用。

---

## 5. 安全性（Security）

**目标**：在生产环境使用时，避免明显的安全风险。

- [ ] **凭证管理与敏感信息保护**
  - 数据库密码、Kafka 凭证等敏感信息不出现在日志和默认配置示例中。
  - 推荐使用环境变量、Secret 管理（如 K8s Secret、Vault 等）来存储敏感信息。
  - 验收：在正常运行和故障排查日志中看不到明文密码。

- [ ] **权限最小化**
  - 文档中列出源库/目标库所需的最小权限集合（如 MySQL REPLICATION、PostgreSQL INSERT/UPDATE/DELETE 等）。
  - 建议避免使用超级用户账号。
  - 验收：在只授予最小权限的账号下，E2E 测试能够通过。

---

## 6. 测试与文档（Testing & Documentation）

**目标**：通过自动化测试和清晰文档，降低上线和日常运维风险。

- [ ] **自动化端到端测试**
  - 至少提供一套基于 Testcontainers 或 docker-compose 的 E2E 测试：
    - MySQL → PostgreSQL 完整链路（包含初始快照 + 增量）；  
    - 覆盖 restart、pause/resume、部分故障场景。
  - 验收：在 CI 或指定环境中，可以一键运行 E2E 测试并查看结果。

- [ ] **关键模块单元/集成测试**
  - 以下模块有覆盖率可接受的测试：  
    - 连接器配置生成（MySql/Oracle/Postgres Source/Sink）；  
    - TaskExecutionService / TaskService；  
    - ApplyTypeMapping SMT 及类型映射规则；  
    - 元数据持久化（Repository 层）。
  - 验收：整体测试覆盖率达到预设目标（例如 60%+），并在 PR 检查中强制执行。

- [ ] **运行与运维文档**
  - README 中给出快速开始 + 本地 E2E 示例。
  - docs 下提供生产运维手册（建议新建 `docs/OPERATIONS.md`），包含：
    - 支持场景与边界；  
    - 配置说明；  
    - 监控指标说明；  
    - 常见故障与排查流程。
  - 验收：新接触项目的工程师/运维能在一两天内依据文档独立完成一次迁移演练。

---

## 使用方式建议

- 在每个里程碑前后更新本文件的勾选状态，形成“当前能力快照”；  
- 在 `docs/PROJECT_STATUS.md` 中引用本文件，将 Issue/模块与上述条目进行映射，帮助确定优先级；  
- 在规划首个生产环境试点前，至少保证本文件中标记为“必选”的条目全部打勾，其余条目根据业务风险和资源评估决定是否纳入首批范围。

