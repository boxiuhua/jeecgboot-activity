<template>
  <div class="p-4 bg-white">
    <a-form layout="inline" class="mb-4">
      <a-form-item label="关键字">
        <a-input v-model:value="keyword" placeholder="流程名称" @pressEnter="loadData" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="loadData">查询</a-button>
      </a-form-item>
    </a-form>

    <a-table :data-source="list" :columns="columns" :pagination="pagination" :loading="loading" row-key="id" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="start(record)">发起</a>
          <a-divider type="vertical" />
          <a v-if="!record.suspended" @click="toggle(record, 'suspend')">挂起</a>
          <a v-else @click="toggle(record, 'activate')">激活</a>
          <a-divider type="vertical" />
          <a-popconfirm title="确认删除部署？" @confirm="del(record)">
            <a class="text-red-500">删除</a>
          </a-popconfirm>
        </template>
        <template v-else-if="column.key === 'suspended'">
          <a-tag :color="record.suspended ? 'orange' : 'green'">{{ record.suspended ? '已挂起' : '运行中' }}</a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { definitionList, definitionSuspend, definitionActivate, definitionDelete } from '/@/api/flowable';

  const router = useRouter();
  const keyword = ref('');
  const loading = ref(false);
  const list = ref<any[]>([]);
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

  const columns = [
    { title: '流程名称', dataIndex: 'name' },
    { title: 'Key', dataIndex: 'key' },
    { title: '版本', dataIndex: 'version' },
    { title: '分类', dataIndex: 'category' },
    { title: '租户', dataIndex: 'tenantId' },
    { title: '状态', key: 'suspended' },
    { title: '部署 ID', dataIndex: 'deploymentId' },
    { title: '操作', key: 'action' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res: any = await definitionList({
        keyword: keyword.value,
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
      });
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

  function start(record: any) {
    router.push({ path: '/flowable/process/start', query: { key: record.key, name: record.name } });
  }

  async function toggle(record: any, action: string) {
    if (action === 'suspend') await definitionSuspend(record.id);
    else await definitionActivate(record.id);
    message.success('操作成功');
    loadData();
  }

  async function del(record: any) {
    await definitionDelete(record.deploymentId);
    message.success('删除成功');
    loadData();
  }

  onMounted(loadData);
</script>
