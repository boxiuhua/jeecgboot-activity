package org.jeecg.modules.flowable.model.vo;

import lombok.Data;

@Data
public class ModelCreateRequest {
    private String key;
    private String name;
    private String category;
    private String description;
}
