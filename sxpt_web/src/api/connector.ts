import { apiRequest } from './http';
import type {
  ConnectorResource,
  ConnectorSystem,
  IdentityBinding,
  IsoDateTime,
  PlatformLaunchContext,
  TeachingDataInstance,
  TeachingDataTemplate,
  VerifiedPlatformLaunchContext
} from './contracts';

export interface CreateConnectorSystemRequest {
  tenantId: string;
  systemCode: string;
  systemName: string;
  systemType: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
}

export interface UpdateConnectorSystemRequest {
  id: string;
  systemName: string;
  systemType: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
}

export interface CreateIdentityBindingRequest {
  tenantId: string;
  userId: string;
  connectorSystemId: string;
  externalUserId: string;
  externalUsername?: string;
  externalRoleJson?: string;
  externalOrgJson?: string;
  bindingType: string;
}

export interface CreateLaunchContextRequest {
  tenantId: string;
  userId: string;
  connectorSystemId: string;
  taskId?: string;
  teachingPointId?: string;
  executionId?: string;
  dataInstanceId?: string;
  sceneType: string;
  sdkMode: string;
  targetUrl: string;
  segmentNo?: number;
  actorType?: string;
  requiredExternalOrgId?: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  dataScopeJson?: string;
}

export interface CreateDataTemplateRequest {
  tenantId: string;
  connectorSystemId: string;
  teachingPointId?: string;
  templateCode: string;
  templateName: string;
  sceneType: string;
  initState?: string;
  supportMode?: string;
  configJson?: string;
}

export interface CreateDataInstanceRequest {
  tenantId: string;
  templateId: string;
  connectorSystemId: string;
  ownerUserId?: string;
  classId?: string;
  taskId?: string;
  teachingPointId?: string;
  executionId?: string;
  attemptId?: string;
  sceneType: string;
  externalBusinessId: string;
  externalBusinessNo?: string;
  externalStatus?: string;
  expireTime?: IsoDateTime;
  metadataJson?: string;
}

export interface ResetDataInstanceRequest {
  externalBusinessId: string;
  externalBusinessNo?: string;
  externalStatus?: string;
  metadataJson?: string;
}

export interface CreateConnectorResourceRequest {
  tenantId: string;
  connectorSystemId: string;
  resourceCode: string;
  resourceName: string;
  resourceType: string;
  pageUrl?: string;
  locator?: string;
  stableKey?: string;
  metadataJson?: string;
  sourceCaptureId?: string;
  createBy?: string;
}

