import { defHttp } from '/@/utils/http/axios';

// 全局 axios 默认 successMessageMode='success'，会把后端 message 字段当成功 toast 弹出来；
// 工作流的只读接口（Result.OK(String) 会把字符串同时设到 message 和 result 上，
// 比如 /flowable/model/xml/* 会把整段 BPMN XML 作为 toast 推到屏幕顶上）
// 统一把查询/下载类接口的 toast 关掉；写入类接口保留后端自己的成功消息。
const SILENT = { successMessageMode: 'none' } as const;

enum Api {
  // 模型
  ModelCreate = '/flowable/model/create',
  ModelUpdate = '/flowable/model/update/',
  ModelDelete = '/flowable/model/delete/',
  ModelDetail = '/flowable/model/detail/',
  ModelXml = '/flowable/model/xml/',
  ModelList = '/flowable/model/list',
  ModelDeploy = '/flowable/model/deploy/',
  IdentitySync = '/flowable/model/identity/sync',
  // 流程定义
  DefList = '/flowable/definition/list',
  DefSuspend = '/flowable/definition/suspend/',
  DefActivate = '/flowable/definition/activate/',
  DefDelete = '/flowable/definition/delete/',
  DefXml = '/flowable/definition/xml/',
  DefDiagram = '/flowable/definition/diagram/',
  // 流程运行时
  ProcStart = '/flowable/process/start',
  ProcMy = '/flowable/process/my',
  ProcCancel = '/flowable/process/cancel/',
  // 任务
  TaskTodo = '/flowable/task/todo',
  TaskDone = '/flowable/task/done',
  TaskComplete = '/flowable/task/complete/',
  TaskReject = '/flowable/task/reject/',
  TaskClaim = '/flowable/task/claim/',
  TaskDelegate = '/flowable/task/delegate/',
  TaskAddSign = '/flowable/task/addSign/',
  // 历史
  HistInstance = '/flowable/history/instance/',
  HistDiagram = '/flowable/history/diagram/',
}

// ---- 模型 ----
export const modelCreate = (data: { key: string; name: string; category?: string; description?: string }) =>
  defHttp.post({ url: Api.ModelCreate, data });

/** 保存模型。silent=true 时静默（自动补 DI 的回写场景不需要弹 toast） */
export const modelUpdate = (
  id: string,
  data: { name?: string; category?: string; bpmnXml: string; svg?: string },
  opts?: { silent?: boolean },
) => defHttp.put({ url: Api.ModelUpdate + id, data }, opts?.silent ? SILENT : undefined);

export const modelDetail = (id: string) => defHttp.get({ url: Api.ModelDetail + id }, SILENT);
export const modelXml = (id: string) => defHttp.get({ url: Api.ModelXml + id }, SILENT);
export const modelList = (params: any) => defHttp.get({ url: Api.ModelList, params }, SILENT);
export const modelDelete = (id: string) => defHttp.delete({ url: Api.ModelDelete + id });
export const modelDeploy = (id: string) => defHttp.post({ url: Api.ModelDeploy + id });
export const identitySync = () => defHttp.post({ url: Api.IdentitySync });

// ---- 流程定义 ----
export const definitionList = (params: any) => defHttp.get({ url: Api.DefList, params }, SILENT);
export const definitionSuspend = (id: string) => defHttp.put({ url: Api.DefSuspend + id });
export const definitionActivate = (id: string) => defHttp.put({ url: Api.DefActivate + id });
export const definitionDelete = (deploymentId: string, cascade = true) =>
  defHttp.delete({ url: Api.DefDelete + deploymentId, params: { cascade } });
export const definitionXml = (id: string) => defHttp.get({ url: Api.DefXml + id }, SILENT);

// ---- 运行时 ----
export const processStart = (data: any) => defHttp.post({ url: Api.ProcStart, data });
export const processMy = (params: any) => defHttp.get({ url: Api.ProcMy, params }, SILENT);
export const processCancel = (instanceId: string, reason?: string) =>
  defHttp.post({ url: Api.ProcCancel + instanceId, params: { reason } });

// ---- 任务 ----
export const taskTodo = (params: any) => defHttp.get({ url: Api.TaskTodo, params }, SILENT);
export const taskDone = (params: any) => defHttp.get({ url: Api.TaskDone, params }, SILENT);
export const taskComplete = (taskId: string, data?: any) => defHttp.post({ url: Api.TaskComplete + taskId, data });
export const taskReject = (taskId: string, data?: any) => defHttp.post({ url: Api.TaskReject + taskId, data });
export const taskClaim = (taskId: string) => defHttp.post({ url: Api.TaskClaim + taskId });
export const taskDelegate = (taskId: string, data: any) => defHttp.post({ url: Api.TaskDelegate + taskId, data });
export const taskAddSign = (taskId: string, data: any) => defHttp.post({ url: Api.TaskAddSign + taskId, data });

// ---- 历史 ----
export const historyInstance = (instanceId: string) => defHttp.get({ url: Api.HistInstance + instanceId }, SILENT);
/** 下载带高亮的流程图 PNG（带 token），返回 blob，用 URL.createObjectURL 挂到 <img> */
export const historyDiagram = (instanceId: string) =>
  defHttp.get(
    { url: Api.HistDiagram + instanceId, responseType: 'blob' },
    { isTransformResponse: false },
  );
export const historyDiagramUrl = (instanceId: string) => Api.HistDiagram + instanceId;
export const definitionDiagramUrl = (id: string) => Api.DefDiagram + id;
