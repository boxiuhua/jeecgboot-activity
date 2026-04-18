<template>
  <div class="p-4 bg-white">
    <a-table :data-source="list" :columns="columns" :pagination="pagination" :loading="loading" row-key="id" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="viewTrace(record)">流转追踪</a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { taskDone } from '/@/api/flowable';

  const router = useRouter();
  const loading = ref(false);
  const list = ref<any[]>([]);
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

  const columns = [
    { title: '任务名', dataIndex: 'name' },
    { title: '处理人', dataIndex: 'assignee' },
    { title: '开始', dataIndex: 'startTime' },
    { title: '结束', dataIndex: 'endTime' },
    { title: '耗时(ms)', dataIndex: 'durationInMillis' },
    { title: '操作', key: 'action' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res: any = await taskDone({ pageNo: pagination.current, pageSize: pagination.pageSize });
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
    router.push({ path: '/flowable/history/trace', query: { instanceId: record.processInstanceId } });
  }

  onMounted(loadData);
</script>