export const connectorApi = {
  createSystem(request: CreateConnectorSystemRequest) {
    return apiRequest<ConnectorSystem>({
      method: 'POST',
      url: '/connector/system/create',
      data: request
    });
  },

  updateSystem(request: UpdateConnectorSystemRequest) {
    return apiRequest<ConnectorSystem>({
      method: 'POST',
      url: '/connector/system/update',
      data: request
    });
  },

  enableSystem(id: string) {
    return apiRequest<ConnectorSystem>({
      method: 'POST',
      url: `/connector/system/${encodeURIComponent(id)}/enable`
    });
  },

  disableSystem(id: string) {
    return apiRequest<ConnectorSystem>({
      method: 'POST',
      url: `/connector/system/${encodeURIComponent(id)}/disable`
    });
  },

  getSystem(id: string) {
    return apiRequest<ConnectorSystem>({
      method: 'GET',
      url: `/connector/system/${encodeURIComponent(id)}`
    });
  },

  listSystems(tenantId: string) {
    return apiRequest<ConnectorSystem[]>({
      method: 'GET',
      url: '/connector/system/list',
      params: { tenantId }
    });
  },

  createIdentityBinding(request: CreateIdentityBindingRequest) {
    return apiRequest<IdentityBinding>({
      method: 'POST',
      url: '/connector/identity-bindings/create',
      data: request
    });
  },

  getIdentityBinding(
    tenantId: string,
    userId: string,
    connectorSystemId: string
  ) {
    return apiRequest<IdentityBinding>({
      method: 'GET',
      url: '/connector/identity-bindings/user',
      params: { tenantId, userId, connectorSystemId }
    });
  },

  createLaunchContext(request: CreateLaunchContextRequest) {
    return apiRequest<PlatformLaunchContext>({
      method: 'POST',
      url: '/connector/launch-contexts/create',
      data: request
    });
  },

  verifyLaunchToken(tenantId: string, launchToken: string) {
    return apiRequest<VerifiedPlatformLaunchContext>({
      method: 'POST',
      url: '/connector/launch-contexts/verify',
      data: { tenantId, launchToken }
    });
  },

  markLaunchUsed(id: string) {
    return apiRequest<PlatformLaunchContext>({
      method: 'POST',
      url: '/connector/launch-contexts/used',
      data: { id }
    });
  },

  markLaunchFailed(id: string, errorMessage: string) {
    return apiRequest<PlatformLaunchContext>({
      method: 'POST',
      url: '/connector/launch-contexts/failed',
      data: { id, errorMessage }
    });
  },

  createDataTemplate(request: CreateDataTemplateRequest) {
    return apiRequest<TeachingDataTemplate>({
      method: 'POST',
      url: '/connector/data-templates/create',
      data: request
    });
  },

  listDataTemplates(tenantId: string, connectorSystemId: string) {
    return apiRequest<TeachingDataTemplate[]>({
      method: 'GET',
      url: '/connector/data-templates',
      params: { tenantId, connectorSystemId }
    });
  },

  listActiveDataTemplates(
    tenantId: string,
    connectorSystemId: string,
    teachingPointId: string,
    sceneType: string
  ) {
    return apiRequest<TeachingDataTemplate[]>({
      method: 'GET',
      url: '/connector/data-templates/active',
      params: { tenantId, connectorSystemId, teachingPointId, sceneType }
    });
  },

  createDataInstance(request: CreateDataInstanceRequest) {
    return apiRequest<TeachingDataInstance>({
      method: 'POST',
      url: '/connector/data-instances/create',
      data: request
    });
  },

  findDataInstanceByExternal(
    tenantId: string,
    connectorSystemId: string,
    externalBusinessId: string
  ) {
    return apiRequest<TeachingDataInstance>({
      method: 'GET',
      url: '/connector/data-instances/external',
      params: { tenantId, connectorSystemId, externalBusinessId }
    });
  },

  listDataInstancesByOwner(
    tenantId: string,
    ownerUserId: string,
    sceneType: string
  ) {
    return apiRequest<TeachingDataInstance[]>({
      method: 'GET',
      url: '/connector/data-instances/owner',
      params: { tenantId, ownerUserId, sceneType }
    });
  },

  listDataInstancesByTask(
    tenantId: string,
    taskId: string,
    sceneType: string
  ) {
    return apiRequest<TeachingDataInstance[]>({
      method: 'GET',
      url: '/connector/data-instances/task',
      params: { tenantId, taskId, sceneType }
    });
  },

  lockDataInstance(id: string) {
    return apiRequest<TeachingDataInstance>({
      method: 'POST',
      url: `/connector/data-instances/${encodeURIComponent(id)}/lock`
    });
  },

  discardDataInstance(id: string) {
    return apiRequest<TeachingDataInstance>({
      method: 'POST',
      url: `/connector/data-instances/${encodeURIComponent(id)}/discard`
    });
  },

  resetDataInstance(id: string, request: ResetDataInstanceRequest) {
    return apiRequest<TeachingDataInstance>({
      method: 'POST',
      url: `/connector/data-instances/${encodeURIComponent(id)}/reset`,
      data: request
    });
  },

  createResource(request: CreateConnectorResourceRequest) {
    return apiRequest<ConnectorResource>({
      method: 'POST',
      url: '/connector/resources/create',
      data: request
    });
  },

  listResources(
    tenantId: string,
    connectorSystemId: string,
    pageUrl: string
  ) {
    return apiRequest<ConnectorResource[]>({
      method: 'GET',
      url: '/connector/resources',
      params: { tenantId, connectorSystemId, pageUrl }
    });
  }
};
