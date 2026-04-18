package org.jeecg.modules.flowable.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ModelVO {
    private String id;
    private String name;
    private String key;
    private String category;
    private Integer version;
    private Date createTime;
    private Date lastUpdateTime;
    private String tenantId;
    private String description;
    private Boolean hasDeployment;
}
