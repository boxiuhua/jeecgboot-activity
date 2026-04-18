package org.jeecg.modules.flowable.runtime.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.flowable.common.FlowableConstants;
import org.jeecg.modules.flowable.common.FlowableException;
import org.jeecg.modules.flowable.runtime.dto.ProcessStartRequest;
import org.jeecg.modules.flowable.runtime.service.IFlowableRuntimeService;
import org.jeecg.modules.flowable.tenant.JeecgTenantInfoHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlowableRuntimeServiceImpl implements IFlowableRuntimeService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final IdentityService identityService;
    private final JeecgTenantInfoHolder tenantHolder;

    @Override
    public String start(ProcessStartRequest request) {
        if (request == null || request.getProcessDefinitionKey() == null) {
            throw new FlowableException("processDefinitionKey 必填");
        }
        String currentUser = currentUserId();
        String tenantId = tenantHolder.getCurrentTenantId();
        // 预置发起人变量
        Map<String, Object> vars = request.getVariables() == null ? new HashMap<>() : new HashMap<>(request.getVariables());
        vars.put(FlowableConstants.VAR_INITIATOR, currentUser);
        if (request.getBusinessTitle() != null) {
            vars.put(FlowableConstants.VAR_BUSINESS_TITLE, request.getBusinessTitle());
        }

        identityService.setAuthenticatedUserId(currentUser);
        ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(request.getProcessDefinitionKey())
                .tenantId(tenantId)
                .variables(vars);
        if (request.getBusinessKey() != null) {
            builder.businessKey(request.getBusinessKey());
        }
        if (request.getBusinessTitle() != null) {
            builder.name(request.getBusinessTitle());
        }
        ProcessInstance instance = builder.start();
        return instance.getId();
    }

    @Override
    public IPage<Map<String, Object>> listMyStarted(int pageNo, int pageSize) {
        String user = currentUserId();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantHolder.getCurrentTenantId())
                .startedBy(user)
                .orderByProcessInstanceStartTime().desc();
        long total = query.count();
        int offset = Math.max(0, (pageNo - 1)) * pageSize;
        List<HistoricProcessInstance> records = query.listPage(offset, pageSize);
        List<Map<String, Object>> rows = new ArrayList<>(records.size());
        for (HistoricProcessInstance hi : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", hi.getId());
            map.put("name", hi.getName());
            map.put("businessKey", hi.getBusinessKey());
            map.put("processDefinitionKey", hi.getProcessDefinitionKey());
            map.put("processDefinitionName", hi.getProcessDefinitionName());
            map.put("processDefinitionVersion", hi.getProcessDefinitionVersion());
            map.put("startTime", hi.getStartTime());
            map.put("endTime", hi.getEndTime());
            map.put("durationInMillis", hi.getDurationInMillis());
            map.put("endActivityId", hi.getEndActivityId());
            map.put("status", hi.getEndTime() == null ? "RUNNING" : "ENDED");
            rows.add(map);
        }
        Page<Map<String, Object>> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(rows);
        return page;
    }

    @Override
    public void cancel(String instanceId, String reason) {
        runtimeService.deleteProcessInstance(instanceId, reason == null ? "用户取消" : reason);
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
