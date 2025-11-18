# DB-Syncer-Debezium 生产运行手册（草稿）

> 本文档面向运维工程师和负责迁移任务的后端工程师，描述在 **受控生产环境** 中使用 DB-Syncer-Debezium 的推荐实践。  
> 当前项目仍处于早期阶段，部分能力尚未完全实现，请结合 `docs/PROJECT_STATUS.md` 与 `docs/MVP_CRITERIA.md` 一起评估风险。

最后更新：2025-11-18

---

## 1. 支持场景与边界

### 1.1 已规划的首个生产场景（建议范围）

- 源数据库：MySQL 8.x（单实例或主从集群的只读节点）
- 目标数据库：PostgreSQL 12+（单实例或高可用集群的任一写节点）
- 迁移方向：**单向**（MySQL → PostgreSQL），无反向或双向同步
- 数据类型：常见业务字段（整数、小数、字符串、时间、JSON 等），详见 `docs/MAPPING_REFERENCE.md`
- 规模建议（首批试点）：
  - 单库内表数量：≤ 50 张
  - 单表数据量：≤ 数千万行（大表需要分批迁移和容量评估）
  - 变更速率：TPS 中等（每秒几十到几百条 CDC 事件）

> 对 Oracle 源库、跨 Region、高 TPS 场景，目前仅建议在测试环境或小流量试点中使用，不建议直接上核心生产链路。

### 1.2 当前主要限制

- 多源多目标复杂拓扑、双向同步未提供官方支持；
- 增量快照、多表并行、自动限流等高级能力尚在迭代中；
- 监控和告警能力为最小可用版本，需要结合现有监控体系补足；
- 回滚和重放依赖明确的业务策略与人工介入，暂未实现“一键回滚”。

---

## 2. 生产前准备

### 2.1 基础设施与组件

在生产环境中，建议至少包含以下组件（可部署在 K8s 或独立虚机上）：

- **Zookeeper + Kafka 集群**
  - 用于 Debezium Source Connector 推送 CDC 事件
- **Kafka Connect 集群**
  - 运行 Debezium Source Connector 与 JDBC Sink Connector
- **Metadata PostgreSQL**
  - 存储迁移任务元数据、进度、Connector 配置等
- **源数据库（MySQL）**
- **目标数据库（PostgreSQL）**
- **监控系统**
  - Prometheus + Grafana 或公司已有监控栈
- **日志收集系统**
  - 如 ELK / Loki / Splunk 等，用于集中分析日志

> 建议先在测试/预生产环境中用 `docker/docker-compose.yml` 对应的组件拓扑跑通一套完整链路，再在生产中按相同模式部署对应组件。

### 2.2 权限准备

- **MySQL 源库账号**（示例）：
  - 最小权限建议：
    - `SELECT`（业务库所有需要迁移的表）
    - `REPLICATION SLAVE` / `REPLICATION CLIENT`（用于读取 binlog）
  - 禁止使用 `root` 或拥有 DDL 权限的账号（除非有明确规则）。

- **PostgreSQL 目标库账号**：
  - 在目标 schema 上的：
    - `CREATE`（若希望工具自动建表）
    - `INSERT`、`UPDATE`、`DELETE`
  - 可考虑禁止 DROP 权限，避免误删。

- **Metadata PostgreSQL 账号**：
  - 仅对元数据库拥有读写权限。

### 2.3 网络与安全

- Kafka Connect 所在环境必须能访问：
  - 源 MySQL、目标 PostgreSQL、Kafka 集群
- metadata-service 必须能访问：
  - Metadata PostgreSQL、Kafka Connect REST API
- 建议：
  - 通过防火墙/安全组限制访问范围；
  - 密码不写死在配置文件中，使用环境变量或 Secret 管理。

### 2.4 容量与性能评估

在正式迁移前，需要对以下指标进行预估：

- 源库当前数据量 + 每日增量量级；
- CDC 事件生成速率（峰值），可先在测试环境模拟；
- Kafka 集群容量（Topic 分区数、磁盘空间、带宽）；
- Kafka Connect 集群实例数与每实例并发 Connector 数；
- 目标库写入能力（考虑索引、触发器、约束等开销）。

---

## 3. 部署与启动

### 3.1 Metadata Service 部署

1. 构建/获取可运行包：
   - `mvn clean package -pl metadata-service -am`
   - 获取 `metadata-service/target/*.jar`
2. 运行：
   ```bash
   java -jar metadata-service/target/metadata-service.jar \
     --spring.profiles.active=prod \
     --DB_HOST=<metadata_pg_host> \
     --DB_PORT=<port> \
     --DB_NAME=dbsyncer_metadata \
     --DB_USERNAME=dbsyncer \
     --DB_PASSWORD=****** \
     --CONNECT_REST_URL=http://<connect-host>:8083
   ```
3. 验证：
   - `curl http://<metadata-service-host>:8080/actuator/health`
   - `curl http://<metadata-service-host>:8080/api/tasks`（若已有任务）

### 3.2 CLI 工具准备

