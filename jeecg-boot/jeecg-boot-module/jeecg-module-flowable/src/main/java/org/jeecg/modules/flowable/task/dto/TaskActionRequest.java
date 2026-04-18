package org.jeecg.modules.flowable.task.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskActionRequest {
    /** 审批意见 */
    private String comment;
    /** 流程变量 */
    private Map<String, Object> variables;
    /** 委派/加签/转办目标用户 */
    private String targetUser;
    /** 加签位置 before/after */
    private String position;
}
