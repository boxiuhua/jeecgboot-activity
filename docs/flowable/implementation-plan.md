# JeecgBoot 集成 Flowable 工作流引擎 · 实施计划

> **版本**：v1.0
> **制定日期**：2026-04-17
> **负责人**：baixiuhua
> **目标版本**：Flowable 7.1.x + JeecgBoot 3.9.1

---

## 一、项目背景与目标

### 1.1 项目现状

- **技术栈**：Spring Boot 3.5.5 + Java 17 + MyBatis-Plus 3.5.12
- **认证**：Shiro 2.0.5 + JWT
- **数据源**：已集成 dynamic-datasource 4.3.1
- **用户体系**：`sys_user` / `sys_role` / `sys_depart` / `sys_permission`（位于 `jeecg-module-system/jeecg-system-biz`）
- **前端**：Vue3（`jeecgboot-vue3`）
- **工作流**：无（空白）

### 1.2 集成目标

在 JeecgBoot 3.9.1 基础上**零侵入**集成 Flowable 工作流引擎，实现：

- 后台可视化 BPMN 流程设计与保存
- 流程部署、发布、版本管理、挂起/激活
- 用户任务节点的候选人/候选组/候选部门授权
- 待办、已办、流转历史、流程追踪图
- 与 JeecgBoot 现有用户权限体系打通
- 多租户隔离
- 通知扩展点（预留 IM 对接能力）

---

## 二、核心决策

| # | 决策项 | 最终选择 | 说明 |
|---|---|---|---|
| 1 | 数据库 | **共用业务库** | `ACT_*` 表与 `sys_*`、业务表同库 |
| 2 | 设计器 | **bpmn-js**（MIT 协议） | 自定义属性面板，支持二次开发 |
| 3 | 表单方案 | **online 表单 + 外部路由双模式** | 节点属性 `formType` 选类型 |
| 4 | 多租户 | **启用** | 按 `tenantId` 隔离流程实例与任务 |
| 5 | IM 通知 | **预留扩展点** | `NotifyStrategy` 接口，默认站内信 |

---

## 三、核心原则

| 原则 | 说明 |
|---|---|
| **单库共存** | Flowable 与业务表同库，事务可统一管理（`@Transactional` 直接覆盖） |
| **用户单向同步** | JeecgBoot 为主，Flowable 的 `ACT_ID_*` 为从；监听 `sys_user` / `sys_role` 增删改事件 |
| **认证复用** | 全部走 Shiro + JWT，不使用 flowable-ui 的自带账号体系 |
| **统一返回** | 所有接口返回 `org.jeecg.common.api.vo.Result` |
| **租户隔离** | 所有部署、启动、查询强制带 `tenantId` |
| **可回滚** | 每阶段均可独立删除模块、清空 `ACT_*` 表回到干净状态 |

---

## 四、阶段划分

共 6 个阶段，**预计 17 工作日**。

### 🟦 阶段 1：环境搭建与选型验证（2 天）

**目标**：确认 Flowable 7.1.x 与 Spring Boot 3.5.5 / Java 17 / MyBatis-Plus 兼容，跑通最小流程。

**产出物**：
1. 独立 POC 工程（验证后删除，不污染主仓）
2. `docs/flowable/版本兼容性验证报告.md`
3. 最终选定版本号（建议 `flowable-spring-boot-starter:7.1.0`）

**关键任务**：
- [ ] 新建临时 Spring Boot 3.5.5 工程，引入 `flowable-spring-boot-starter`
- [ ] 配置 MySQL 数据源，启动让其自建 61 张 `ACT_*` 表
- [ ] 部署单节点 BPMN，调用 `RuntimeService.startProcessInstanceByKey()` 跑通
- [ ] 验证依赖冲突（重点：`mybatis-plus`、`jackson`、`spring-security`）
- [ ] 导出 `ACT_*` 表 DDL 供阶段 2 纳入 Flyway

**风险点**：
- Flowable 内部使用自己的 MyBatis，可能与 MyBatis-Plus 拦截器冲突 → 需单独 `SqlSessionFactory`
- Flowable 7 默认 Jackson 版本 vs. Spring Boot 3.5 管理的版本

**退出准则**：
- ✅ 最小流程能完整运行
- ✅ 无启动报错、无依赖版本冲突
- ✅ `ACT_*` DDL 已导出

---

### 🟦 阶段 2：后端模块骨架搭建 + 多租户（2.5 天）

**目标**：新增 `jeecg-module-flowable` 模块，完成依赖、配置、用户同步、多租户集成。

