import type { AppRouteRecordRaw } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

export const AI_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'ai-parent',
  component: LAYOUT,
  meta: {
    title: 'ai',
  },
  children: [
    {
      path: '/ai',
      name: 'ai',
      component: () => import('/@/views/dashboard/ai/index.vue'),
      meta: {
        title: 'AI助手',
      },
    },
  ],
};

export const FLOWABLE_MODELER_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'FlowableModelerParent',
  component: LAYOUT,
  meta: {
    title: '流程设计器',
    hideMenu: true,
    hideBreadcrumb: true,
  },
  children: [
    {
      path: '/flowable/modeler',
      name: 'FlowableModeler',
      component: () => import('/@/views/flowable/modeler/index.vue'),
      meta: {
        title: '流程设计器',
        hideMenu: true,
        hideBreadcrumb: true,
      },
    },
  ],
};

export const staticRoutesList = [AI_ROUTE, FLOWABLE_MODELER_ROUTE];
