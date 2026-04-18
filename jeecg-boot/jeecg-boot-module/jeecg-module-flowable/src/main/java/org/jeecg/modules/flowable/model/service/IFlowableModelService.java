package org.jeecg.modules.flowable.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.flowable.model.vo.ModelCreateRequest;
import org.jeecg.modules.flowable.model.vo.ModelUpdateRequest;
import org.jeecg.modules.flowable.model.vo.ModelVO;

public interface IFlowableModelService {

    String createModel(ModelCreateRequest request);

    void updateModel(String modelId, ModelUpdateRequest request);

    void deleteModel(String modelId);

    ModelVO getModel(String modelId);

    /** 返回 BPMN XML */
    String getModelXml(String modelId);

    IPage<ModelVO> listModels(String keyword, int pageNo, int pageSize);

    /** 部署模型为流程定义，返回 deploymentId */
    String deployModel(String modelId);
}
