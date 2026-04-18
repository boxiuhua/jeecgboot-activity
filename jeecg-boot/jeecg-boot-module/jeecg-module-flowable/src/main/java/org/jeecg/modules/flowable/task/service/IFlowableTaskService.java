package org.jeecg.modules.flowable.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.flowable.task.dto.TaskActionRequest;

import java.util.Map;

public interface IFlowableTaskService {

    IPage<Map<String, Object>> todo(int pageNo, int pageSize);

    IPage<Map<String, Object>> done(int pageNo, int pageSize);

    void complete(String taskId, TaskActionRequest request);

    void reject(String taskId, TaskActionRequest request);

    void claim(String taskId);

    void delegate(String taskId, TaskActionRequest request);

    void addSign(String taskId, TaskActionRequest request);
}
