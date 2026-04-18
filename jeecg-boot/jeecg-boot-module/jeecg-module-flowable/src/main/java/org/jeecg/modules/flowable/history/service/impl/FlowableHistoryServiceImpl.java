package org.jeecg.modules.flowable.history.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.task.Comment;
import org.flowable.engine.TaskService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.jeecg.modules.flowable.common.FlowableException;
import org.jeecg.modules.flowable.history.service.IFlowableHistoryService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowableHistoryServiceImpl implements IFlowableHistoryService {

    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ProcessEngineConfiguration processEngineConfiguration;

    @Override
    public List<Map<String, Object>> instanceHistory(String instanceId) {
        List<HistoricTaskInstance> histTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(instanceId).orderByHistoricTaskInstanceStartTime().asc().list();
        List<Map<String, Object>> rows = new ArrayList<>(histTasks.size());
        for (HistoricTaskInstance t : histTasks) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskId", t.getId());
            map.put("taskName", t.getName());
            map.put("assignee", t.getAssignee());
            map.put("startTime", t.getStartTime());
            map.put("endTime", t.getEndTime());
            map.put("durationInMillis", t.getDurationInMillis());
            List<Comment> comments = taskService.getTaskComments(t.getId());
            map.put("comments", comments.stream().map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("type", c.getType());
                cm.put("userId", c.getUserId());
                cm.put("time", c.getTime());
                cm.put("message", c.getFullMessage());
                return cm;
            }).collect(Collectors.toList()));
            rows.add(map);
        }
        return rows;
    }

    @Override
    public byte[] diagramWithHighlight(String instanceId) {
        var processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        String processDefinitionId;
        if (processInstance != null) {
            processDefinitionId = processInstance.getProcessDefinitionId();
        } else {
            var hi = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).singleResult();
            if (hi == null) {
                throw new FlowableException("流程实例不存在: " + instanceId);
            }
            processDefinitionId = hi.getProcessDefinitionId();
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<HistoricActivityInstance> finished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId)
                .finished()
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        List<String> highlightActivities = finished.stream()
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());
        List<String> highlightFlows = new ArrayList<>();

        List<String> currentActivities = new ArrayList<>();
        if (processInstance != null) {
            List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(instanceId).list();
            for (Execution e : executions) {
                if (e.getActivityId() != null) {
                    currentActivities.add(e.getActivityId());
                }
            }
        }

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        try (InputStream in = diagramGenerator.generateDiagram(
                bpmnModel, "png",
                highlightActivities,
                highlightFlows,
                processEngineConfiguration.getActivityFontName(),
                processEngineConfiguration.getLabelFontName(),
                processEngineConfiguration.getAnnotationFontName(),
                processEngineConfiguration.getClassLoader(),
                1.0,
                true);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new FlowableException("生成流程图失败", e);
        }
    }
}
