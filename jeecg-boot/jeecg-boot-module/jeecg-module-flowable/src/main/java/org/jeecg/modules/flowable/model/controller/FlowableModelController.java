package org.jeecg.modules.flowable.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.flowable.identity.JeecgIdentitySyncService;
import org.jeecg.modules.flowable.model.service.IFlowableModelService;
import org.jeecg.modules.flowable.model.vo.ModelCreateRequest;
import org.jeecg.modules.flowable.model.vo.ModelUpdateRequest;
import org.jeecg.modules.flowable.model.vo.ModelVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作流-模型管理")
@RestController
@RequestMapping("/flowable/model")
@RequiredArgsConstructor
public class FlowableModelController {

    private final IFlowableModelService modelService;
    private final JeecgIdentitySyncService identitySyncService;

    @Operation(summary = "创建空白模型")
    @PostMapping("/create")
    public Result<String> create(@RequestBody ModelCreateRequest request) {
        return Result.OK(modelService.createModel(request));
    }

    @Operation(summary = "保存模型 BPMN XML")
    @PutMapping("/update/{id}")
    public Result<?> update(@PathVariable("id") String id, @RequestBody ModelUpdateRequest request) {
        modelService.updateModel(id, request);
        return Result.ok("保存成功");
    }

    @Operation(summary = "模型详情")
    @GetMapping("/detail/{id}")
    public Result<ModelVO> detail(@PathVariable("id") String id) {
        return Result.OK(modelService.getModel(id));
    }

    @Operation(summary = "获取模型 BPMN XML")
    @GetMapping("/xml/{id}")
    public Result<String> xml(@PathVariable("id") String id) {
        return Result.OK(modelService.getModelXml(id));
    }

    @Operation(summary = "模型列表")
    @GetMapping("/list")
    public Result<IPage<ModelVO>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                       @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.OK(modelService.listModels(keyword, pageNo, pageSize));
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable("id") String id) {
        modelService.deleteModel(id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "部署模型")
    @PostMapping("/deploy/{id}")
    public Result<String> deploy(@PathVariable("id") String id) {
        return Result.OK(modelService.deployModel(id));
    }

    @Operation(summary = "手动触发身份同步")
    @PostMapping("/identity/sync")
    public Result<?> syncIdentity() {
        return Result.OK(identitySyncService.syncAll());
    }
}
