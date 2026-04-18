package org.jeecg.modules.flowable.history.service;

import java.util.List;
import java.util.Map;

public interface IFlowableHistoryService {

    /** 审批历史轨迹（节点+评论+时间）。 */
    List<Map<String, Object>> instanceHistory(String instanceId);

    /** 带高亮的流程图。 */
    byte[] diagramWithHighlight(String instanceId);
}