**产出物**：
```
jeecg-boot/
└── jeecg-module-flowable/
    ├── pom.xml
    └── src/main/java/org/jeecg/modules/flowable/
        ├── config/
        │   ├── FlowableConfig.java              # 引擎配置
        │   ├── FlowableSqlSessionConfig.java    # 隔离 MyBatis
        │   └── FlowableSecurityBridge.java      # Shiro 桥接
        ├── tenant/
        │   ├── JeecgTenantInfoHolder.java       # 多租户上下文
        │   └── TenantAwareQueryHelper.java
        ├── identity/
        │   ├── JeecgUserSyncListener.java       # 用户同步监听
        │   ├── JeecgRoleSyncListener.java       # 角色同步监听
        │   └── JeecgIdentityService.java
        └── common/
            └── FlowableConstants.java
```

**关键任务**：

| # | 任务 | 备注 |
|---|---|---|
| 1 | 根 `pom.xml` 声明 `flowable.version=7.1.0` | - |
| 2 | 新建 `jeecg-module-flowable` 子模块 | 参考 `jeecg-module-demo` |
| 3 | 配置独立 `SqlSessionFactory` | 避免 MyBatis-Plus 拦截器污染 Flowable |
| 4 | 阶段 1 导出的 DDL 纳入 Flyway | `V3.9.1.1__flowable_init.sql` |
| 5 | `application-dev.yml` 配置 `flowable.database-schema-update: false` | 生产环境锁定 |
| 6 | 实现 `JeecgTenantInfoHolder` | 从 `JwtUtil.getTenantId()` 获取 |
| 7 | 实现 `JeecgUserSyncListener` | 监听 `sys_user` 增删改 → `ACT_ID_USER` |
| 8 | 实现 `JeecgRoleSyncListener` | 监听 `sys_role` → `ACT_ID_GROUP` (type=`role`) |
| 9 | 启动时全量同步（首次初始化） | `ApplicationRunner` |

**关键配置**（`application.yml`）：
```yaml
flowable:
  database-schema-update: false       # 生产关闭自动建表
  async-executor-activate: true       # 开启异步执行器
  history-level: full                 # 记录完整历史
  process:
    servlet:
      path: /flowable-api
```

**退出准则**：
- ✅ 项目启动无异常
- ✅ 新增 `sys_user`，`ACT_ID_USER` 同步出现记录
- ✅ 新增 `sys_role`，`ACT_ID_GROUP` 同步
- ✅ 多租户上下文可正确透传

---

### 🟦 阶段 3：流程设计器（前端）（3 天）

**目标**：Vue3 集成 bpmn-js，实现拖拽设计、保存 XML、编辑已有模型，支持节点授权与表单双模式。

**产出物**：
```
jeecgboot-vue3/src/views/flowable/
├── modeler/
│   ├── index.vue                      # 设计器主页
│   └── components/
│       ├── BpmnDesigner.vue           # bpmn-js 画布封装
│       ├── PropertyPanel.vue          # 自定义右侧属性面板
│       ├── AssigneeConfig.vue         # 审批人配置子组件
│       ├── FormConfig.vue             # 表单配置子组件
│       └── NodeAuthDialog.vue         # 节点授权弹窗
├── model/
│   └── index.vue                      # 模型列表
└── api/
    └── model.ts                       # 模型相关 API
```

**关键任务**：

1. **依赖安装**
   ```bash
   pnpm add bpmn-js bpmn-js-properties-panel camunda-bpmn-moddle
   ```

2. **封装 `BpmnDesigner.vue`**
   - 暴露方法：`getXml()` / `importXml(xml)` / `downloadSvg()`
   - 监听事件：`element.changed` / `shape.added`

3. **自定义属性面板**（核心，`userTask` 节点）

   **基础属性**：
   - `name` 节点名称
   - `id` 节点 key

   **审批人类型**（`flowable:assigneeType`，自定义扩展属性）：
   | 类型 | 值 | 对应 Flowable 属性 |
   |---|---|---|
   | 指定人 | `fixed` | `flowable:assignee` |
   | 候选人 | `users` | `flowable:candidateUsers` |
   | 候选组（角色） | `roles` | `flowable:candidateGroups` |
   | 候选组（部门） | `depts` | `flowable:candidateGroups`（带前缀 `DEPT_`） |
   | 发起人 | `starter` | `${INITIATOR}` |
   | 上级 | `leader` | `${getLeader(INITIATOR)}` |
   | 部门领导 | `deptLeader` | `${getDeptLeader(INITIATOR)}` |

   **表单配置**（`flowable:formType` + `flowable:formValue`，自定义扩展）：
   | 类型 | 值 | 说明 |
   |---|---|---|
   | online 表单 | `online` | formValue 存 `cgformHeadId` |
   | 外部路由 | `route` | formValue 存 `/xxx/approval/:id` |
   | 无表单 | `none` | 仅审批按钮 |

   **其他**：
   - `dueDate` 到期时间
   - `priority` 优先级

