<template>
  <div class="flowable-modeler">
    <div class="page-header">
      <a-page-header :title="`流程设计器 - ${modelInfo?.name || ''}`" @back="goBack" />
    </div>
    <BpmnDesigner ref="designerRef" :initial-xml="initialXml" @save="onSave" @deploy="onDeploy" @auto-layout-persist="onAutoLayoutPersist" />
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message, Modal } from 'ant-design-vue';
  import BpmnDesigner from './components/BpmnDesigner.vue';
  import { modelDetail, modelXml, modelUpdate, modelDeploy } from '/@/api/flowable';

  const route = useRoute();
  const router = useRouter();
  const designerRef = ref<any>();
  const modelInfo = ref<any>(null);
  const initialXml = ref('');

  const modelId = route.query.id as string;

  onMounted(async () => {
    if (!modelId) return;
    const [detail, xml] = await Promise.all([modelDetail(modelId), modelXml(modelId)]);
    modelInfo.value = detail;
    initialXml.value = xml || '';
  });

  async function onSave(payload: { xml: string; svg: string }) {
    if (!modelId) return;
    await modelUpdate(modelId, {
      bpmnXml: payload.xml,
      svg: payload.svg,
    });
    message.success('保存成功');
  }

  /** 模型原 XML 缺 DI，设计器加载时自动补了布局；这里静默写回避免下次还得重来 */
  async function onAutoLayoutPersist(payload: { xml: string; svg: string }) {
    if (!modelId) return;
    try {
      await modelUpdate(modelId, { bpmnXml: payload.xml, svg: payload.svg }, { silent: true });
      console.info('[modeler] 已为无布局的历史 BPMN 自动补写 DI');
    } catch (e) {
      console.warn('[modeler] 自动补写 DI 失败，后续仍可点保存', e);
    }
  }

  function onDeploy(payload: { xml: string; svg: string }) {
    Modal.confirm({
      title: '确认部署？',
      content: '部署后会生成新版本的流程定义。',
      onOk: async () => {
        await modelUpdate(modelId, { bpmnXml: payload.xml, svg: payload.svg });
        const deployId = await modelDeploy(modelId);
        message.success(`部署成功，deploymentId=${deployId}`);
      },
    });
  }

  function goBack() {
    router.back();
  }
</script>

<style lang="less" scoped>
  .flowable-modeler {
    .page-header {
      background: #fff;
      margin-bottom: 4px;
    }
  }
</style>
