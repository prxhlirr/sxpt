import { computed, ref } from 'vue';
import {
  dataPrepareApi,
  type AuthoringLaunchResult,
  type CreateAuthoringLaunchRequest
} from '../services/trainingApi';

export type AuthoringLaunchApi = (
  request: CreateAuthoringLaunchRequest
) => Promise<AuthoringLaunchResult>;

export function useAuthoringBusinessLaunch(
  launchApi: AuthoringLaunchApi = dataPrepareApi.createAuthoringLaunch
) {
  const result = ref<AuthoringLaunchResult | null>(null);
  const loading = ref(false);
  const error = ref('');
  const frameUrl = computed(() => result.value?.launchUrl?.trim() || '');

  async function start(request: CreateAuthoringLaunchRequest): Promise<void> {
    loading.value = true;
    error.value = '';
    result.value = null;
    try {
      result.value = await launchApi(request);
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '业务数据生成失败';
    } finally {
      loading.value = false;
    }
  }

  return {
    result,
    loading,
    error,
    frameUrl,
    start
  };
}
