<template>
  <div class="p-4 bg-white">
    <a-table :data-source="list" :columns="columns" :pagination="pagination" :loading="loading" row-key="id" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="approve(record, true)">审批</a>
          <a-divider type="vertical" />
          <a @click="approve(record, false)">驳回</a>
          <a-divider type="vertical" />
          <a @click="claim(record)">签收</a>
          <a-divider type="vertical" />
          <a @click="delegate(record)">委派</a>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="approvalVisible" :title="approveOk ? '审批通过' : '驳回'" @ok="doApprove">
      <a-form layout="vertical" :model="approvalForm">
        <a-form-item label="意见">
          <a-textarea v-model:value="approvalForm.comment" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="delegateVisible" title="委派" @ok="doDelegate">
      <a-form layout="vertical" :model="delegateForm">
        <a-form-item label="目标用户">
          <a-input v-model:value="delegateForm.targetUser" placeholder="用户名" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, reactive } from 'vue';
  import { message } from 'ant-design-vue';
  import { taskTodo, taskComplete, taskReject, taskClaim, taskDelegate } from '/@/api/flowable';

  const loading = ref(false);
  const list = ref<any[]>([]);
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

  const approvalVisible = ref(false);
  const approveOk = ref(true);
  const approvalForm = reactive({ taskId: '', comment: '' });

  const delegateVisible = ref(false);
  const delegateForm = reactive({ taskId: '', targetUser: '' });

  const columns = [
    { title: '任务名', dataIndex: 'name' },
    { title: '处理人', dataIndex: 'assignee' },
    { title: '创建时间', dataIndex: 'createTime' },
    { title: '到期时间', dataIndex: 'dueDate' },
    { title: '优先级', dataIndex: 'priority' },
    { title: '操作', key: 'action' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res: any = await taskTodo({ pageNo: pagination.current, pageSize: pagination.pageSize });
      list.value = res.records || [];
      pagination.total = res.total || 0;
    } finally {
      loading.value = false;
    }
  }

  function handleTableChange(p: any) {
    pagination.current = p.current;
    pagination.pageSize = p.pageSize;
    loadData();
  }

  function approve(record: any, pass: boolean) {
    approvalForm.taskId = record.id;
    approvalForm.comment = '';
    approveOk.value = pass;
    approvalVisible.value = true;
  }

  async function doApprove() {
    if (approveOk.value) {
      await taskComplete(approvalForm.taskId, { comment: approvalForm.comment });
      message.success('审批完成');
    } else {
      await taskReject(approvalForm.taskId, { comment: approvalForm.comment });
      message.success('已驳回');
    }
    approvalVisible.value = false;
    loadData();
  }

  async function claim(record: any) {
    await taskClaim(record.id);
    message.success('签收成功');
    loadData();
  }

  function delegate(record: any) {
    delegateForm.taskId = record.id;
    delegateForm.targetUser = '';
    delegateVisible.value = true;
  }

  async function doDelegate() {
    await taskDelegate(delegateForm.taskId, { targetUser: delegateForm.targetUser });
    message.success('已委派');
    delegateVisible.value = false;
    loadData();
  }

  onMounted(loadData);
</script>
