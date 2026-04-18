# JeecgBoot Flowable 工作流 · 使用说明书

> **文档版本**：v1.0
> **适用版本**：jeecg-module-flowable 3.9.1 / Flowable 7.1.0 / JeecgBoot 3.9.1 / Spring Boot 3.5.5
> **编写日期**：2026-04-17
> **受众**：系统管理员、业务配置人员、二次开发工程师

---

## 目录

1. [产品概述](#1-产品概述)
2. [系统架构](#2-系统架构)
3. [环境准备与部署](#3-环境准备与部署)
4. [菜单与权限配置](#4-菜单与权限配置)
5. [用户操作手册](#5-用户操作手册)
6. [管理员配置指南](#6-管理员配置指南)
7. [开发者集成手册](#7-开发者集成手册)
8. [API 参考](#8-api-参考)
9. [运维与故障排查](#9-运维与故障排查)
10. [术语表](#10-术语表)

---

## 1. 产品概述

### 1.1 功能边界

`jeecg-module-flowable` 在 JeecgBoot 3.9.1 上零侵入集成了 Flowable 7.1.0，提供：

| 能力 | 说明 |
|---|---|
| **可视化设计** | 基于 bpmn-js 的 BPMN 2.0 流程设计器，支持节点属性面板 |
| **模型管理** | 流程模型 CRUD，草稿态保存 BPMN XML |
| **部署与版本** | 一键部署生成流程定义，自动递增版本号；可挂起/激活/删除 |
| **运行时** | 发起流程、作废实例、查询"我发起的" |
| **任务中心** | 待办、已办、签收、审批、驳回、委派、加签 |
| **历史追踪** | 审批轨迹、带高亮节点的流程图 PNG |
| **多租户** | 按 `X-Tenant-Id` 隔离模型、定义、实例、任务 |
| **身份同步** | 启动/定时把 `sys_user`、`sys_role`、`sys_depart` 同步到 `ACT_ID_*` |
| **通知扩展** | `NotifyStrategy` 策略接口，默认站内信，可扩展钉钉/企微/邮件 |

### 1.2 与同类方案对比

| 维度 | jeecg-module-flowable | flowable-ui | Activiti |
|---|---|---|---|
| 认证体系 | ✅ 复用 Shiro + JWT | ❌ 自带账号 | ❌ 自带账号 |
| 多租户 | ✅ 内置 | ⚠️ 手搓 | ⚠️ 手搓 |
| 设计器 | ✅ bpmn-js 内嵌 | ✅ 自带 | ❌ 需自研 |
| JeecgBoot 菜单 | ✅ SQL 一键导入 | ❌ | ❌ |
| 代码量 | 轻量（~30 类） | 重量 | 重量 |

---

## 2. 系统架构

### 2.1 模块结构

```
jeecg-boot-module/jeecg-module-flowable/
└── src/main/java/org/jeecg/modules/flowable/
    ├── common/         # 常量、异常
    ├── config/         # FlowableEngineConfig（applicationTaskExecutor + 字体）
    ├── definition/     # 流程定义 Controller/Service
    ├── history/        # 历史查询 Controller/Service
    ├── identity/       # 身份同步 Runner/Service
    ├── listener/       # BPMN 执行监听
    ├── model/          # 模型 Controller/Service/VO
    ├── notify/         # 通知策略（Dispatcher + Strategy 接口）
    ├── runtime/        # 运行时 Controller/Service
    ├── task/           # 任务 Controller/Service/DTO
    └── tenant/         # JeecgTenantInfoHolder
```

### 2.2 数据存储

- **业务库**（共用 `jeecgai` 库）：61 张 `ACT_*` 表，首启自动创建（`flowable.database-schema-update=true`）。
- **菜单表**：`sys_permission` 下新增 `/flowable/*` 菜单（Flyway 脚本 `V3.9.1_2__flowable_menu.sql`）。
- **无新建表**：整个模块不引入任何业务表，所有业务字段通过 BPMN `variables` + `businessKey` 关联。

### 2.3 请求链路

```
浏览器 → Nginx → Shiro(JwtFilter) → FlowableXxxController
                                       ↓
                                  FlowableXxxServiceImpl
                                       ↓
                    ┌──────────────────┼──────────────────┐
                    ↓                  ↓                  ↓
             RepositoryService  RuntimeService     TaskService
                 (模型/定义)      (实例)              (任务)
                    ↓                  ↓                  ↓
                    └─────────→ MySQL ACT_* ←─────────────┘
```

### 2.4 关键依赖

| 依赖 | 版本 | 作用 |
|---|---|---|
| flowable-spring-boot-starter-process | 7.1.0 | 引擎核心 |
| flowable-image-generator | 7.1.0 | 流程图 PNG 渲染 |
| flowable-bpmn-converter | 7.1.0 | BPMN XML ↔ Model 转换 |
| flowable-bpmn-model | 7.1.0 | BPMN 对象模型 |
| bpmn-js (前端) | ≥ 11 | 设计器 |
| bpmn-js-properties-panel | ≥ 4 | 属性面板 |

---

## 3. 环境准备与部署

### 3.1 软硬件要求

| 项 | 要求 |
|---|---|
| JDK | 17 / 21 / 24 |
| MySQL | 8.0+（InnoDB, utf8mb4） |
| Redis | 6+ |
| 浏览器 | Chrome 110+ / Edge 110+ |
| 后端内存 | ≥ 2 GB |

### 3.2 后端部署

1. 确认父 `pom.xml` 已锁定版本：
   ```xml
   <flowable.version>7.1.0</flowable.version>
   ```
2. `jeecg-system-start/pom.xml` 已引入 `jeecg-module-flowable`，无需再改。
3. 首次启动：让 Flowable 自建表：
   ```yaml
   flowable:
     database-schema-update: true   # 开发期
   ```
4. 生产冻结：DDL 导出后纳入 Flyway，然后改为：
   ```yaml
   flowable:
     database-schema-update: false
   ```
5. 启动：
   ```bash
   cd jeecg-module-system/jeecg-system-start
   mvn spring-boot:run
   ```

### 3.3 前端部署

```bash
cd jeecgboot-vue3
pnpm install       # 首次
pnpm dev           # 开发模式，localhost:3100
pnpm build         # 生产构建 → dist/
```

`package.json` 已含 `bpmn-js`、`bpmn-js-properties-panel`、`camunda-bpmn-moddle`。

### 3.4 必备中间件

**Redis**（必须）：
```bash
docker run -d --name redis -p 6379:6379 redis:7 redis-server --requirepass 123456
```

**MySQL**（示例）：
```sql
CREATE DATABASE jeecgai DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 3.5 关键配置（`application-dev.yml`）

```yaml
flowable:
  database-schema-update: true    # 开发 true，生产 false
  history-level: full             # none / activity / audit / full
  async-executor-activate: true   # 异步任务/定时边界事件必开
  identity:
    sync-on-startup: true         # 启动时全量同步 sys_user/role/depart

spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
```

---

## 4. 菜单与权限配置

### 4.1 导入菜单

**Flyway 启用时**自动执行：
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:flyway/sql/mysql
```

**手动执行**：MySQL 导入
```
jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.1_2__flowable_menu.sql
```

### 4.2 菜单结构

| 路径 | 组件 | 说明 |
|---|---|---|
| `/flowable` | `layouts/RouteView` | 一级父菜单 |
| `/flowable/model` | `flowable/model/index` | 模型管理 |
| `/flowable/modeler` | `flowable/modeler/index` | 设计器（隐藏，由模型列表跳转） |
| `/flowable/definition` | `flowable/definition/index` | 已部署流程 |
| `/flowable/process/my` | `flowable/process/my` | 我发起的 |
| `/flowable/process/start` | `flowable/process/start` | 发起流程（隐藏） |
| `/flowable/task/todo` | `flowable/task/todo` | 我的待办 |
| `/flowable/task/done` | `flowable/task/done` | 我的已办 |
| `/flowable/history/trace` | `flowable/history/trace` | 流转追踪（隐藏） |

### 4.3 按钮权限分配

登录管理员 → **系统管理 → 角色管理** → 选择目标角色 → 勾选菜单：

| 操作 | perms |
|---|---|
| 模型创建/编辑/删除/部署 | `flowable:model:create`/`:update`/`:delete`/`:deploy` |
| 定义挂起激活/删除 | `flowable:definition:toggle`/`:delete` |
| 任务审批/驳回/委派 | `flowable:task:approve`/`:reject`/`:delegate` |

分配后**被授权用户需重新登录**以刷新权限缓存。

---

## 5. 用户操作手册

### 5.1 流程设计器（业务分析员）

**入口**：工作流管理 → 模型管理 → 新建/编辑

**步骤**：
1. 点击 **新建**，填：
   - **key**（必填）：英文唯一标识，如 `leave_request`
   - **name**：中文名，如 `请假审批`
   - **category**（可选）：分类，如 `HR`
   - **description**：说明
2. 进入设计器，拖拽：开始 → 用户任务 → 网关 → 用户任务 → 结束
3. 选中节点 → 右侧属性面板配置：
   - **userTask 节点**：见 [6.1 节点授权](#61-节点授权类型) / [6.2 表单方式](#62-表单集成方式)
   - **exclusiveGateway**：条件流写 `${amount > 1000}`
4. 点击 **保存**（写入草稿，未部署不影响运行）
5. 点击 **部署** → 系统自动生成流程定义，**开始可被发起**

> 💡 **小贴士**：保存 ≠ 部署。保存只是草稿，必须部署后才生效；每次部署会递增版本号，旧版本实例继续按旧版本执行。

### 5.2 发起流程（业务用户）

**入口**：已部署流程 → 选择流程 → 点击 **发起**

**参数**：
- **businessKey**：业务单号，如 `LR20260417-001`
- **businessTitle**：单据标题，如"张三的请假单"
- **variables**：业务字段，如 `{"amount": 3, "reason": "年假"}`

提交后系统自动找到第一个 userTask 节点并按规则分配办理人。

### 5.3 我的待办（办理人）

**入口**：我的待办

| 操作按钮 | 行为 |
|---|---|
| **签收** | 候选人组中一人抢占任务，其他人待办列表移除 |
| **通过** | 任务流转到下一节点；可填 `comment` 与 `variables` |
| **驳回** | 当前实现为**直接结束流程**（见 FlowableTaskServiceImpl.java:131） |
| **委派** | 把任务交给 `targetUser`，完成后会回到委派发起人 |
| **加签** | 在当前任务追加候选人，不改变流转 |

### 5.4 查看历史

**入口**：我发起的 → 点击单号 → **流转追踪**

会展示：
- 每个节点的**办理人**、**开始时间**、**结束时间**、**耗时**
- **审批意见**（comment）
- **带高亮的流程图**：已完成节点填充绿色，当前节点描红

---

## 6. 管理员配置指南

### 6.1 节点授权类型

在 `userTask` 的**扩展属性**里设 `assigneeType`，共 7 种：

| 类型 | 含义 | BPMN 示例 |
|---|---|---|
| `fixed` | 固定单人 | `flowable:assignee="zhangsan"` |
| `users` | 指定多个候选人 | `flowable:candidateUsers="zhangsan,lisi"` |
| `roles` | 按角色 | `flowable:candidateGroups="role_hr,role_finance"` |
| `depts` | 按部门 | `flowable:candidateGroups="DEPT_A01A03"` |
| `starter` | 发起人本人 | `flowable:assignee="${INITIATOR}"` |
| `leader` | 发起人直属上级 | `flowable:assignee="${getLeader(INITIATOR)}"` |
| `deptLeader` | 发起人部门负责人 | `flowable:assignee="${getDeptLeader(INITIATOR)}"` |

> `leader` / `deptLeader` 依赖 Spring EL 函数，需自行实现。

### 6.2 表单集成方式

`userTask` 扩展属性 `formType` 三选一：

| formType | 说明 | 填充字段 |
|---|---|---|
| `online` | Jeecg online 表单 | `formKey` = online 表单 ID |
| `route` | 前端自定义路由 | `formValue` = 路由路径，如 `/leave/approval/:id` |
| `none` | 纯审批按钮 | — |

### 6.3 通知策略

**默认**：站内信（`SiteNotifyStrategy`，写入 `sys_announcement` + `sys_announcement_send`）。

**扩展**：新建 Bean 实现 `NotifyStrategy`：

```java
@Component
public class DingTalkNotifyStrategy implements NotifyStrategy {
    @Override
    public String getType() { return "DINGTALK"; }

    @Override
    public void send(NotifyContext ctx) {
        // 调用钉钉 OpenAPI
    }
}
```

`NotifyDispatcher` 会在任务分配时并行调所有启用的策略。

### 6.4 身份同步

默认配置：
```yaml
flowable:
  identity:
    sync-on-startup: true
```

启动时 `JeecgIdentityInitRunner` 全量同步 `sys_user` → `ACT_ID_USER`、`sys_role` → `ACT_ID_GROUP`(type=role)、`sys_depart` → `ACT_ID_GROUP`(type=dept 且 id 加 `DEPT_` 前缀)。

**手动触发**（管理员）：
```http
POST /flowable/model/identity/sync
X-Access-Token: <admin token>
```

---

## 7. 开发者集成手册

### 7.1 与业务表对接

Flowable **不持有业务数据**，通过两个字段关联：

- **businessKey**：业务表主键或单据号，发起时写入流程实例
- **variables**：业务字段（如 `amount`、`dept`），作为网关条件 / 通知模板变量

**反查业务数据**：

```java
ProcessInstance pi = runtimeService.createProcessInstanceQuery()
    .processInstanceId(iid).singleResult();
String bizId = pi.getBusinessKey();   // 用它去业务表查
```

### 7.2 审批完成回写业务状态

推荐在 BPMN **结束事件**上挂监听：

```xml
<endEvent id="end">
  <extensionElements>
    <flowable:executionListener event="end" delegateExpression="${bizFinishListener}"/>
  </extensionElements>
</endEvent>
```

```java
@Component("bizFinishListener")
public class BizFinishListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) {
        String bizId = execution.getProcessInstanceBusinessKey();
        leaveRequestService.updateStatus(bizId, "APPROVED");
    }
}
```

### 7.3 租户上下文传递

**HTTP 请求**：前端附带请求头 `X-Tenant-Id: 1000`，Shiro 通过后由 `JeecgTenantInfoHolder` 读取。

**异步线程 / 定时任务**：手动透传：

```java
try {
    tenantHolder.setTenantId("1000");
    runtimeService.startProcessInstanceByKey("leave_request");
} finally {
    tenantHolder.clear();
}
```

### 7.4 扩展点一览

| 扩展点 | 接口 / Bean 名 | 用途 |
|---|---|---|
| 通知策略 | `NotifyStrategy` | 对接钉钉/企微/短信/邮件 |
| BPMN 执行监听 | `ExecutionListener` / `TaskListener` | 节点进入离开时回写业务/触发额外逻辑 |
| 引擎配置 | `EngineConfigurationConfigurer<SpringProcessEngineConfiguration>` | 修改历史级别、字体、拦截器等 |
| Spring EL 函数 | 在 Bean 上加 `@Component` 即自动可用 | `${getLeader(INITIATOR)}` 等 |

### 7.5 常用 SDK 代码片段

**启动流程**：
```java
ProcessInstance pi = runtimeService.startProcessInstanceByKey(
    "leave_request",
    "LR20260417-001",                       // businessKey
    Map.of(
        "INITIATOR", "zhangsan",
        "amount", 3,
        "businessTitle", "张三请假"
    )
);
```

**查询某用户待办**：
```java
List<Task> todos = taskService.createTaskQuery()
    .taskTenantId("1000")
    .or().taskAssignee("zhangsan").taskCandidateUser("zhangsan").endOr()
    .active()
    .list();
```

**审批通过**：
```java
taskService.setAssignee(taskId, "zhangsan");
taskService.addComment(taskId, piId, "pass", "同意");
taskService.complete(taskId, Map.of("approved", true));
```

---

## 8. API 参考

所有接口：
- **前缀**：`/jeecg-boot`（服务上下文）+ 路径
- **认证**：请求头 `X-Access-Token: <jwt>`
- **租户**：请求头 `X-Tenant-Id: <tenantId>`（不传默认 `0`）
- **返回体**：`{ code, success, message, result, timestamp }`

### 8.1 模型管理 `/flowable/model`

| Method | Path | 参数 | 说明 |
|---|---|---|---|
| POST | `/create` | Body: `{key,name,category,description}` | 创建空白模型，返回 modelId |
| PUT | `/update/{id}` | Body: `{name,category,description,bpmnXml,svg}` | 保存 BPMN（草稿） |
| GET | `/detail/{id}` | — | 返回 `ModelVO` |
| GET | `/xml/{id}` | — | 返回 BPMN XML 字符串 |
| GET | `/list` | Query: `keyword,pageNo,pageSize` | 分页 |
| DELETE | `/delete/{id}` | — | 仅删草稿，不影响已部署 |
| POST | `/deploy/{id}` | — | 部署，返回 deploymentId |
| POST | `/identity/sync` | — | 手动触发身份同步（管理员） |

### 8.2 流程定义 `/flowable/definition`

| Method | Path | 说明 |
|---|---|---|
| GET | `/list` | 分页查询已部署流程 |
| PUT | `/suspend/{id}` | 挂起：不可发起新实例 |
| PUT | `/activate/{id}` | 激活 |
| DELETE | `/delete/{deploymentId}?cascade=true` | 级联删除部署 + 相关实例/历史 |
| GET | `/xml/{id}` | 部署后的 BPMN XML |
| GET | `/diagram/{id}` | 流程图 PNG（二进制流） |

### 8.3 运行时 `/flowable/process`

| Method | Path | 参数 | 说明 |
|---|---|---|---|
| POST | `/start` | `{processDefinitionKey,businessKey,businessTitle,variables}` | 发起实例，返回 instanceId |
| GET | `/my` | `pageNo,pageSize` | 我发起的 |
| POST | `/cancel/{instanceId}?reason=xxx` | — | 作废 |

### 8.4 任务中心 `/flowable/task`

| Method | Path | Body | 说明 |
|---|---|---|---|
| GET | `/todo` | — | 我的待办 |
| GET | `/done` | — | 我的已办 |
| POST | `/claim/{taskId}` | — | 签收 |
| POST | `/complete/{taskId}` | `{comment,variables}` | 审批通过 |
| POST | `/reject/{taskId}` | `{comment}` | 驳回（=结束实例） |
| POST | `/delegate/{taskId}` | `{targetUser,comment}` | 委派 |
| POST | `/addSign/{taskId}` | `{targetUser,position,comment}` | 加签 |

### 8.5 历史 `/flowable/history`

| Method | Path | 说明 |
|---|---|---|
| GET | `/instance/{instanceId}` | 审批轨迹列表 |
| GET | `/diagram/{instanceId}` | 带高亮的流程图 PNG |

---

## 9. 运维与故障排查

### 9.1 常见启动错误

| 报错 | 原因 | 解决 |
|---|---|---|
| `No qualifying bean ... AsyncTaskExecutor @Qualifier("applicationTaskExecutor")` | Spring `TaskExecutionAutoConfiguration` 被其他 Executor Bean 遮蔽 | 已由 `FlowableEngineConfig` 兜底提供 |
| `Failed to start bean 'redisContainer'` | Redis 未启动或密码错 | 启动 Redis，核对 `spring.data.redis.password` |
| `Unable to find the data source` | 动态数据源未初始化 | 检查 `spring.datasource.dynamic.datasource.master` 是否正确 |
| `Table 'ACT_GE_PROPERTY' doesn't exist` | 建表开关关闭 + 脚本未跑 | 开 `database-schema-update=true` 让其自建，或手动导入 `flowable.mysql.create.all.sql` |

### 9.2 运行时错误

| 错误 | 原因 | 处理 |
|---|---|---|
| `No process instance found for id 'xxx'` | instanceId 不存在 / 已删除 | 检查是否已被作废/删除 |
| `Cannot find task instance with id xx` | taskId 已完成 | 刷新待办列表 |
| 中文在图片里显示口口 | 字体缺失 | 系统安装"宋体"，或改 `FlowableEngineConfig` 中字体名 |
| 流程部署后立刻 404 找不到 | 挂起状态 | `/flowable/definition/activate/{id}` |

### 9.3 监控指标建议

- **引擎 JMX**：`org.flowable:*`
- **慢 SQL**：Druid 监控面板（已开启 `/druid`）
- **异步任务队列积压**：`ACT_RU_JOB` 表行数突增
- **线程池水位**：`flowable-task-*` 前缀（由 `FlowableEngineConfig` 定义，core=8 max=32）

### 9.4 清理数据

**清空所有流程数据**（开发环境）：
```sql
-- 按依赖顺序 truncate
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE ACT_HI_TASKINST;
TRUNCATE ACT_HI_ACTINST;
TRUNCATE ACT_HI_PROCINST;
TRUNCATE ACT_RU_TASK;
TRUNCATE ACT_RU_EXECUTION;
TRUNCATE ACT_RU_VARIABLE;
TRUNCATE ACT_GE_BYTEARRAY;
TRUNCATE ACT_RE_DEPLOYMENT;
TRUNCATE ACT_RE_PROCDEF;
TRUNCATE ACT_RE_MODEL;
SET FOREIGN_KEY_CHECKS=1;
```

---

## 10. 术语表

| 术语 | 含义 |
|---|---|
| **模型（Model）** | 草稿态 BPMN，保存在 `ACT_RE_MODEL`，**不可被发起** |
| **部署（Deployment）** | 把模型发布到引擎，生成 **流程定义** |
| **流程定义（ProcessDefinition）** | 已部署的流程版本，`key` + `version` 唯一 |
| **流程实例（ProcessInstance）** | 一次发起后的运行时单据，对应一条业务 |
| **任务（Task）** | userTask 节点产生的待办条目 |
| **businessKey** | 业务系统主键，关联业务表 |
| **variables** | 流程变量，供网关/监听使用 |
| **候选人 / 候选组** | `candidateUser` / `candidateGroup`，多对多，需签收 |
| **分配人 / 办理人** | `assignee`，一对一，直接可操作 |
| **挂起（Suspend）** | 流程定义暂停，不可发起新实例，已有实例继续执行 |
| **级联删除** | 删除部署同时删除所有实例、任务、历史 |
| **租户** | `tenantId`，按 HTTP 头 `X-Tenant-Id` 传入，默认 `0` |

---

## 附录 A：相关文档

| 文档 | 路径 |
|---|---|
| 实施计划 | `docs/flowable/implementation-plan.md` |
| 自动化测试报告 | `docs/flowable/test-report-2026-04-17.md` |
| API 测试脚本 | `docs/flowable/tests/run-api-tests.ps1` |
| 后端 README | `jeecg-boot-module/jeecg-module-flowable/README.md` |
| 菜单初始化 SQL | `jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.1_2__flowable_menu.sql` |

## 附录 B：联系与反馈

- 内部负责人：baixiuhua
- Bug / 需求：提工单到内部 Issue 系统，标签 `flowable`
- 紧急联系：查看启动类上的 `@author` 注释
