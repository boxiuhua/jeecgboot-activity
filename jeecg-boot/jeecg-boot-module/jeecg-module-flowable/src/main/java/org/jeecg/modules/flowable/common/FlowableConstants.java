package org.jeecg.modules.flowable.common;

public final class FlowableConstants {

    private FlowableConstants() {
    }

    public static final String DEFAULT_TENANT_ID = "0";
    public static final String TENANT_HEADER = "X-Tenant-Id";

    public static final String ASSIGNEE_TYPE = "assigneeType";
    public static final String FORM_TYPE = "formType";
    public static final String FORM_VALUE = "formValue";

    public static final String ASSIGNEE_FIXED = "fixed";
    public static final String ASSIGNEE_USERS = "users";
    public static final String ASSIGNEE_ROLES = "roles";
    public static final String ASSIGNEE_DEPTS = "depts";
    public static final String ASSIGNEE_STARTER = "starter";
    public static final String ASSIGNEE_LEADER = "leader";
    public static final String ASSIGNEE_DEPT_LEADER = "deptLeader";

    public static final String FORM_ONLINE = "online";
    public static final String FORM_ROUTE = "route";
    public static final String FORM_NONE = "none";

    public static final String GROUP_TYPE_ROLE = "role";
    public static final String GROUP_TYPE_DEPT = "dept";
    public static final String DEPT_GROUP_PREFIX = "DEPT_";

    public static final String VAR_INITIATOR = "INITIATOR";
    public static final String VAR_BUSINESS_TITLE = "businessTitle";

    public static final String APPROVE_PASS = "pass";
    public static final String APPROVE_REJECT = "reject";
    public static final String APPROVE_COMMENT_TYPE = "approval";
}
