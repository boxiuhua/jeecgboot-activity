package org.jeecg.modules.flowable.runtime.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.flowable.runtime.dto.ProcessStartRequest;

import java.util.Map;

public interface IFlowableRuntimeService {

    String start(ProcessStartRequest request);

    IPage<Map<String, Object>> listMyStarted(int pageNo, int pageSize);

    void cancel(String instanceId, String reason);
}
