<template>
  <div class="bpmn-designer">
    <div class="toolbar">
      <a-space>
        <a-button type="primary" @click="emitSave">保存</a-button>
        <a-button @click="emitDeploy">部署</a-button>
        <a-button @click="triggerImport">导入 XML</a-button>
        <a-button @click="downloadXml">导出 XML</a-button>
        <a-button @click="downloadSvg">导出 SVG</a-button>
        <a-button @click="zoomReset">适应画布</a-button>
      </a-space>
      <input ref="fileInputRef" type="file" accept=".bpmn,.xml" style="display:none" @change="handleImportFile" />
    </div>
    <div class="body">
      <div ref="canvasRef" class="canvas"></div>
      <div class="property-panel">
        <PropertyPanel :modeler="modeler" :selected="selectedElement" @change="onPropertyChange" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
  import BpmnModeler from 'bpmn-js/lib/Modeler';
  import PropertyPanel from './PropertyPanel.vue';
  import { flowableModdleDescriptor } from './flowableModdle';

  const props = defineProps<{ initialXml?: string }>();
  const emit = defineEmits<{
    (e: 'save', payload: { xml: string; svg: string }): void;
    (e: 'deploy', payload: { xml: string; svg: string }): void;
    /** 加载时对无 DI 的 XML 自动补了布局，通知父组件静默回写到后端 */
    (e: 'autoLayoutPersist', payload: { xml: string; svg: string }): void;
  }>();

  const canvasRef = ref<HTMLDivElement>();
  const fileInputRef = ref<HTMLInputElement>();
  const modeler = shallowRef<any>(null);
  const selectedElement = ref<any>(null);

  const DEFAULT_XML = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             targetNamespace="http://www.jeecg.com/bpmn">
  <process id="Process_1" name="未命名流程" isExecutable="true">
    <startEvent id="StartEvent_1" name="开始"/>
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <omgdc:Bounds x="172" y="180" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;

  onMounted(() => {
    modeler.value = new BpmnModeler({
      container: canvasRef.value,
      keyboard: { bindTo: document },
      moddleExtensions: {
        flowable: flowableModdleDescriptor,
      },
    });

    modeler.value.on('selection.changed', (e: any) => {
      selectedElement.value = e.newSelection?.[0] || null;
    });
    modeler.value.on('element.changed', (e: any) => {
      if (selectedElement.value && e.element && e.element.id === selectedElement.value.id) {
        selectedElement.value = e.element;
      }
    });

    loadXml(props.initialXml);
  });

  // 父组件异步拿到后端 XML 后会更新 initialXml,子组件 onMounted 早于父组件 onMounted 执行,
  // 所以必须 watch 才能把后端的 XML 渲染到画布上
  watch(
    () => props.initialXml,
    (xml) => {
      if (!modeler.value) return;
      loadXml(xml);
    },
  );

  /**
   * 为只有语义、缺失 <bpmndi:BPMNDiagram> 布局的 BPMN 2.0 XML 生成一份"垂直堆叠"布局。
   * 没有用 bpmn-auto-layout：它在 1.3 版本只生成 BPMNShape、不生成 BPMNEdge，
   * 导致 bpmn-js importXML 时把无 edge 的 sequenceFlow 丢弃，saveXML 再写回就变成三节点无连接，
   * 部署出去的流程直接空跑（不产生 user task）。
   *
   * 策略：扫描 process 下的 flowNode 与 sequenceFlow，顺序（从 start 沿 sequenceFlow 拓扑前进）
   * 给每个节点挂一个向下的 Bounds，并为每条 sequenceFlow 生成直线 BPMNEdge。
   */
  function autoLayout(xml: string): string {
    // 提取节点
    const nodeRe = /<(startEvent|endEvent|userTask|serviceTask|scriptTask|exclusiveGateway|parallelGateway|intermediateThrowEvent|intermediateCatchEvent|task)\b[^>]*id="([^"]+)"[^>]*\/?>/g;
    const NODE_SIZE: Record<string, { w: number; h: number }> = {
      startEvent: { w: 36, h: 36 },
      endEvent: { w: 36, h: 36 },
      exclusiveGateway: { w: 50, h: 50 },
      parallelGateway: { w: 50, h: 50 },
      intermediateThrowEvent: { w: 36, h: 36 },
      intermediateCatchEvent: { w: 36, h: 36 },
    };
    const defaultSize = { w: 100, h: 80 };
    const nodes: { id: string; type: string; w: number; h: number }[] = [];
    let m: RegExpExecArray | null;
    while ((m = nodeRe.exec(xml)) !== null) {
      const type = m[1];
      const id = m[2];
      const size = NODE_SIZE[type] || defaultSize;
      nodes.push({ id, type, w: size.w, h: size.h });
    }
    if (nodes.length === 0) return xml;
    // 提取 sequenceFlow
    const flowRe = /<sequenceFlow\b[^>]*id="([^"]+)"[^>]*sourceRef="([^"]+)"[^>]*targetRef="([^"]+)"/g;
    const flows: { id: string; src: string; tgt: string }[] = [];
    while ((m = flowRe.exec(xml)) !== null) flows.push({ id: m[1], src: m[2], tgt: m[3] });

    // 拓扑排序（以 startEvent 为起点，走 sequenceFlow）
    const adj: Record<string, string[]> = {};
    for (const f of flows) (adj[f.src] ||= []).push(f.tgt);
    const order: string[] = [];
    const visited = new Set<string>();
    const start = nodes.find((n) => n.type === 'startEvent')?.id ?? nodes[0].id;
    (function dfs(id: string) {
      if (visited.has(id)) return;
      visited.add(id);
      order.push(id);
      for (const n of adj[id] || []) dfs(n);
    })(start);
    for (const n of nodes) if (!visited.has(n.id)) order.push(n.id); // 孤岛节点兜底

    // 按 order 从上往下排
    const PAD = 60;
    const CX = 180;
    const bounds: Record<string, { x: number; y: number; w: number; h: number; cx: number; cy: number }> = {};
    let y = 40;
    for (const id of order) {
      const node = nodes.find((n) => n.id === id);
      if (!node) continue;
      const x = CX - node.w / 2;
      bounds[id] = { x, y, w: node.w, h: node.h, cx: CX, cy: y + node.h / 2 };
      y += node.h + PAD;
    }

    // 生成 BPMNDiagram
    const shapeXml = nodes
      .filter((n) => bounds[n.id])
      .map((n) => {
        const b = bounds[n.id];
        return `      <bpmndi:BPMNShape id="${n.id}_di" bpmnElement="${n.id}"><omgdc:Bounds x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}"/></bpmndi:BPMNShape>`;
      })
      .join('\n');
    const edgeXml = flows
      .filter((f) => bounds[f.src] && bounds[f.tgt])
      .map((f) => {
        const a = bounds[f.src], b = bounds[f.tgt];
        return `      <bpmndi:BPMNEdge id="${f.id}_di" bpmnElement="${f.id}"><omgdi:waypoint x="${a.cx}" y="${a.y + a.h}"/><omgdi:waypoint x="${b.cx}" y="${b.y}"/></bpmndi:BPMNEdge>`;
      })
      .join('\n');
    const processIdMatch = xml.match(/<process\b[^>]*id="([^"]+)"/);
    const pid = processIdMatch?.[1] ?? 'Process_1';
    const diagram = `  <bpmndi:BPMNDiagram id="BPMNDiagram_${pid}">
    <bpmndi:BPMNPlane id="BPMNPlane_${pid}" bpmnElement="${pid}">
${shapeXml}
${edgeXml}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>`;

    // 确保根节点声明了 bpmndi / omgdc / omgdi 命名空间
    let out = xml;
    const nsDecls: [string, string][] = [
      ['bpmndi', 'http://www.omg.org/spec/BPMN/20100524/DI'],
      ['omgdc', 'http://www.omg.org/spec/DD/20100524/DC'],
      ['omgdi', 'http://www.omg.org/spec/DD/20100524/DI'],
    ];
    for (const [prefix, uri] of nsDecls) {
      if (!new RegExp(`xmlns:${prefix}\\s*=`).test(out)) {
        out = out.replace(/<definitions(\s)/, `<definitions xmlns:${prefix}="${uri}"$1`);
      }
    }
    // 把 diagram 塞到 </definitions> 前
    return out.replace(/<\/definitions>/, diagram + '\n</definitions>');
  }

  async function loadXml(xml: string | undefined) {
    let target = xml && xml.trim().length > 0 ? xml : DEFAULT_XML;
    // 通过 API / 历史部署回导的 BPMN 可能只有 <process> 语义而没有 <bpmndi:BPMNDiagram> 布局信息，
    // bpmn-js 渲染器会抛 "no diagram to display" 并把画布留空。这里自动补一份布局后再渲染。
    let autoLayoutApplied = false;
    if (!/BPMNDiagram/.test(target)) {
      try {
        target = autoLayout(target);
        autoLayoutApplied = true;
      } catch (err) {
        console.warn('[bpmn] auto-layout failed, fallback to DEFAULT_XML', err);
        target = DEFAULT_XML;
      }
    }
    try {
      await modeler.value.importXML(target);
      modeler.value.get('canvas').zoom('fit-viewport', 'auto');
      if (autoLayoutApplied) {
        // 把带 DI 的 XML 静默写回后端，避免 /flowable/history/diagram 因缺布局而 500
        try {
          emit('autoLayoutPersist', { xml: await exportXml(), svg: await exportSvg() });
        } catch (err) {
          console.warn('[bpmn] persist auto-layout failed', err);
        }
      }
    } catch (err) {
      console.error('[bpmn] import failed', err);
    }
  }

  onBeforeUnmount(() => {
    modeler.value?.destroy();
  });

  async function exportXml(): Promise<string> {
    const { xml } = await modeler.value.saveXML({ format: true });
    return xml;
  }

  async function exportSvg(): Promise<string> {
    const { svg } = await modeler.value.saveSVG();
    return svg;
  }

  async function emitSave() {
    emit('save', { xml: await exportXml(), svg: await exportSvg() });
  }
  async function emitDeploy() {
    emit('deploy', { xml: await exportXml(), svg: await exportSvg() });
  }

  function triggerImport() {
    fileInputRef.value?.click();
  }

  function handleImportFile(e: Event) {
    const target = e.target as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      modeler.value.importXML(reader.result as string);
    };
    reader.readAsText(file);
    target.value = '';
  }

  async function downloadXml() {
    const xml = await exportXml();
    download(xml, 'process.bpmn', 'application/xml');
  }

  async function downloadSvg() {
    const svg = await exportSvg();
    download(svg, 'process.svg', 'image/svg+xml');
  }

  function download(data: string, filename: string, mime: string) {
    const blob = new Blob([data], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  function zoomReset() {
    modeler.value.get('canvas').zoom('fit-viewport', 'auto');
  }

  function onPropertyChange() {
    // 属性面板通过 modelerInstance.get('modeling') 直接改，画布会自己刷新
  }

  async function importXml(xml: string) {
    await modeler.value.importXML(xml);
  }
  async function getXml(): Promise<string> {
    return exportXml();
  }
  async function getSvg(): Promise<string> {
    return exportSvg();
  }
  defineExpose({ importXml, getXml, getSvg });
</script>

<style lang="less" scoped>
  .bpmn-designer {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 160px);
    background: #fff;
    .toolbar {
      padding: 8px 12px;
      border-bottom: 1px solid #e8e8e8;
    }
    .body {
      flex: 1;
      display: flex;
      overflow: hidden;
      .canvas {
        flex: 1;
        height: 100%;
        background: #fafafa;
      }
      .property-panel {
        width: 320px;
        border-left: 1px solid #e8e8e8;
        background: #fff;
        overflow-y: auto;
      }
    }
  }
</style>

<style>
  @import 'bpmn-js/dist/assets/diagram-js.css';
  @import 'bpmn-js/dist/assets/bpmn-js.css';
  @import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
</style>
