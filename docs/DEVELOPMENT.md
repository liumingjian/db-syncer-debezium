# DB-Syncer-Debezium 开发指南

> 版本: 1.0.0-SNAPSHOT
> 最后更新: 2025-11-17

## 目录

- [开发环境设置](#开发环境设置)
- [项目结构](#项目结构)
- [构建和测试](#构建和测试)
- [开发工作流](#开发工作流)
- [代码规范](#代码规范)
- [调试指南](#调试指南)
- [常见问题](#常见问题)

## 开发环境设置

### 前置要求

确保你的开发环境满足以下要求:

| 工具 | 最低版本 | 推荐版本 | 检查命令 |
|-----|---------|---------|---------|
| JDK | 17 | 17 或 21 | `java -version` |
| Maven | 3.8 | 3.9+ | `mvn -version` |
| Docker | 20.10 | Latest | `docker --version` |
| Docker Compose | 2.0 | Latest | `docker compose version` |
| Git | 2.30 | Latest | `git --version` |

### IDE 推荐配置

#### IntelliJ IDEA (推荐)

1. **安装插件**:
   - Lombok Plugin
   - Checkstyle-IDEA
   - SonarLint
   - Save Actions

2. **导入项目**:
   ```bash
   File → Open → 选择项目根目录的 pom.xml
   ```

3. **配置代码格式化**:
   - 导入 `eclipse-formatter.xml` (如果创建的话)
   - 或使用 IntelliJ 默认的 Google Java Style

4. **启用 Lombok**:
   ```
   Preferences → Build, Execution, Deployment → Compiler → Annotation Processors
   勾选 "Enable annotation processing"
   ```

5. **配置 Checkstyle**:
   ```
   Preferences → Tools → Checkstyle
   添加配置文件: checkstyle.xml
   ```

#### VS Code

1. **安装扩展**:
   - Extension Pack for Java
   - Lombok Annotations Support
   - Checkstyle for Java
   - Docker

2. **配置 settings.json**:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic",
     "editor.formatOnSave": true
   }
   ```

### 克隆仓库

```bash
git clone https://github.com/liumingjian/db-syncer-debezium.git
cd db-syncer-debezium
```

### 启动开发环境

#### 1. 启动 Docker 服务

```bash
cd docker
docker compose up -d
```

等待所有服务启动完成:

```bash
# 检查服务状态
docker compose ps

# 查看服务日志
docker compose logs -f
```

#### 2. 验证服务可用性

```bash
# 检查 Kafka Connect
curl http://localhost:8083/

# 检查 PostgreSQL 元数据库
docker compose exec postgres psql -U dbsyncer -d dbsyncer_metadata -c '\dt'

# 检查 Grafana
open http://localhost:3005
```

服务端口列表:

| 服务 | 端口 | 用途 |
|-----|------|------|
| Kafka | 9092 | Kafka Broker |
| Kafka Connect | 8083 | Kafka Connect REST API |
| PostgreSQL (Metadata) | 5432 | 元数据存储 |
| MySQL (Test Source) | 3306 | 测试源数据库 |
| PostgreSQL (Test Target) | 5433 | 测试目标数据库 |
| Prometheus | 9090 | 监控指标 |
| Grafana | 3005 | 监控面板 |

### 构建项目

```bash
# 清理并编译
mvn clean compile

# 跳过测试快速构建
mvn clean install -DskipTests

# 完整构建(包含测试)
mvn clean install
```

## 项目结构

```
db-syncer-debezium/
├── .github/
│   ├── workflows/           # GitHub Actions CI/CD
│   │   └── build.yml
│   └── ISSUE_TEMPLATE/      # Issue 模板
│
├── common/                  # 公共模块
│   └── src/
│       ├── main/java/com/dbsyncer/common/
│       │   ├── model/       # 数据模型
│       │   ├── exception/   # 异常类
│       │   └── utils/       # 工具类
│       └── test/java/
│
├── metadata-service/        # 元数据管理服务
│   └── src/
│       └── main/
│           ├── java/com/dbsyncer/metadata/
│           │   ├── entity/      # JPA 实体
│           │   ├── repository/  # Repository 层
│           │   ├── service/     # Service 层
│           │   ├── controller/  # REST Controller
│           │   ├── dto/         # 数据传输对象
│           │   └── config/      # 配置类
│           └── resources/
│               ├── application.yml
│               ├── application-dev.yml
│               ├── application-prod.yml
│               └── db/migration/  # Flyway 脚本
│
├── cli/                     # 命令行工具
│   └── src/
│       └── main/java/com/dbsyncer/cli/
│           ├── command/     # Picocli 命令
│           ├── client/      # API 客户端
│           ├── formatter/   # 输出格式化
│           └── DbSyncerCli.java  # 主入口
│
├── connectors/              # Connector 扩展
│   └── src/
│       └── main/java/com/dbsyncer/connectors/
│           ├── offset/      # Offset 存储
│           ├── history/     # Schema History
│           └── config/      # 配置生成器
│
├── transformations/         # 数据转换层
│   └── src/
│       └── main/java/com/dbsyncer/transformations/
│           ├── smt/         # SMT 实现
│           ├── mapper/      # 类型映射
│           └── schema/      # Schema 转换
│
├── monitoring/              # 监控模块
│   └── src/
│       └── main/java/com/dbsyncer/monitoring/
│           ├── metrics/     # 指标收集
│           └── health/      # 健康检查
│
├── docker/                  # Docker 环境
│   ├── docker-compose.yml
│   └── config/
│
├── docs/                    # 文档
│   ├── ARCHITECTURE.md      # 架构文档
│   ├── DEVELOPMENT.md       # 开发指南
│   ├── ISSUES_PLAN.md       # 开发计划
│   └── PROJECT_STATUS.md    # 项目状态
│
├── scripts/                 # 脚本
│   └── create-issues.sh     # Issue 创建脚本
│
├── checkstyle.xml           # Checkstyle 配置
├── checkstyle-suppressions.xml
├── .editorconfig            # EditorConfig
├── .gitignore
├── pom.xml                  # 根 POM
├── README.md
├── CONTRIBUTING.md
└── LICENSE
```

## 构建和测试

### Maven 命令

```bash
# 编译所有模块
mvn compile

# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 跳过单元测试运行集成测试
mvn verify -DskipUnitTests

# 生成测试覆盖率报告
mvn jacoco:report

# 运行代码质量检查
mvn checkstyle:check
mvn spotbugs:check

# 格式化代码
mvn formatter:format

# 安全漏洞检查
mvn dependency-check:check

# 打包
mvn package

# 安装到本地仓库
mvn install

# 清理
mvn clean
```

### 单独构建模块

```bash
# 只构建 common 模块
mvn -pl common clean install

# 构建 metadata-service 及其依赖
mvn -pl metadata-service -am clean install

# 构建 CLI 工具
mvn -pl cli -am clean package
```

### 运行应用

#### Metadata Service

```bash
cd metadata-service
mvn spring-boot:run

# 或指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### CLI Tool

```bash
cd cli
mvn spring-boot:run -Dspring-boot.run.arguments="--help"

# 或打包后运行
mvn package
java -jar target/db-syncer-cli.jar --help
```

## 开发工作流

### 1. 选择 Issue

从 [GitHub Issues](https://github.com/liumingjian/db-syncer-debezium/issues) 选择一个任务。

### 2. 创建分支

```bash
# Feature 分支
git checkout -b feature/issue-<number>-<brief-description>

# Bug 修复分支
git checkout -b bugfix/issue-<number>-<brief-description>

# 示例
git checkout -b feature/issue-7-metadata-schema
```

### 3. 开发

1. **编写代码**:
   - 遵循代码规范
   - 添加必要的注释
   - 保持方法简洁 (< 150 行)

2. **编写测试**:
   - 单元测试覆盖率 > 80%
   - 为关键逻辑编写集成测试

3. **本地测试**:
   ```bash
   # 运行受影响模块的测试
   mvn test -pl <module-name>

   # 运行所有测试
   mvn clean verify
   ```

### 4. 提交代码

```bash
# 添加文件
git add .

# 提交 (使用规范的 commit message)
git commit -m "feat: add metadata schema design

- Create migration_tasks table
- Create table_progress table
- Add Flyway migration script

Closes #7"
```

**Commit Message 规范**:

```
<type>: <subject>

<body>

<footer>
```

Type 类型:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具配置

### 5. 推送并创建 PR

```bash
# 推送到远程
git push origin feature/issue-7-metadata-schema

# 在 GitHub 创建 Pull Request
```

**PR 标题格式**:

```
[#<issue-number>] <Brief description>

示例: [#7] Add metadata database schema design
```

**PR 描述模板**:

```markdown
## 概述
简要描述此 PR 的目的

## 变更内容
- 变更点 1
- 变更点 2

## 测试
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试场景

## 截图 (如适用)

## 关联 Issue
Closes #<issue-number>
```

### 6. Code Review

- 等待 CI 检查通过
- 响应 review 意见
- 根据反馈修改代码

### 7. 合并

PR 被批准后,由 maintainer 合并到 main 分支。

## 代码规范

### Java 代码规范

项目遵循 **Google Java Style Guide** 的修改版本。

#### 命名规范

```java
// 类名: PascalCase
public class MigrationTaskService { }

// 接口名: PascalCase
public interface TypeMapper { }

// 方法名: camelCase
public void createMigrationTask() { }

// 常量: UPPER_SNAKE_CASE
public static final String DEFAULT_CONNECTOR_TYPE = "mysql";

// 变量: camelCase
private String taskName;

// 包名: lowercase
package com.dbsyncer.metadata.service;
```

#### 代码格式

```java
// 缩进: 4 spaces
public class Example {
    private String field;

    // 方法之间空一行
    public void method1() {
        if (condition) {
            // 代码块
        }
    }

    public void method2() {
        // ...
    }
}

// 行长度: <= 120 字符

// 花括号风格: K&R
if (condition) {
    // ...
} else {
    // ...
}
```

#### 注释规范

```java
/**
 * 服务类的 JavaDoc
 *
 * <p>详细描述...
 *
 * @author Your Name
 * @since 1.0.0
 */
public class TaskService {

    /**
     * 创建迁移任务
     *
     * @param request 任务创建请求
     * @return 创建的任务
     * @throws ValidationException 如果参数验证失败
     */
    public MigrationTask createTask(TaskCreateRequest request) {
        // 实现
    }

    // 复杂逻辑的行内注释
    private void complexLogic() {
        // Step 1: Validate input
        validateInput();

        // Step 2: Process data
        processData();
    }
}
```

#### 异常处理

```java
// 好的实践
try {
    performOperation();
} catch (SpecificException e) {
    log.error("Failed to perform operation: {}", e.getMessage(), e);
    throw new ServiceException("Operation failed", e);
}

// 避免空 catch 块
try {
    performOperation();
} catch (Exception e) {
    // 至少记录日志
    log.warn("Ignoring exception: {}", e.getMessage());
}
```

### 测试规范

#### 单元测试

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("Should create task successfully")
    void shouldCreateTaskSuccessfully() {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskName("test-task");

        MigrationTask expectedTask = new MigrationTask();
        when(taskRepository.save(any())).thenReturn(expectedTask);

        // When
        MigrationTask actualTask = taskService.createTask(request);

        // Then
        assertThat(actualTask).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception when task name is empty")
    void shouldThrowExceptionWhenTaskNameIsEmpty() {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskName("");

        // When & Then
        assertThatThrownBy(() -> taskService.createTask(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Task name cannot be empty");
    }
}
```

#### 集成测试

```java
@SpringBootTest
@Testcontainers
class TaskControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
        .withDatabaseName("testdb");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateTaskViaApi() throws Exception {
        String requestBody = """
            {
              "taskName": "test-task",
              "sourceType": "mysql",
              "targetType": "postgresql"
            }
            """;

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskName").value("test-task"));
    }
}
```

### SQL 规范

```sql
-- 表名: snake_case
CREATE TABLE migration_tasks (
    task_id UUID PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引命名: idx_<table>_<column>
CREATE INDEX idx_migration_tasks_status ON migration_tasks(status);

-- 外键命名: fk_<table>_<ref_table>
ALTER TABLE table_progress
ADD CONSTRAINT fk_table_progress_migration_tasks
FOREIGN KEY (task_id) REFERENCES migration_tasks(task_id);
```

## 调试指南

### 调试 Metadata Service

IntelliJ IDEA:

1. 右键 `MetadataServiceApplication.java`
2. 选择 "Debug 'MetadataServiceApplication'"

或配置远程调试:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

然后在 IDE 中连接到 `localhost:5005`

### 调试 Kafka Connect

查看 Connector 日志:

```bash
docker compose logs -f kafka-connect
```

调试 Connector 配置:

```bash
# 获取 Connector 状态
curl http://localhost:8083/connectors/<connector-name>/status

# 获取 Connector 配置
curl http://localhost:8083/connectors/<connector-name>/config

# 重启 Connector
curl -X POST http://localhost:8083/connectors/<connector-name>/restart
```

### 查看 Kafka 消息

```bash
# 进入 Kafka 容器
docker compose exec kafka bash

# 列出所有 topic
kafka-topics --bootstrap-server localhost:9092 --list

# 查看 topic 消息
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic <topic-name> \
  --from-beginning
```

### 数据库调试

PostgreSQL (元数据库):

```bash
docker compose exec postgres psql -U dbsyncer -d dbsyncer_metadata

# 查看表
\dt

# 查询数据
SELECT * FROM migration_tasks;
```

MySQL (测试源):

```bash
docker compose exec mysql mysql -u root -pmysql_root_pass

USE sourcedb;
SHOW TABLES;
```

## 常见问题

### 构建问题

**Q: Maven 构建失败,依赖下载超时**

A: 配置国内镜像源 (`~/.m2/settings.xml`):

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

**Q: Lombok 注解不生效**

A: 确保 IDE 安装了 Lombok 插件并启用注解处理。

### Docker 问题

**Q: Docker 容器启动失败**

A: 检查端口占用:

```bash
# macOS/Linux
lsof -i :8083

# Windows
netstat -ano | findstr :8083
```

**Q: Kafka Connect 无法连接到 Kafka**

A: 检查 `docker-compose.yml` 中的网络配置和 `KAFKA_ADVERTISED_LISTENERS`。

### 测试问题

**Q: 集成测试失败,Testcontainers 无法启动**

A: 确保 Docker daemon 正在运行:

```bash
docker ps
```

**Q: 测试覆盖率不足**

A: 运行以下命令查看详细报告:

```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

## 有用的资源

### 官方文档

- [Debezium Documentation](https://debezium.io/documentation/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Picocli](https://picocli.info/)

### 教程和示例

- [Debezium Tutorial](https://debezium.io/documentation/reference/stable/tutorial.html)
- [Kafka Connect Deep Dive](https://kafka.apache.org/documentation/#connect)

### 工具

- [Kafka Connect UI](https://github.com/lensesio/kafka-connect-ui)
- [Debezium UI](https://github.com/debezium/debezium-ui)
- [Conduktor](https://www.conduktor.io/) - Kafka GUI

## 获取帮助

遇到问题?

1. 查看 [GitHub Issues](https://github.com/liumingjian/db-syncer-debezium/issues)
2. 提交新的 Issue
3. 查阅项目文档

欢迎贡献!详见 [CONTRIBUTING.md](../CONTRIBUTING.md)