1. 构建 CLI：
   - `mvn clean package -pl cli -am`
   - 获取 `cli/target/*.jar` 或对应启动脚本。
2. 准备配置文件（可选）：`~/.dbsyncer.yml` 或项目内配置，例如：
   ```yaml
   api:
     baseUrl: http://metadata-service-prod:8080
   output:
     format: table
   ```

### 3.3 Kafka Connect 集群与 Connector 校验

1. 确保 Kafka Connect 集群正常：
   ```bash
   curl http://<connect-host>:8083/
   curl http://<connect-host>:8083/connector-plugins
   ```
2. 确认 Debezium Source Connector 与 JDBC Sink Connector 插件已安装。
3. 确认 Connect Worker 配置中：
   - `key.converter.schemas.enable`、`value.converter.schemas.enable` 配置符合项目要求；
   - 监控与日志配置符合现网规范。

---

## 4. 任务管理流程

### 4.1 创建迁移任务

1. 确认源/目标库已就绪，网络与权限无误。
2. 使用 CLI 创建任务（示例）：
   ```bash
   db-syncer task create \
     --name mysql-to-pg-order \
     --description "MySQL orders -> PG orders" \
     --source-type MYSQL \
     --source-host mysql-prod-ro \
     --source-port 3306 \
     --source-db order_db \
     --source-user sync_user \
     --source-pass '******' \
     --target-type POSTGRESQL \
     --target-host pg-migration \
     --target-port 5432 \
     --target-db order_db_mig \
     --target-user sync_user \
     --target-pass '******'
   ```
3. 查看任务列表与详情：
   ```bash
   db-syncer task list
   db-syncer task show <task-id-or-name>
   ```

### 4.2 启动任务

1. 启动：
   ```bash
   db-syncer task start <task-id-or-name>
   ```
2. CLI 将调用 metadata-service，后者负责：
   - 生成 Source/Sink Connector 配置；
   - 通过 Kafka Connect REST API 部署 Connector；
   - 更新任务状态为 `RUNNING`。
3. 观察状态：
   ```bash
   db-syncer task status <task-id-or-name>
   db-syncer monitor <task-id-or-name>
   ```
4. 若启动失败：
   - 检查 `metadata-service` 日志与 Kafka Connect 日志；
   - 查看任务状态与错误信息（未来可通过 `TaskLog` 查询）。

### 4.3 暂停 / 恢复任务

- 暂停：
  ```bash
  db-syncer task pause <task-id-or-name>
  ```
  - 暂停对应 Source/Sink Connector；
  - 任务状态更新为 `PAUSED`。

- 恢复：
  ```bash
  db-syncer task resume <task-id-or-name>
  ```
  - 恢复 Connector 运行；
  - 任务状态更新为 `RUNNING`。

> 建议：在计划内维护窗口前先 pause，再执行数据库维护，维护完成后 resume。

### 4.4 停止与删除任务

- 停止：
  ```bash
  db-syncer task stop <task-id-or-name>
  ```
  - 删除或停止 Connect 中的 Connector；
  - 任务状态更新为 `STOPPED`。

- 删除：
  ```bash
  db-syncer task delete <task-id-or-name>
  ```
  - 一般仅在确认不再需要该迁移任务时操作；
  - 注意保留元数据/日志以便后续审计和问题排查。

---

## 5. 监控与告警

> 以下为推荐实践，具体实现需结合 `metadata-service` 暴露的 metrics 与现有监控平台。

### 5.1 关键监控指标

建议至少监控以下指标（按任务维度）：

- 任务状态：
  - `dbsyncer_task_state{task="..."}`
- 已处理记录数：
  - `dbsyncer_task_processed_records_total{task="..."}`
- 失败记录数：
  - `dbsyncer_task_failed_records_total{task="..."}`
- 迁移延迟（lag）：
  - `dbsyncer_task_lag_seconds{task="..."}`
- 最后事件时间戳：
  - `dbsyncer_task_last_event_timestamp{task="..."}`

若还未完全实现上述指标，可临时使用：

- Kafka Connect 的 Connector/Task 状态；
- 源/目标库的表行数对比；
- 自定义日志提取的统计信息。

### 5.2 告警规则示例

基于 Prometheus（伪代码）：

- 任务长期失败：
  ```promql
  dbsyncer_task_state{state="FAILED"} == 1
  ```
  告警条件：持续 5 分钟。

- 延迟过高：
  ```promql
  dbsyncer_task_lag_seconds > 300
  ```

- 无事件流入（可能源库无变更或链路中断）：
  ```promql
  time() - dbsyncer_task_last_event_timestamp > 600
  ```

---

## 6. 常见故障与处理建议

### 6.1 任务启动失败

**现象：**
- `task start` 后任务状态为 `FAILED` 或长时间停留在 `STARTING`。

**排查：**
- 检查 metadata-service 日志（Connector 配置生成、REST 调用错误）；
- 检查 Kafka Connect 日志（插件缺失、配置非法、目标库连接失败）；
- 确认源/目标库网络连通与权限正确。

