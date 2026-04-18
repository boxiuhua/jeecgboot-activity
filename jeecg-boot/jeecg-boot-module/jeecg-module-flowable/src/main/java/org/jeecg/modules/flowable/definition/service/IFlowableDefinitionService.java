package org.jeecg.modules.flowable.definition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface IFlowableDefinitionService {

    IPage<Map<String, Object>> listDefinitions(String keyword, int pageNo, int pageSize);

    void suspend(String definitionId);

    void activate(String definitionId);

    void deleteDeployment(String deploymentId, boolean cascade);

    String getBpmnXml(String definitionId);

    byte[] getDiagram(String definitionId);
}
