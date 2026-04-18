<template>
  <div class="p-4 bg-white">
    <a-form layout="inline" class="mb-4">
      <a-form-item label="关键字">
        <a-input v-model:value="keyword" placeholder="名称搜索" @pressEnter="loadData" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button class="ml-2" @click="openCreate">新建模型</a-button>
        <a-button class="ml-2" @click="syncIdentity">同步用户/角色</a-button>
      </a-form-item>
    </a-form>

    <a-table :data-source="list" :columns="columns" :pagination="pagination" :loading="loading" row-key="id" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="openDesigner(record)">设计</a>
          <a-divider type="vertical" />
          <a @click="deploy(record)">部署</a>
          <a-divider type="vertical" />
          <a-popconfirm title="确认删除？" @confirm="del(record)">
            <a class="text-red-500">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="createVisible" title="新建流程模型" @ok="doCreate">
      <a-form layout="vertical" :model="createForm">
        <a-form-item label="流程 Key" required>
          <a-input v-model:value="createForm.key" placeholder="英文字母，唯一" />
        </a-form-item>
        <a-form-item label="流程名称" required>
          <a-input v-model:value="createForm.name" />
        </a-form-item>
        <a-form-item label="分类">
          <a-input v-model:value="createForm.category" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="createForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { message, Modal } from 'ant-design-vue';
  import { modelList, modelCreate, modelDelete, modelDeploy, identitySync } from '/@/api/flowable';

  const router = useRouter();
  const keyword = ref('');
  const loading = ref(false);
  const list = ref<any[]>([]);
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

  const createVisible = ref(false);
  const createForm = reactive({ key: '', name: '', category: '', description: '' });

  const columns = [
    { title: '名称', dataIndex: 'name' },
    { title: 'Key', dataIndex: 'key' },
    { title: '分类', dataIndex: 'category' },
    { title: '版本', dataIndex: 'version' },
    { title: '最后修改', dataIndex: 'lastUpdateTime' },
    { title: '已部署', dataIndex: 'hasDeployment', customRender: ({ value }: any) => (value ? '是' : '否') },
    { title: '操作', key: 'action' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res: any = await modelList({
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

  function openCreate() {
    Object.assign(createForm, { key: '', name: '', category: '', description: '' });
    createVisible.value = true;
  }

  async function doCreate() {
    if (!createForm.key || !createForm.name) {
      message.warning('请填写 Key 和 名称');
      return;
    }
    await modelCreate(createForm);
    createVisible.value = false;
    message.success('创建成功');
    loadData();
  }

  function openDesigner(record: any) {
    router.push({ path: '/flowable/modeler', query: { id: record.id } });
  }

  function deploy(record: any) {
    Modal.confirm({
      title: '确认部署此模型？',
      onOk: async () => {
        await modelDeploy(record.id);
        message.success('部署成功');
        loadData();
      },
    });
  }

  async function del(record: any) {
    await modelDelete(record.id);
    message.success('删除成功');
    loadData();
  }

  async function syncIdentity() {
    const res: any = await identitySync();
    message.success(`同步完成 users=${res.userCount}`);
  }

  onMounted(loadData);
</script>
