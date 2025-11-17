# 贡献指南

感谢您对 DB-Syncer-Debezium 项目的关注!我们欢迎所有形式的贡献。

## 如何贡献

### 报告 Bug

如果您发现了 bug,请创建一个 Issue 并包含以下信息:

- 清晰的标题和描述
- 复现步骤
- 期望行为
- 实际行为
- 环境信息 (Java 版本、操作系统等)
- 相关日志或错误信息

### 提出新功能

如果您有新功能建议:

1. 先检查 Issues 中是否已有类似建议
2. 创建新 Issue,标签为 `enhancement`
3. 详细描述功能需求和使用场景
4. 等待维护者反馈

### 提交代码

1. **Fork 仓库**

2. **创建特性分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **编写代码**
   - 遵循项目代码规范
   - 添加必要的单元测试
   - 更新相关文档

4. **提交更改**
   ```bash
   git commit -m "feat: add your feature description"
   ```

   提交信息格式:
   - `feat:` 新功能
   - `fix:` Bug 修复
   - `docs:` 文档更新
   - `test:` 测试相关
   - `refactor:` 代码重构
   - `chore:` 构建/工具相关

5. **推送到 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **创建 Pull Request**
   - 描述清楚变更内容
   - 关联相关 Issue
   - 等待 Code Review

## 代码规范

### Java 代码风格

- 使用 4 个空格缩进
- 类名使用大驼峰 (PascalCase)
- 方法和变量使用小驼峰 (camelCase)
- 常量使用全大写下划线分隔 (UPPER_SNAKE_CASE)
- 每行代码不超过 120 字符

### 注释规范

```java
/**
 * 任务管理服务
 *
 * 负责管理迁移任务的生命周期,包括创建、启动、停止、删除等操作。
 *
 * @author Your Name
 * @since 1.0.0
 */
public class TaskService {

    /**
     * 创建新的迁移任务
     *
     * @param request 任务创建请求
     * @return 创建的任务 ID
     * @throws TaskCreationException 任务创建失败时抛出
     */
    public String createTask(TaskCreateRequest request) {
        // 实现...
    }
}
```

### 测试要求

- 所有新功能必须包含单元测试
- 测试覆盖率目标: 80%+
- 使用 JUnit 5 和 Mockito
- 测试类命名: `ClassNameTest`

示例:
```java
@Test
void testCreateTask_Success() {
    // Given
    TaskCreateRequest request = new TaskCreateRequest();
    request.setName("test-task");

    // When
    String taskId = taskService.createTask(request);

    // Then
    assertNotNull(taskId);
    assertTrue(taskId.matches("[0-9a-f-]+"));
}
```

## 开发环境设置

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- IDE (推荐 IntelliJ IDEA 或 Eclipse)

### 环境搭建

1. 克隆项目
   ```bash
   git clone https://github.com/your-username/db-syncer-debezium.git
   cd db-syncer-debezium
   ```

2. 启动依赖服务
   ```bash
   cd docker
   docker-compose up -d
   ```

3. 导入 IDE
   - IntelliJ IDEA: File → Open → 选择项目根目录
   - Eclipse: File → Import → Maven → Existing Maven Projects

4. 运行测试
   ```bash
   mvn test
   ```

5. 编译打包
   ```bash
   mvn clean package
   ```

## 分支策略

- `main`: 主分支,保持稳定
- `develop`: 开发分支,包含最新功能
- `feature/*`: 功能分支
- `bugfix/*`: Bug 修复分支
- `release/*`: 发布分支

## Pull Request 审核流程

1. 自动化检查 (CI)
   - 代码编译
   - 单元测试
   - 代码风格检查
   - 测试覆盖率

2. Code Review
   - 至少一位维护者审核
   - 解决所有评论

3. 合并
   - Squash and Merge (保持提交历史整洁)
   - 删除特性分支

## 发布流程

1. 更新版本号 (pom.xml)
2. 更新 CHANGELOG.md
3. 创建 Release 分支
4. 测试验证
5. 合并到 main
6. 打标签 (v1.0.0)
7. 发布到 Maven Central (如适用)

## 许可证

贡献的代码将使用 Apache License 2.0 许可证。

## 行为准则

- 尊重所有贡献者
- 建设性的讨论
- 专注于技术问题
- 欢迎新手贡献

## 联系方式

- GitHub Issues: 技术问题和 Bug 报告
- Discussions: 一般性讨论和问答

感谢您的贡献!
