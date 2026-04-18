package org.jeecg.modules.flowable.runtime.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProcessStartRequest {
    /** 流程定义 Key */
    private String processDefinitionKey;
    /** 业务 Key（业务表的主键或单据号） */
    private String businessKey;
    /** 业务标题 */
    private String businessTitle;
    /** 流程变量 */
    private Map<String, Object> variables;
}
