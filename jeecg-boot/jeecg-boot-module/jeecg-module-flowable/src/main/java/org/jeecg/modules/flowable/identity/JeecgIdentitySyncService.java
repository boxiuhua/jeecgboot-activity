package org.jeecg.modules.flowable.identity;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.flowable.common.FlowableConstants;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 将 JeecgBoot 的 sys_user / sys_role / sys_depart 单向同步到 Flowable 的 ACT_ID_* 表。
 * <p>
 * 作为启动时全量同步 + 按需手动触发（管理页面按钮）的实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JeecgIdentitySyncService {

    private final IdentityService identityService;
    private final ISysBaseAPI sysBaseAPI;

    /** 全量同步用户 + 角色 + 部门。 */
    public SyncReport syncAll() {
        SyncReport report = new SyncReport();
        try {
            report.userCount = syncUsers();
        } catch (Exception e) {
            log.warn("[flowable] 同步用户失败: {}", e.getMessage(), e);
            report.errors.add("user: " + e.getMessage());
        }
        try {
            report.roleCount = syncRoles();
        } catch (Exception e) {
            log.warn("[flowable] 同步角色失败: {}", e.getMessage(), e);
            report.errors.add("role: " + e.getMessage());
        }
        try {
            report.deptCount = syncDepts();
        } catch (Exception e) {
            log.warn("[flowable] 同步部门失败: {}", e.getMessage(), e);
            report.errors.add("dept: " + e.getMessage());
        }
        log.info("[flowable] 身份同步完成: {}", report);
        return report;
    }

    public int syncUsers() {
        JSONObject page = sysBaseAPI.queryAllUser(null, 1, 10000);
        if (page == null || page.getJSONArray("records") == null) {
            return 0;
        }
        List<JSONObject> users = page.getJSONArray("records").toJavaList(JSONObject.class);
        for (JSONObject u : users) {
            String id = u.getString("username");
            if (id == null || id.isEmpty()) {
                continue;
            }
            User existing = identityService.createUserQuery().userId(id).singleResult();
            User target = existing != null ? existing : identityService.newUser(id);
            target.setFirstName(u.getString("realname"));
            target.setEmail(u.getString("email"));
            if (existing != null) {
                identityService.saveUser(target);
            } else {
                identityService.saveUser(target);
            }
        }
        return users.size();
    }

    public int syncRoles() {
        return syncGroupsByApi("ROLE");
    }

    public int syncDepts() {
        return syncGroupsByApi("DEPT");
    }

    /**
     * 轻量的组同步，依赖 ISysBaseAPI 暴露的扁平数据。
     * <p>
     * 实际项目可替换为直接读 sys_role / sys_depart。
     */
    private int syncGroupsByApi(String kind) {
        // 占位实现：由于 ISysBaseAPI 没有直接获取全量角色/部门的便捷方法，
        // 此处保留接口，运行时通过管理页面按需触发“刷新候选组”。
        // 建议在 jeecg-system-biz 补充 listAllRoles/listAllDeparts 方法后替换。
        log.info("[flowable] syncGroups kind={} 暂采用按需创建策略，新增流程时若候选组不存在会自动补齐", kind);
        return 0;
    }

    /**
     * 按需确保组存在（候选组/候选部门被引用但 ACT_ID_GROUP 还没有时调用）。
     */
    public void ensureGroup(String groupId, String type, String name) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        Group existing = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (existing == null) {
            Group group = identityService.newGroup(groupId);
            group.setName(name != null ? name : groupId);
            group.setType(type != null ? type : FlowableConstants.GROUP_TYPE_ROLE);
            identityService.saveGroup(group);
        }
    }

    /** 将用户与组建立关系（幂等）。 */
    public void bindUserToGroup(String userId, String groupId) {
        try {
            identityService.createMembership(userId, groupId);
        } catch (Exception e) {
            // 已存在则忽略
            log.debug("[flowable] bindUserToGroup 忽略: {}-{} {}", userId, groupId, e.getMessage());
        }
    }

    public static class SyncReport {
        public int userCount;
        public int roleCount;
        public int deptCount;
        public java.util.List<String> errors = new java.util.ArrayList<>();

        @Override
        public String toString() {
            return "users=" + userCount + ", roles=" + roleCount + ", depts=" + deptCount
                    + (errors.isEmpty() ? "" : ", errors=" + errors);
        }
    }
}
