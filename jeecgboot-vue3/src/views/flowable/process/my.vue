<template>
  <div class="p-4 bg-white">
    <a-table :data-source="list" :columns="columns" :pagination="pagination" :loading="loading" row-key="id" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'RUNNING' ? 'blue' : 'default'">{{ record.status }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="viewTrace(record)">流转追踪</a>
          <a-divider type="vertical" />
          <a-popconfirm v-if="record.status === 'RUNNING'" title="确认作废？" @confirm="cancel(record)">
            <a class="text-red-500">作废</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { processMy, processCancel } from '/@/api/flowable';

  const router = useRouter();
  const loading = ref(false);
  const list = ref<any[]>([]);
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

  const columns = [
    { title: '流程', dataIndex: 'processDefinitionName' },
    { title: '业务', dataIndex: 'name' },
    { title: '业务 Key', dataIndex: 'businessKey' },
    { title: '开始', dataIndex: 'startTime' },
    { title: '结束', dataIndex: 'endTime' },
    { title: '状态', key: 'status' },
    { title: '操作', key: 'action' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res: any = await processMy({ pageNo: pagination.current, pageSize: pagination.pageSize });
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

  function viewTrace(record: any) {
    router.push({ path: '/flowable/history/trace', query: { instanceId: record.id } });
  }

  async function cancel(record: any) {
    await processCancel(record.id, '发起人作废');
    message.success('已作废');
    loadData();
  }

  onMounted(loadData);
</script>
