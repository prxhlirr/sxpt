/**
 * 实训平台后端运行时配置。
 *
 * 部署后可直接修改本文件，不需要重新执行前端构建。
 */
window.__SXPT_API_CONFIG__ = {
  enabled: true,
  apiBaseUrl: 'http://127.0.0.1:8080/api/v1',
  timeoutMs: 15000,

  // 正式环境建议由登录接口写入 Token；也可在此临时配置联调 Token。
  bearerToken: '',

  // 仅用于本地 dev 后端：Token 为空时通过 /auth/token 自动获取测试 Token。
  autoDevToken: true,
  tenantId: 'demo-tenant',
  currentUserId: 'demo-teacher',
  currentUsername: 'demo-teacher',

  // 用户、角色和正式登录尚未接入时，用于承载课程发布与学生端演示。
  simulatedOrgId: 'demo-class',
  simulatedStudentId: 'demo-student',
  simulatedStudentName: '模拟学生'
};
