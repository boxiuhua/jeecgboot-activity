package org.jeecg.modules.flowable.definition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.flowable.definition.service.IFlowableDefinitionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Tag(name = "工作流-流程定义")
@RestController
@RequestMapping("/flowable/definition")
@RequiredArgsConstructor
public class FlowableDefinitionController {

    private final IFlowableDefinitionService definitionService;

    @Operation(summary = "流程定义列表")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.OK(definitionService.listDefinitions(keyword, pageNo, pageSize));
    }

    @Operation(summary = "挂起流程定义")
    @PutMapping("/suspend/{id}")
    public Result<?> suspend(@PathVariable("id") String id) {
        definitionService.suspend(id);
        return Result.ok("已挂起");
    }

    @Operation(summary = "激活流程定义")
    @PutMapping("/activate/{id}")
    public Result<?> activate(@PathVariable("id") String id) {
        definitionService.activate(id);
        return Result.ok("已激活");
    }

    @Operation(summary = "删除部署")
    @DeleteMapping("/delete/{deploymentId}")
    public Result<?> delete(@PathVariable("deploymentId") String deploymentId,
                            @RequestParam(value = "cascade", defaultValue = "true") boolean cascade) {
        definitionService.deleteDeployment(deploymentId, cascade);
        return Result.ok("删除成功");
    }

    @Operation(summary = "查看部署后的 BPMN XML")
    @GetMapping("/xml/{id}")
    public Result<String> xml(@PathVariable("id") String id) {
        return Result.OK(definitionService.getBpmnXml(id));
    }

    @Operation(summary = "下载流程图片（PNG）")
    @GetMapping("/diagram/{id}")
    public void diagram(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        byte[] bytes = definitionService.getDiagram(id);
        response.setContentType("image/png");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
