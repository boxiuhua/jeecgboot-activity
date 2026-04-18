<template>
  <div class="property-panel-wrap">
    <div v-if="!selected" class="empty">请选择一个节点查看属性</div>
    <a-form v-else layout="vertical" :model="form">
      <a-form-item label="节点 ID">
        <a-input v-model:value="form.id" disabled />
      </a-form-item>
      <a-form-item label="节点名称">
        <a-input v-model:value="form.name" @change="updateName" />
      </a-form-item>

      <template v-if="isUserTask">
        <a-divider orientation="left">审批人</a-divider>
        <a-form-item label="审批人类型">
          <a-select v-model:value="form.assigneeType" @change="updateAssigneeType">
            <a-select-option value="fixed">指定人</a-select-option>
            <a-select-option value="users">候选人</a-select-option>
            <a-select-option value="roles">候选组（角色）</a-select-option>
            <a-select-option value="depts">候选组（部门）</a-select-option>
            <a-select-option value="starter">发起人</a-select-option>
            <a-select-option value="leader">上级</a-select-option>
            <a-select-option value="deptLeader">部门领导</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="needAssigneeValue" label="值">
          <a-input
            v-model:value="form.assigneeValue"
            placeholder="用户名/角色编码/部门编码，逗号分隔"
            @change="updateAssigneeValue"
          />
          <div class="tip">用户可调用 JSelectUser 选择；角色可调用 JSelectRole 选择</div>
        </a-form-item>

        <a-divider orientation="left">表单</a-divider>
        <a-form-item label="表单类型">
          <a-radio-group v-model:value="form.formType" @change="updateFormType">
            <a-radio value="online">online 表单</a-radio>
            <a-radio value="route">外部路由</a-radio>
            <a-radio value="none">无表单</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="form.formType && form.formType !== 'none'" label="表单值">
          <a-input
            v-model:value="form.formValue"
            :placeholder="form.formType === 'online' ? 'online 表单 ID' : '/xxx/approval/:id'"
            @change="updateFormValue"
          />
        </a-form-item>
      </template>

      <template v-if="isProcess">
        <a-form-item label="流程 Key">
          <a-input v-model:value="form.id" disabled />
          <div class="tip">流程 key 在模型创建时指定，不可修改</div>
        </a-form-item>
      </template>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, watch } from 'vue';

  const FLOWABLE_NS = 'flowable';
  const NS_ASSIGNEE = FLOWABLE_NS + ':assignee';
  const NS_CANDIDATE_USERS = FLOWABLE_NS + ':candidateUsers';
  const NS_CANDIDATE_GROUPS = FLOWABLE_NS + ':candidateGroups';
  const NS_FORM_KEY = FLOWABLE_NS + ':formKey';
  const NS_ASSIGNEE_TYPE = FLOWABLE_NS + ':assigneeType';
  const NS_ASSIGNEE_VALUE = FLOWABLE_NS + ':assigneeValue';
  const NS_FORM_TYPE = FLOWABLE_NS + ':formType';
  const NS_FORM_VALUE = FLOWABLE_NS + ':formValue';

  const props = defineProps<{
    modeler: any;
    selected: any;
  }>();
  const emit = defineEmits<{ (e: 'change'): void }>();

  const form = reactive<any>({
    id: '',
    name: '',
    assigneeType: '',
    assigneeValue: '',
    formType: 'none',
    formValue: '',
  });

  const elementType = computed(() => props.selected?.type || '');
  const isUserTask = computed(() => elementType.value === 'bpmn:UserTask');
  const isProcess = computed(() => elementType.value === 'bpmn:Process');
  const needAssigneeValue = computed(() =>
    ['fixed', 'users', 'roles', 'depts'].includes(form.assigneeType)
  );

  watch(
    () => props.selected,
    (el) => {
      if (!el) return;
      form.id = el.id || el.businessObject?.id || '';
      form.name = el.businessObject?.name || '';
      const bo = el.businessObject || {};
      form.assigneeType =
        readNs(bo, NS_ASSIGNEE_TYPE) || detectAssigneeType(bo);
      form.assigneeValue = resolveAssigneeValue(bo);
      const formKey = readNs(bo, NS_FORM_KEY);
      form.formType = readNs(bo, NS_FORM_TYPE) || (formKey ? 'online' : 'none');
      form.formValue = readNs(bo, NS_FORM_VALUE) || formKey || '';
    },
    { immediate: true },
  );

  function readNs(bo: any, key: string): any {
    return bo?.get?.(key) ?? bo?.[key] ?? bo?.['$attrs']?.[key];
  }

  function detectAssigneeType(bo: any): string {
    if (readNs(bo, NS_ASSIGNEE)) return 'fixed';
    if (readNs(bo, NS_CANDIDATE_USERS)) return 'users';
    if (readNs(bo, NS_CANDIDATE_GROUPS)) return 'roles';
    return '';
  }

  function resolveAssigneeValue(bo: any): string {
    return (
      readNs(bo, NS_ASSIGNEE) ||
      readNs(bo, NS_CANDIDATE_USERS) ||
      readNs(bo, NS_CANDIDATE_GROUPS) ||
      ''
    );
  }

  function modeling() {
    return props.modeler.get('modeling');
  }

  function updateName() {
    modeling().updateProperties(props.selected, { name: form.name });
    emit('change');
  }

  function updateAssigneeType() {
    const prop: any = { [NS_ASSIGNEE_TYPE]: form.assigneeType };
    // 根据类型映射回 BPMN 原生字段
    clearAssigneeProps(prop);
    applyAssignee(prop);
    modeling().updateProperties(props.selected, prop);
    emit('change');
  }

  function updateAssigneeValue() {
    const prop: any = { [NS_ASSIGNEE_VALUE]: form.assigneeValue };
    clearAssigneeProps(prop);
    applyAssignee(prop);
    modeling().updateProperties(props.selected, prop);
    emit('change');
  }

  function clearAssigneeProps(prop: any) {
    prop[NS_ASSIGNEE] = null;
    prop[NS_CANDIDATE_USERS] = null;
    prop[NS_CANDIDATE_GROUPS] = null;
  }

  function applyAssignee(prop: any) {
    switch (form.assigneeType) {
      case 'fixed':
        prop[NS_ASSIGNEE] = form.assigneeValue;
        break;
      case 'users':
        prop[NS_CANDIDATE_USERS] = form.assigneeValue;
        break;
      case 'roles':
        prop[NS_CANDIDATE_GROUPS] = form.assigneeValue;
        break;
      case 'depts':
        prop[NS_CANDIDATE_GROUPS] = (form.assigneeValue || '')
          .split(',')
          .map((d: string) => (d.startsWith('DEPT_') ? d : `DEPT_${d}`))
          .join(',');
        break;
      case 'starter':
        prop[NS_ASSIGNEE] = '${INITIATOR}';
        break;
      case 'leader':
        prop[NS_ASSIGNEE] = '${getLeader(INITIATOR)}';
        break;
      case 'deptLeader':
        prop[NS_ASSIGNEE] = '${getDeptLeader(INITIATOR)}';
        break;
    }
  }

  function updateFormType() {
    const prop: any = { [NS_FORM_TYPE]: form.formType };
    if (form.formType === 'online') {
      prop[NS_FORM_KEY] = form.formValue;
    } else {
      prop[NS_FORM_KEY] = null;
    }
    modeling().updateProperties(props.selected, prop);
    emit('change');
  }

  function updateFormValue() {
    const prop: any = { [NS_FORM_VALUE]: form.formValue };
    if (form.formType === 'online') {
      prop[NS_FORM_KEY] = form.formValue;
    }
    modeling().updateProperties(props.selected, prop);
    emit('change');
  }
</script>

<style lang="less" scoped>
  .property-panel-wrap {
    padding: 12px 16px;
    .empty {
      color: #999;
      text-align: center;
      padding: 40px 0;
    }
    .tip {
      color: #888;
      font-size: 12px;
      margin-top: 4px;
    }
  }
</style>