4. **复用 JeecgBoot 现有选人组件**
   - `JSelectUserByDept`（候选人）
   - `JSelectRole`（候选角色）
   - `JSelectDepart`（候选部门）

**退出准则**：
- ✅ 拖出"开始 → 用户任务 → 结束"能保存
- ✅ 设置候选组后重新打开能还原
- ✅ 表单类型切换 UI 正确展示
- ✅ 导出 XML 格式合规（Flowable 可解析）

---

### 🟦 阶段 4：流程管理后端接口（3 天）

**目标**：覆盖模型、部署、流程定义三个维度的完整 CRUD。

**产出物**：Controller/Service/Mapper 三层代码 + Swagger 接口文档

#### 4.1 模型管理（`FlowableModelController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/flowable/model/create` | 创建空白模型（key/name/category） |
| PUT | `/flowable/model/update/{id}` | 保存 BPMN XML |
| GET | `/flowable/model/detail/{id}` | 获取模型 XML |
| GET | `/flowable/model/list` | 分页列表（支持租户过滤） |
| DELETE | `/flowable/model/delete/{id}` | 删除模型 |
| POST | `/flowable/model/deploy/{id}` | **部署为流程定义**（核心） |

#### 4.2 流程定义（`FlowableDefinitionController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/flowable/definition/list` | 已部署流程列表（含版本） |
| PUT | `/flowable/definition/suspend/{id}` | 挂起 |
| PUT | `/flowable/definition/activate/{id}` | 激活 |
| DELETE | `/flowable/definition/delete/{id}` | 删除部署（级联） |
| GET | `/flowable/definition/xml/{id}` | 查看部署后的 XML |
| GET | `/flowable/definition/diagram/{id}` | 流程图 SVG |

#### 4.3 关键实现点

- 部署时把模型 XML 写入 `Deployment`，保留与模型表的关联
- 部署时必须带 `tenantId`：`deployment.tenantId(currentTenantId).deploy()`
- 挂起后 `startProcessInstance` 将抛异常，前端需做错误提示
- 所有操作记录审计日志（写 `sys_log`）

**退出准则**：
- ✅ 前端设计器保存的模型能成功部署
- ✅ 列表能按租户隔离显示
- ✅ 挂起/激活状态切换正确

---

### 🟦 阶段 5：流程运行时 + 任务中心 + 通知扩展（4 天）

**目标**：实现发起、审批、回退、撤回、待办列表，集成 JeecgBoot 消息通知；预留 IM 扩展点。

**产出物**：

#### 5.1 运行时 API（`FlowableRuntimeController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/flowable/process/start` | 发起流程 |
| GET | `/flowable/process/my` | 我发起的 |
| POST | `/flowable/process/cancel/{instanceId}` | 作废 |

#### 5.2 任务 API（`FlowableTaskController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/flowable/task/todo` | **我的待办** |
| GET | `/flowable/task/done` | 我的已办 |
| POST | `/flowable/task/complete/{taskId}` | 审批通过 |
| POST | `/flowable/task/reject/{taskId}` | 驳回到上一节点 |
| POST | `/flowable/task/claim/{taskId}` | 签收 |
| POST | `/flowable/task/delegate/{taskId}` | 委派 |
| POST | `/flowable/task/addSign` | 加签（前/后） |

#### 5.3 历史 API（`FlowableHistoryController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/flowable/history/instance/{instanceId}` | 审批历史记录 |
| GET | `/flowable/history/diagram/{instanceId}` | 带高亮的流程图 |

#### 5.4 通知扩展点（新增）

```java
public interface NotifyStrategy {
    String getType();   // "SITE" / "DINGTALK" / "WECOM"
    void send(NotifyContext context);
}
```

- `NotifyContext` 包含：接收人、任务 ID、流程名、操作链接
- 默认实现 `SiteNotifyStrategy`（站内信，接入 JeecgBoot 现有 `sys_announcement`）
- 通过 Spring SPI 加载，后期补 `DingTalkNotifyStrategy` 无需改核心

#### 5.5 前端页面

```
views/flowable/
├── process/
│   ├── start.vue              # 发起流程
│   └── my.vue                 # 我发起的
├── task/
│   ├── todo.vue               # 待办（首页 Widget 同步提供）
│   ├── done.vue               # 已办
│   └── approval.vue           # 审批详情页（含历史 + 表单 + 操作按钮）
└── history/
    └── trace.vue              # 流转追踪图
```

**关键集成点**：
- 待办数量推送：通过 JeecgBoot 现有 WebSocket 推送
- 表单渲染：`formType=online` 时动态加载 online 表单；`formType=route` 时跳转路由
- 历史追踪：使用 `ProcessDiagramGenerator` 生成高亮 SVG

