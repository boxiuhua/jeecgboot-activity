package org.jeecg.modules.flowable.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.flowable.task.dto.TaskActionRequest;
import org.jeecg.modules.flowable.task.service.IFlowableTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "工作流-任务中心")
@RestController
@RequestMapping("/flowable/task")
@RequiredArgsConstructor
public class FlowableTaskController {

    private final IFlowableTaskService taskService;

    @Operation(summary = "我的待办")
    @GetMapping("/todo")
    public Result<IPage<Map<String, Object>>> todo(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.OK(taskService.todo(pageNo, pageSize));
    }

    @Operation(summary = "我的已办")
    @GetMapping("/done")
    public Result<IPage<Map<String, Object>>> done(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.OK(taskService.done(pageNo, pageSize));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/complete/{taskId}")
    public Result<?> complete(@PathVariable("taskId") String taskId,
                              @RequestBody(required = false) TaskActionRequest request) {
        taskService.complete(taskId, request);
        return Result.ok("审批成功");
    }

    @Operation(summary = "驳回")
    @PostMapping("/reject/{taskId}")
    public Result<?> reject(@PathVariable("taskId") String taskId,
                            @RequestBody(required = false) TaskActionRequest request) {
        taskService.reject(taskId, request);
        return Result.ok("已驳回");
    }

    @Operation(summary = "签收")
    @PostMapping("/claim/{taskId}")
    public Result<?> claim(@PathVariable("taskId") String taskId) {
        taskService.claim(taskId);
        return Result.ok("签收成功");
    }

    @Operation(summary = "委派")
    @PostMapping("/delegate/{taskId}")
    public Result<?> delegate(@PathVariable("taskId") String taskId,
                              @RequestBody TaskActionRequest request) {
        taskService.delegate(taskId, request);
        return Result.ok("已委派");
    }

    @Operation(summary = "加签")
    @PostMapping("/addSign/{taskId}")
    public Result<?> addSign(@PathVariable("taskId") String taskId,
                             @RequestBody TaskActionRequest request) {
        taskService.addSign(taskId, request);
        return Result.ok("加签成功");
    }
}
