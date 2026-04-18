package org.jeecg.modules.flowable.task.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.flowable.common.FlowableConstants;
import org.jeecg.modules.flowable.common.FlowableException;
import org.jeecg.modules.flowable.notify.NotifyContext;
import org.jeecg.modules.flowable.notify.NotifyDispatcher;
import org.jeecg.modules.flowable.task.dto.TaskActionRequest;
import org.jeecg.modules.flowable.task.service.IFlowableTaskService;
import org.jeecg.modules.flowable.tenant.JeecgTenantInfoHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowableTaskServiceImpl implements IFlowableTaskService {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final JeecgTenantInfoHolder tenantHolder;
    private final NotifyDispatcher notifyDispatcher;

    @Override
    public IPage<Map<String, Object>> todo(int pageNo, int pageSize) {
        String user = currentUserId();
        TaskQuery query = taskService.createTaskQuery()
                .taskTenantId(tenantHolder.getCurrentTenantId())
                .or()
                    .taskAssignee(user)
                    .taskCandidateUser(user)
                .endOr()
                .active()
                .orderByTaskCreateTime().desc();
        long total = query.count();
        int offset = Math.max(0, (pageNo - 1)) * pageSize;
        List<Task> tasks = query.listPage(offset, pageSize);
        List<Map<String, Object>> rows = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("description", t.getDescription());
            map.put("assignee", t.getAssignee());
            map.put("owner", t.getOwner());
            map.put("createTime", t.getCreateTime());
            map.put("dueDate", t.getDueDate());
            map.put("priority", t.getPriority());
            map.put("processInstanceId", t.getProcessInstanceId());
            map.put("processDefinitionId", t.getProcessDefinitionId());
            map.put("formKey", t.getFormKey());
            rows.add(map);
        }
        Page<Map<String, Object>> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(rows);
        return page;
    }

    @Override
    public IPage<Map<String, Object>> done(int pageNo, int pageSize) {
        String user = currentUserId();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskTenantId(tenantHolder.getCurrentTenantId())
                .taskAssignee(user)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc();
        long total = query.count();
        int offset = Math.max(0, (pageNo - 1)) * pageSize;
        List<HistoricTaskInstance> tasks = query.listPage(offset, pageSize);
        List<Map<String, Object>> rows = new ArrayList<>(tasks.size());
        for (HistoricTaskInstance t : tasks) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("assignee", t.getAssignee());
            map.put("startTime", t.getStartTime());
            map.put("endTime", t.getEndTime());
            map.put("durationInMillis", t.getDurationInMillis());
            map.put("processInstanceId", t.getProcessInstanceId());
            map.put("processDefinitionId", t.getProcessDefinitionId());
            rows.add(map);
        }
        Page<Map<String, Object>> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(rows);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskId, TaskActionRequest request) {
        Task task = requireTask(taskId);
        String user = currentUserId();
        taskService.setAssignee(taskId, user);
        addComment(task, user, request, FlowableConstants.APPROVE_PASS);
        Map<String, Object> vars = request == null || request.getVariables() == null
                ? new HashMap<>() : new HashMap<>(request.getVariables());
        taskService.complete(taskId, vars);
        fanoutNotify(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, TaskActionRequest request) {
        Task task = requireTask(taskId);
        String user = currentUserId();
        taskService.setAssignee(taskId, user);
        addComment(task, user, request, FlowableConstants.APPROVE_REJECT);
        // 简化实现：驳回 = 直接结束流程实例
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(),
                "驳回-" + (request != null && request.getComment() != null ? request.getComment() : ""));
    }

    @Override
    public void claim(String taskId) {
        taskService.claim(taskId, currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, TaskActionRequest request) {
        if (request == null || oConvertUtils.isEmpty(request.getTargetUser())) {
            throw new FlowableException("委派目标用户必填");
        }
        taskService.delegateTask(taskId, request.getTargetUser());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, TaskActionRequest request) {
        if (request == null || oConvertUtils.isEmpty(request.getTargetUser())) {
            throw new FlowableException("加签目标用户必填");
        }
        Task task = requireTask(taskId);
        // 简化：复制当前任务给目标用户（追加候选人）
        taskService.addCandidateUser(taskId, request.getTargetUser());
        addComment(task, currentUserId(), request, "addSign-" + request.getPosition());
    }

    private Task requireTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new FlowableException("任务不存在或已完成: " + taskId);
        }
        return task;
    }

    private void addComment(Task task, String user, TaskActionRequest request, String type) {
        String comment = request == null ? null : request.getComment();
        if (oConvertUtils.isEmpty(comment)) {
            return;
        }
        taskService.addComment(task.getId(), task.getProcessInstanceId(), type, comment);
    }

    private void fanoutNotify(Task completedTask) {
        // 任务完成后通知下一环节候选人
        try {
            List<Task> nextTasks = taskService.createTaskQuery()
                    .processInstanceId(completedTask.getProcessInstanceId()).active().list();
            ProcessDefinition def = repositoryService.getProcessDefinition(completedTask.getProcessDefinitionId());
            for (Task next : nextTasks) {
                NotifyContext ctx = NotifyContext.builder()
                        .assignee(next.getAssignee())
                        .taskId(next.getId())
                        .taskName(next.getName())
                        .processName(def == null ? null : def.getName())
                        .businessTitle(String.valueOf(runtimeService.getVariable(next.getProcessInstanceId(),
                                FlowableConstants.VAR_BUSINESS_TITLE)))
                        .tenantId(tenantHolder.getCurrentTenantId())
                        .build();
                notifyDispatcher.dispatch(ctx);
            }
        } catch (Exception e) {
            log.warn("[flowable] 通知下一节点失败: {}", e.getMessage());
        }
    }

    private String currentUserId() {
        try {
            String username = JwtUtil.getUserNameByToken(org.jeecg.common.util.SpringContextUtils.getHttpServletRequest());
            if (username != null && !username.isEmpty()) {
                return username;
            }
        } catch (Exception ignored) {
        }
        return "anonymous";
    }
}