**退出准则**：
- ✅ 发起流程 → 审批 → 完结 全流程跑通
- ✅ 多租户用户只能看到本租户待办
- ✅ 站内信通知正常触达
- ✅ IM 扩展点接口可被第三方实现

---

### 🟦 阶段 6：权限菜单 + 文档 + 压测（1.5 天）

**目标**：交付可用的完整系统。

**产出物**：
- [ ] 数据库脚本：`sys_permission` 插入流程管理相关菜单和按钮权限
- [ ] 角色默认授权：超管自动拥有所有流程权限
- [ ] `docs/flowable/用户手册.md`（截图版）
- [ ] `docs/flowable/开发手册.md`（自定义监听器、对接业务表单、扩展通知）
- [ ] JMeter 压测：100 并发发起流程，P99 < 500ms

**菜单结构**：
```
工作流管理
├── 流程设计
│   ├── 模型管理    [flowable:model:view]
│   └── 在线设计器   [flowable:model:design]
├── 流程部署
│   └── 已部署流程   [flowable:definition:view]
├── 任务中心
│   ├── 我的待办    [flowable:task:todo]
│   ├── 我的已办    [flowable:task:done]
│   └── 我发起的    [flowable:process:my]
└── 流程监控
    └── 运行实例    [flowable:monitor:view]
```

**退出准则**：
- ✅ 超管登录能看到完整菜单
- ✅ 普通用户菜单按权限过滤
- ✅ 压测通过
- ✅ 用户/开发手册完成

---

## 五、里程碑与时间线

| 里程碑 | 预计日期 | 累计工期 | 交付物 |
|---|---|---|---|
| M1 POC 验证 | 2026-04-19 | 2 天 | POC 工程 + 兼容性报告 |
| M2 后端骨架 + 多租户 | 2026-04-22 | 4.5 天 | 模块代码 + 用户同步 |
| M3 设计器（含双模式表单） | 2026-04-27 | 8 天 | 前端设计器 |
| M4 管理端 API | 2026-04-30 | 11 天 | 模型/部署接口 |
| M5 任务中心 + 通知扩展 | 2026-05-07 | 15 天 | 任务中心 + 审批页 |
| **M6 系统交付** | **2026-05-09** | **17 天** | 菜单 + 手册 + 压测报告 |

---

## 六、风险清单

| 风险 | 等级 | 应对措施 |
|---|---|---|
| Flowable MyBatis 与 MyBatis-Plus 冲突 | **高** | 阶段 1 POC 优先验证；独立 SqlSessionFactory |
| bpmn-js 属性面板二次开发复杂度 | 中 | 参考开源项目 `vue3-bpmn-modeler`；留足 3 天 |
| 多租户隔离遗漏查询入口 | 中 | 建立统一 `TenantAwareQueryHelper` 强制包装 |
| 共用业务库导致备份膨胀 | 低 | 阶段 6 运维文档中明确告知；后期可归档 `ACT_HI_*` |
| Shiro 用户上下文在异步任务丢失 | 中 | 使用 `AsyncExecutor` 时主动传递 `Subject` |

---

## 七、待后续明确的事项

以下事项当前**不阻塞开工**，在对应阶段前明确即可：

1. **业务表单对接清单** — 阶段 5 前明确首批接入哪些 online 表单
2. **角色到 Flowable Group 的映射规则** — 阶段 2 前确认是否所有 `sys_role` 都同步
3. **压测目标指标** — 阶段 6 前由运维/产品提出具体 TPS 要求
4. **IM 通知优先级** — 阶段 6 后规划 v2 版本（钉钉 or 企微）

---

## 八、附录

### 8.1 关键代码路径参考

| 功能 | JeecgBoot 现有路径 |
|---|---|
| 用户实体 | `jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/entity/SysUser.java` |
| 角色实体 | 同上目录 `SysRole.java` |
| 统一返回 | `jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/common/api/vo/Result.java` |
| Shiro 配置 | `jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/config/shiro/` |
| Flyway 脚本 | `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/` |
| 前端菜单 API | `jeecgboot-vue3/src/api/sys/menu.ts` |
| 前端路由辅助 | `jeecgboot-vue3/src/router/helper/menuHelper.ts` |

### 8.2 版本依赖建议

```xml
<properties>
    <flowable.version>7.1.0</flowable.version>
    <bpmn-js.version>17.x</bpmn-js.version>
</properties>
```

### 8.3 变更记录

| 日期 | 版本 | 变更人 | 变更内容 |
|---|---|---|---|
| 2026-04-17 | v1.0 | baixiuhua | 初版制定 |
