# jeecg-module-flowable

JeecgBoot 集成 Flowable 7.1.x 工作流引擎。

## 启用步骤

### 1. 后端

父 pom 已统一管理 `flowable.version=7.1.0`，并在 `jeecg-system-start` 中引入了本模块依赖。开箱即用。

首次启动时 Flowable 会自动建 61 张 `ACT_*` 表到业务库（由 `flowable.database-schema-update=true` 控制）。生产环境建议：

```yaml
flowable:
  database-schema-update: false
```

然后用 Flowable 官方提供的 `org/flowable/db/create/*.mysql.sql` 或手动 dump 后纳入 Flyway 管理。

### 2. 前端依赖

```bash
pnpm install   # package.json 已添加 bpmn-js / bpmn-js-properties-panel / camunda-bpmn-moddle
```

### 3. 菜单数据

`jeecg-system-start` 的 Flyway 目录新增 `V3.9.1_2__flowable_menu.sql`，将工作流管理菜单写入 `sys_permission`。

> 若 `spring.flyway.enabled=false`，可手动执行该 SQL。

## 主要 API

| 模块 | 路径前缀 |
|---|---|
| 模型 | `/flowable/model/*` |
| 流程定义 | `/flowable/definition/*` |
| 运行时 | `/flowable/process/*` |
| 任务 | `/flowable/task/*` |
| 历史 | `/flowable/history/*` |

## 多租户

通过 `JeecgTenantInfoHolder` 读取请求头 `X-Tenant-Id`，所有部署/查询都会带上 `tenantId`。

## 通知扩展点

实现 `NotifyStrategy` 接口并声明为 Spring Bean 即可。默认包含 `SiteNotifyStrategy`（站内信）。

```java
@Component
public class DingTalkNotifyStrategy implements NotifyStrategy {
    public String getType() { return "DINGTALK"; }
    public void send(NotifyContext ctx) { /* 调用钉钉 open API */ }
}
```

## 节点授权

BPMN 中 `userTask` 的 `assigneeType` 扩展属性支持：

| 类型 | 对应 |
|---|---|
| `fixed` | `flowable:assignee` 固定人 |
| `users` | `flowable:candidateUsers` 候选人 |
| `roles` | `flowable:candidateGroups` 候选角色 |
| `depts` | `flowable:candidateGroups` + `DEPT_` 前缀 |
| `starter` | `${INITIATOR}` |
| `leader` | `${getLeader(INITIATOR)}` 需自行实现 Spring EL 函数 |
| `deptLeader` | `${getDeptLeader(INITIATOR)}` 同上 |

## 表单集成

`userTask` 扩展属性 `formType` 取值：
- `online`：`formKey` 存 JeecgBoot online 表单 ID
- `route`：`formValue` 存前端路由（如 `/leave/approval/:id`）
- `none`：纯审批按钮
