<template>
  <div class="p-4 bg-white">
    <a-page-header title="发起流程" @back="() => router.back()" />
    <a-form layout="vertical" :model="form" class="max-w-2xl">
      <a-form-item label="流程 Key" required>
        <a-input v-model:value="form.processDefinitionKey" />
      </a-form-item>
      <a-form-item label="业务 Key">
        <a-input v-model:value="form.businessKey" placeholder="业务单据号（可选）" />
      </a-form-item>
      <a-form-item label="业务标题">
        <a-input v-model:value="form.businessTitle" />
      </a-form-item>
      <a-form-item label="流程变量（JSON）">
        <a-textarea v-model:value="varsJson" :rows="6" placeholder='{ "amount": 100 }' />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="submit">发起</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { processStart } from '/@/api/flowable';

  const route = useRoute();
  const router = useRouter();
  const form = reactive({ processDefinitionKey: '', businessKey: '', businessTitle: '' });
  const varsJson = ref('{}');

  onMounted(() => {
    if (route.query.key) form.processDefinitionKey = route.query.key as string;
    if (route.query.name) form.businessTitle = route.query.name as string;
  });

  async function submit() {
    if (!form.processDefinitionKey) {
      message.warning('请填写流程 Key');
      return;
    }
    let variables: any = {};
    try {
      variables = JSON.parse(varsJson.value || '{}');
    } catch (e) {
      message.error('流程变量 JSON 格式错误');
      return;
    }
    const instanceId = await processStart({ ...form, variables });
    message.success(`发起成功，instanceId=${instanceId}`);
    router.push('/flowable/process/my');
  }
</script>
