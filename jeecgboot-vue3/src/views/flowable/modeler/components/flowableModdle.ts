// Flowable 命名空间的 moddle 描述符，用于让 bpmn-js 正确识别并序列化 flowable:* 扩展属性
// 没有此描述符时，bpmn-js 会把 formKey、assignee 等当作裸属性写到 userTask 上，
// 导致 Flowable 引擎部署时 XSD 校验失败（formkey 不允许出现在 userTask 上）。
export const flowableModdleDescriptor = {
  name: 'Flowable',
  uri: 'http://flowable.org/bpmn',
  prefix: 'flowable',
  xml: { tagAlias: 'lowerCase' },
  associations: [],
  types: [
    {
      name: 'UserTask',
      isAbstract: true,
      extends: ['bpmn:UserTask'],
      properties: [
        { name: 'assignee', isAttr: true, type: 'String' },
        { name: 'candidateUsers', isAttr: true, type: 'String' },
        { name: 'candidateGroups', isAttr: true, type: 'String' },
        { name: 'dueDate', isAttr: true, type: 'String' },
        { name: 'priority', isAttr: true, type: 'String' },
        { name: 'formKey', isAttr: true, type: 'String' },
        { name: 'formFieldValidation', isAttr: true, type: 'Boolean' },
        // 下面是 JeecgBoot 自定义扩展属性
        { name: 'assigneeType', isAttr: true, type: 'String' },
        { name: 'assigneeValue', isAttr: true, type: 'String' },
        { name: 'formType', isAttr: true, type: 'String' },
        { name: 'formValue', isAttr: true, type: 'String' },
      ],
    },
    {
      name: 'Process',
      isAbstract: true,
      extends: ['bpmn:Process'],
      properties: [
        { name: 'candidateStarterUsers', isAttr: true, type: 'String' },
        { name: 'candidateStarterGroups', isAttr: true, type: 'String' },
        { name: 'versionTag', isAttr: true, type: 'String' },
      ],
    },
    {
      name: 'StartEvent',
      isAbstract: true,
      extends: ['bpmn:StartEvent'],
      properties: [
        // flowable 特有：发起人变量名（Jeecg 用 INITIATOR 存当前登录用户）
        { name: 'initiator', isAttr: true, type: 'String' },
        { name: 'formKey', isAttr: true, type: 'String' },
        { name: 'formFieldValidation', isAttr: true, type: 'Boolean' },
      ],
    },
    {
      name: 'ServiceTask',
      isAbstract: true,
      extends: ['bpmn:ServiceTask'],
      properties: [
        { name: 'class', isAttr: true, type: 'String' },
        { name: 'expression', isAttr: true, type: 'String' },
        { name: 'delegateExpression', isAttr: true, type: 'String' },
        { name: 'resultVariableName', isAttr: true, type: 'String' },
        { name: 'type', isAttr: true, type: 'String' },
      ],
    },
    // 注意：不要给 SequenceFlow 加 conditionExpression —— BPMN 标准里已有，
    // 这里再声明一遍会被 bpmn-moddle 视为未 redefines 的属性覆盖，
    // 结果整个 <sequenceFlow> 都会在 importXML 阶段被拒解析，导致所有连线丢失。
  ],
};