**处理：**
- 修正配置（如 URL、账号密码、表白名单等）后重新启动任务；
- 若 Connector 存在部分部署成功的残留，先在 Connect 上删除对应 Connector 再重试。

### 6.2 同步延迟高（lag 上升）

**可能原因：**
- 源库写入激增或批量历史数据导入；
- 目标库写入压力过大（索引过多、锁竞争）；
- Kafka Connect 实例资源不足（CPU、内存低）。

**处理建议：**
- 短期：
  - 检查目标库慢查询，必要时临时关闭部分非关键索引；
  - 增加 Kafka Connect Worker 实例数或提升资源规格；
  - 降低 connector 的 `max.batch.size` / `max.queue.size` 或调小并行度。
- 中期：
  - 对大表采用离线迁移 + CDC 补数据策略；
  - 调整任务和表的分批迁移计划。

### 6.3 目标库写入失败（约束冲突等）

**现象：**
- Sink Connector 报错，如主键冲突、唯一约束冲突、类型不兼容。

**排查：**
- 检查目标表 DDL 与源表定义是否一致；
- 检查是否有其他应用对目标库进行写入导致冲突；
- 查看具体失败 SQL 与数据内容。

**处理：**
- 对于一次性迁移场景，建议在迁移期间禁止业务直接向目标库写入；
- 短期可通过调整目标表约束、修正异常数据后重启任务；
- 必要时执行回滚策略（见下节）。

### 6.4 Schema 变更导致任务异常

**现象：**
- 源库执行 DDL（如新增列、修改类型）后，任务出现错误或停止。

**建议策略：**
- 对于首个 MVP 版本，**尽量在迁移窗口内冻结源库 Schema 变更**；
- 若必须变更：
  - 先在测试环境验证该 DDL 对迁移链路的影响；
  - 使用 `transform ddl/schema` 工具预览目标库 DDL 和类型映射；
  - 再在生产中按计划窗口执行。

---

## 7. 回滚与重试策略

> 目前项目尚未提供“一键回滚”能力，需要结合业务特性和 DBA 策略制定明确的回滚方案。

### 7.1 一次性迁移场景

典型流程：**全量迁移 + 增量追平 + 切流量**。

**推荐做法：**

1. 迁移前：
   - 在目标库创建全新的 schema 或数据库，不与现有线上表混用；
   - 确保迁移期间目标库仅由迁移任务写入。
2. 迁移中：
   - 若出现严重错误，可：
     - 停止任务；
     - 清理或重建目标库（truncate/drop schema）；
     - 修正配置或程序后重新开始迁移。
3. 切换流量：
   - 在源/目标数据比对一致后，将读/写流量切到目标库；
   - 保留源库作为一段时间的“只读回滚源”。

### 7.2 持续同步场景

若希望长期保持源→目标的数据同步，则回滚更复杂，一般建议：

- 明确业务主库（MySQL 或 PostgreSQL），避免“双写”；
- 出现严重错误时：
  - 立即停止任务，避免进一步污染目标库；
  - 在业务允许的情况下，以源库为准重建目标数据；
  - 如无法重建，需与业务侧评估手工修复策略。

---

## 8. 升级策略

### 8.1 升级 db-syncer 版本

1. 准备阶段：
   - 查看 Release Note，确认是否有元数据表结构变更（Flyway 脚本）；
   - 在测试环境验证升级流程。
2. 升级步骤（建议）：
   - 对 metadata PostgreSQL 做备份（逻辑备份或快照）；
   - 确认当前迁移任务状态（RUNNING / PAUSED / STOPPED）；
   - 对关键任务选择在低峰暂停；
   - 部署新版本 metadata-service 和 CLI；
   - 检查 Flyway 迁移是否成功；
   - 逐步恢复任务，观察一段时间。
3. 回滚：
   - 如升级后出现严重问题，可：
     - 停止新版本服务；
     - 恢复 metadata 数据库备份；
     - 启动旧版本服务；
     - 按照回滚策略处理已产生的目标库数据。

### 8.2 升级 Debezium / Kafka / Connector 版本

- 必须在测试环境中验证：
  - 同样任务配置在新版本下行为无差异；
  - 类型映射与 Schema 处理保持兼容。
- 建议：
  - 先升级非生产环境集群；
  - 再在生产中按集群/节点逐步升级；
  - 升级窗口内监控任务状态和 lag，必要时暂停新变更。

---

## 9. 建议的试点路线

1. **内部非核心系统试点**：
   - 选择一套数据规模中等、业务重要性较低的系统先行试点；
   - 重点验证：功能闭环、监控指标、回滚策略。
2. **灰度扩大范围**：
   - 在多个业务系统中扩大试点范围，持续收集问题和指标；
3. **正式纳入公司级迁移方案**：
   - 在满足 `docs/MVP_CRITERIA.md` 中的关键条目后，结合内部规范制定正式 SLO/SLA。

---

> 本文档为草稿版，随着功能完善与实际生产经验的积累，建议持续更新。所有运维决策应结合业务风险评估和 DBA 建议综合制定。 

