package org.jeecg.modules.flowable.model.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.flowable.common.FlowableException;
import org.jeecg.modules.flowable.model.service.IFlowableModelService;
import org.jeecg.modules.flowable.model.vo.ModelCreateRequest;
import org.jeecg.modules.flowable.model.vo.ModelUpdateRequest;
import org.jeecg.modules.flowable.model.vo.ModelVO;
import org.jeecg.modules.flowable.tenant.JeecgTenantInfoHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowableModelServiceImpl implements IFlowableModelService {

    private static final String EMPTY_BPMN_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         targetNamespace="http://www.jeecg.com/bpmn">
                <process id="%s" name="%s" isExecutable="true">
                    <startEvent id="startEvent1" name="开始"/>
                </process>
            </definitions>
            """;

    private final RepositoryService repositoryService;
    private final JeecgTenantInfoHolder tenantHolder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createModel(ModelCreateRequest request) {
        if (request == null || oConvertUtils.isEmpty(request.getKey()) || oConvertUtils.isEmpty(request.getName())) {
            throw new FlowableException("模型 key 和 name 不能为空");
        }
        Model model = repositoryService.newModel();
        model.setKey(request.getKey());
        model.setName(request.getName());
        model.setCategory(request.getCategory());
        model.setTenantId(tenantHolder.getCurrentTenantId());
        repositoryService.saveModel(model);

        String initialXml = String.format(EMPTY_BPMN_TEMPLATE, request.getKey(), request.getName());
        repositoryService.addModelEditorSource(model.getId(), initialXml.getBytes(StandardCharsets.UTF_8));
        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(String modelId, ModelUpdateRequest request) {
        Model model = repositoryService.getModel(modelId);
        if (model == null) {
            throw new FlowableException("模型不存在: " + modelId);
        }
        assertTenant(model.getTenantId());
        if (oConvertUtils.isNotEmpty(request.getName())) {
            model.setName(request.getName());
        }
        if (oConvertUtils.isNotEmpty(request.getCategory())) {
            model.setCategory(request.getCategory());
        }
        repositoryService.saveModel(model);
        if (oConvertUtils.isNotEmpty(request.getBpmnXml())) {
            repositoryService.addModelEditorSource(modelId, request.getBpmnXml().getBytes(StandardCharsets.UTF_8));
        }
        if (oConvertUtils.isNotEmpty(request.getSvg())) {
            repositoryService.addModelEditorSourceExtra(modelId, request.getSvg().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String modelId) {
        Model model = repositoryService.getModel(modelId);
        if (model == null) {
            return;
        }
        assertTenant(model.getTenantId());
        repositoryService.deleteModel(modelId);
    }

    @Override
    public ModelVO getModel(String modelId) {
        Model model = repositoryService.getModel(modelId);
        if (model == null) {
            throw new FlowableException("模型不存在: " + modelId);
        }
        assertTenant(model.getTenantId());
        return toVO(model);
    }

    @Override
    public String getModelXml(String modelId) {
        byte[] bytes = repositoryService.getModelEditorSource(modelId);
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public IPage<ModelVO> listModels(String keyword, int pageNo, int pageSize) {
        ModelQuery query = repositoryService.createModelQuery()
                .modelTenantId(tenantHolder.getCurrentTenantId())
                .orderByLastUpdateTime().desc();
        if (oConvertUtils.isNotEmpty(keyword)) {
            query.modelNameLike("%" + keyword + "%");
        }
        long total = query.count();
        int offset = Math.max(0, (pageNo - 1)) * pageSize;
        List<Model> rows = query.listPage(offset, pageSize);
        List<ModelVO> items = new ArrayList<>(rows.size());
        for (Model m : rows) {
            items.add(toVO(m));
        }
        Page<ModelVO> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(items);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployModel(String modelId) {
        Model model = repositoryService.getModel(modelId);
        if (model == null) {
            throw new FlowableException("模型不存在: " + modelId);
        }
        assertTenant(model.getTenantId());

        byte[] source = repositoryService.getModelEditorSource(modelId);
        if (source == null || source.length == 0) {
            throw new FlowableException("模型尚未保存内容，无法部署");
        }
        // 校验 BPMN XML 合法性
        BpmnXMLConverter converter = new BpmnXMLConverter();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(source));
            BpmnModel bpmnModel = converter.convertToBpmnModel(reader);
            if (bpmnModel.getProcesses().isEmpty()) {
                throw new FlowableException("BPMN 未定义 process 节点");
            }
        } catch (FlowableException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowableException("BPMN 解析失败: " + e.getMessage(), e);
        }

        String resourceName = model.getKey() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(model.getName())
                .key(model.getKey())
                .category(model.getCategory())
                .tenantId(tenantHolder.getCurrentTenantId())
                .addBytes(resourceName, source)
                .deploy();

        // 把部署 ID 写回模型
        model.setDeploymentId(deployment.getId());
        repositoryService.saveModel(model);

        log.info("[flowable] 模型部署成功 modelId={} deploymentId={}", modelId, deployment.getId());
        return deployment.getId();
    }

    private ModelVO toVO(Model m) {
        ModelVO vo = new ModelVO();
        vo.setId(m.getId());
        vo.setKey(m.getKey());
        vo.setName(m.getName());
        vo.setCategory(m.getCategory());
        vo.setVersion(m.getVersion());
        vo.setCreateTime(m.getCreateTime());
        vo.setLastUpdateTime(m.getLastUpdateTime());
        vo.setTenantId(m.getTenantId());
        vo.setHasDeployment(m.getDeploymentId() != null);
        return vo;
    }

    private void assertTenant(String tenantId) {
        String current = tenantHolder.getCurrentTenantId();
        if (tenantId != null && !tenantId.isEmpty() && !tenantId.equals(current)) {
            throw new FlowableException("无权操作其他租户资源");
        }
    }
}
