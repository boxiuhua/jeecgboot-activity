package org.jeecg.modules.flowable.definition.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.flowable.common.FlowableException;
import org.jeecg.modules.flowable.definition.service.IFlowableDefinitionService;
import org.jeecg.modules.flowable.tenant.JeecgTenantInfoHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlowableDefinitionServiceImpl implements IFlowableDefinitionService {

    private final RepositoryService repositoryService;
    private final JeecgTenantInfoHolder tenantHolder;

    @Override
    public IPage<Map<String, Object>> listDefinitions(String keyword, int pageNo, int pageSize) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenantHolder.getCurrentTenantId())
                .latestVersion()
                .orderByProcessDefinitionKey().asc();
        if (oConvertUtils.isNotEmpty(keyword)) {
            query.processDefinitionNameLike("%" + keyword + "%");
        }
        long total = query.count();
        int offset = Math.max(0, (pageNo - 1)) * pageSize;
        List<ProcessDefinition> defs = query.listPage(offset, pageSize);
        List<Map<String, Object>> rows = new ArrayList<>(defs.size());
        for (ProcessDefinition def : defs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", def.getId());
            map.put("key", def.getKey());
            map.put("name", def.getName());
            map.put("version", def.getVersion());
            map.put("category", def.getCategory());
            map.put("description", def.getDescription());
            map.put("deploymentId", def.getDeploymentId());
            map.put("resourceName", def.getResourceName());
            map.put("tenantId", def.getTenantId());
            map.put("suspended", def.isSuspended());
            rows.add(map);
        }
        Page<Map<String, Object>> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(rows);
        return page;
    }

    @Override
    public void suspend(String definitionId) {
        repositoryService.suspendProcessDefinitionById(definitionId, true, null);
    }

    @Override
    public void activate(String definitionId) {
        repositoryService.activateProcessDefinitionById(definitionId, true, null);
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    @Override
    public String getBpmnXml(String definitionId) {
        ProcessDefinition def = repositoryService.getProcessDefinition(definitionId);
        if (def == null) {
            throw new FlowableException("流程定义不存在: " + definitionId);
        }
        try (InputStream in = repositoryService.getResourceAsStream(def.getDeploymentId(), def.getResourceName())) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FlowableException("读取 BPMN XML 失败", e);
        }
    }

    @Override
    public byte[] getDiagram(String definitionId) {
        ProcessDefinition def = repositoryService.getProcessDefinition(definitionId);
        if (def == null) {
            throw new FlowableException("流程定义不存在: " + definitionId);
        }
        String diagramResource = def.getDiagramResourceName();
        if (oConvertUtils.isEmpty(diagramResource)) {
            return new byte[0];
        }
        try (InputStream in = repositoryService.getResourceAsStream(def.getDeploymentId(), diagramResource);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new FlowableException("读取流程图失败", e);
        }
    }
}
