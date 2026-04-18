package org.jeecg.modules.flowable.history.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.flowable.history.service.IFlowableHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "工作流-历史与流程图")
@RestController
@RequestMapping("/flowable/history")
@RequiredArgsConstructor
public class FlowableHistoryController {

    private final IFlowableHistoryService historyService;

    @Operation(summary = "实例审批历史")
    @GetMapping("/instance/{instanceId}")
    public Result<List<Map<String, Object>>> instance(@PathVariable("instanceId") String instanceId) {
        return Result.OK(historyService.instanceHistory(instanceId));
    }

    @Operation(summary = "带高亮的流程图 PNG")
    @GetMapping("/diagram/{instanceId}")
    public void diagram(@PathVariable("instanceId") String instanceId, HttpServletResponse response) throws IOException {
        byte[] bytes = historyService.diagramWithHighlight(instanceId);
        response.setContentType("image/png");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
