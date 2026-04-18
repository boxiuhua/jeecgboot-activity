package org.jeecg.modules.flowable.model.vo;

import lombok.Data;

@Data
public class ModelUpdateRequest {
    private String name;
    private String category;
    private String description;
    /** BPMN XML 内容 */
    private String bpmnXml;
    /** 预览用 SVG */
    private String svg;
}
