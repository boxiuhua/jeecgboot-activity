-- author:baixiuhua---date:20260417--for:工作流管理菜单初始化
-- 父菜单：工作流管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000001', '0', '工作流管理', '/flowable', 'layouts/RouteView', 1, 'FlowableIndex', NULL, 0, NULL, NULL, 2.9, 1, 'ant-design:partition-outlined', 0, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 一级菜单：模型管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000010', 'FLW1000000000000001', '模型管理', '/flowable/model', 'flowable/model/index', 1, 'FlowableModelList', NULL, 1, NULL, NULL, 1, 0, 'ant-design:appstore-outlined', 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 设计器 (隐藏，通过模型列表跳转)
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000011', 'FLW1000000000000001', '流程设计器', '/flowable/modeler', 'flowable/modeler/index', 1, 'FlowableModeler', NULL, 1, NULL, NULL, 99, 0, NULL, 1, 0, 1, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 流程定义
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000020', 'FLW1000000000000001', '已部署流程', '/flowable/definition', 'flowable/definition/index', 1, 'FlowableDefinitionList', NULL, 1, NULL, NULL, 2, 0, 'ant-design:deployment-unit-outlined', 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 我发起的
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000030', 'FLW1000000000000001', '我发起的', '/flowable/process/my', 'flowable/process/my', 1, 'FlowableProcessMy', NULL, 1, NULL, NULL, 3, 0, 'ant-design:play-circle-outlined', 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 发起流程
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000031', 'FLW1000000000000001', '发起流程', '/flowable/process/start', 'flowable/process/start', 1, 'FlowableProcessStart', NULL, 1, NULL, NULL, 99, 0, NULL, 1, 0, 1, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 我的待办
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000040', 'FLW1000000000000001', '我的待办', '/flowable/task/todo', 'flowable/task/todo', 1, 'FlowableTaskTodo', NULL, 1, NULL, NULL, 4, 0, 'ant-design:schedule-outlined', 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 我的已办
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000041', 'FLW1000000000000001', '我的已办', '/flowable/task/done', 'flowable/task/done', 1, 'FlowableTaskDone', NULL, 1, NULL, NULL, 5, 0, 'ant-design:check-square-outlined', 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 流转追踪（隐藏）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000050', 'FLW1000000000000001', '流转追踪', '/flowable/history/trace', 'flowable/history/trace', 1, 'FlowableTrace', NULL, 1, NULL, NULL, 99, 0, NULL, 1, 0, 1, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);

-- 按钮权限（以模型为例）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('FLW1000000000000101', 'FLW1000000000000010', '模型-创建', NULL, NULL, 0, NULL, NULL, 2, 'flowable:model:create', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000102', 'FLW1000000000000010', '模型-编辑', NULL, NULL, 0, NULL, NULL, 2, 'flowable:model:update', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000103', 'FLW1000000000000010', '模型-删除', NULL, NULL, 0, NULL, NULL, 2, 'flowable:model:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000104', 'FLW1000000000000010', '模型-部署', NULL, NULL, 0, NULL, NULL, 2, 'flowable:model:deploy', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000201', 'FLW1000000000000020', '定义-挂起激活', NULL, NULL, 0, NULL, NULL, 2, 'flowable:definition:toggle', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000202', 'FLW1000000000000020', '定义-删除部署', NULL, NULL, 0, NULL, NULL, 2, 'flowable:definition:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000401', 'FLW1000000000000040', '任务-审批', NULL, NULL, 0, NULL, NULL, 2, 'flowable:task:approve', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000402', 'FLW1000000000000040', '任务-驳回', NULL, NULL, 0, NULL, NULL, 2, 'flowable:task:reject', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0),
       ('FLW1000000000000403', 'FLW1000000000000040', '任务-委派', NULL, NULL, 0, NULL, NULL, 2, 'flowable:task:delegate', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), 0, 0, '1', 0);
