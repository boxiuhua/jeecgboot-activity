package org.jeecg.modules.flowable.runtime.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.flowable.runtime.dto.ProcessStartRequest;
import org.jeecg.modules.flowable.runtime.service.IFlowableRuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "工作流-流程运行时")
@RestController
@RequestMapping("/flowable/process")
@RequiredArgsConstructor
public class FlowableRuntimeController {

    private final IFlowableRuntimeService runtimeService;

    @Operation(summary = "发起流程")
    @PostMapping("/start")
    public Result<String> start(@RequestBody ProcessStartRequest request) {
        return Result.OK(runtimeService.start(request));
    }

    @Operation(summary = "我发起的流程")
    @GetMapping("/my")
    public Result<IPage<Map<String, Object>>> my(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.OK(runtimeService.listMyStarted(pageNo, pageSize));
    }

    @Operation(summary = "作废流程实例")
    @PostMapping("/cancel/{instanceId}")
    public Result<?> cancel(@PathVariable("instanceId") String instanceId,
                            @RequestParam(value = "reason", required = false) String reason) {
        runtimeService.cancel(instanceId, reason);
        return Result.ok("已作废");
    }
}
