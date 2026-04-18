<template>
  <div class="p-4 bg-white trace-page">
    <a-page-header title="流程追踪" @back="() => router.back()" />
    <div class="trace-body">
      <div class="diagram-wrap">
        <img v-if="diagramUrl" :src="diagramUrl" alt="流程图" />
        <a-empty v-else :description="diagramError || '流程图加载中...'" />
      </div>
      <div class="history-wrap">
        <a-timeline>
          <a-timeline-item v-for="item in history" :key="item.taskId">
            <template #dot>
              <CheckCircleOutlined v-if="item.endTime" style="color: #52c41a" />
              <ClockCircleOutlined v-else style="color: #faad14" />
            </template>
            <div class="task-title">{{ item.taskName }}</div>
            <div class="task-meta">处理人：{{ item.assignee || '-' }}</div>
            <div class="task-meta">开始：{{ item.startTime }}</div>
            <div class="task-meta" v-if="item.endTime">结束：{{ item.endTime }}（耗时 {{ item.durationInMillis }} ms）</div>
            <div v-if="item.comments?.length" class="task-comments">
              <div v-for="c in item.comments" :key="c.time" class="comment">
                <b>{{ c.userId }}</b>（{{ c.type }}）：{{ c.message }}
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, onBeforeUnmount } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { Timeline, TimelineItem } from 'ant-design-vue';
  import { CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons-vue';
  import { historyInstance, historyDiagram } from '/@/api/flowable';

  const ATimeline = Timeline;
  const ATimelineItem = TimelineItem;

  const route = useRoute();
  const router = useRouter();
  const history = ref<any[]>([]);
  const diagramUrl = ref<string>('');
  const diagramError = ref<string>('');

  onMounted(async () => {
    const instanceId = route.query.instanceId as string;
    if (!instanceId) return;
    history.value = (await historyInstance(instanceId)) as any[];
    // 后端 diagram 接口需要 X-Access-Token，<img> 标签不会带 header。
    // 因此走 defHttp 下载 blob，再用 URL.createObjectURL 挂到 src。
    try {
      const blob = (await historyDiagram(instanceId)) as unknown as Blob;
      // 后端异常时会把 JSON 错误响应当 blob 返回，判一下类型
      if (blob && blob.type.startsWith('image/')) {
        diagramUrl.value = URL.createObjectURL(blob);
      } else {
        const text = blob ? await blob.text() : '';
        try {
          const j = JSON.parse(text);
          diagramError.value = j.message || '流程图生成失败';
        } catch {
          diagramError.value = '流程图生成失败';
        }
        console.warn('[trace] 流程图接口未返回图像:', text.slice(0, 200));
      }
    } catch (e: any) {
      diagramError.value = e?.message || '流程图下载失败';
      console.warn('[trace] 下载流程图失败', e);
    }
  });

  onBeforeUnmount(() => {
    if (diagramUrl.value.startsWith('blob:')) URL.revokeObjectURL(diagramUrl.value);
  });
</script>

<style lang="less" scoped>
  .trace-page {
    .trace-body {
      display: flex;
      gap: 24px;
      .diagram-wrap {
        flex: 1;
        min-height: 400px;
        border: 1px solid #e8e8e8;
        padding: 12px;
        img {
          max-width: 100%;
        }
      }
      .history-wrap {
        width: 400px;
        border: 1px solid #e8e8e8;
        padding: 16px;
        overflow-y: auto;
        max-height: 600px;
        .task-title {
          font-weight: 600;
        }
        .task-meta {
          color: #666;
          font-size: 12px;
        }
        .task-comments {
          margin-top: 4px;
          .comment {
            background: #fafafa;
            padding: 4px 8px;
            border-radius: 4px;
            margin-top: 4px;
          }
        }
      }
    }
  }
</style>
