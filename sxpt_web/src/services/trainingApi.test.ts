import { describe, expect, it } from 'vitest';
import {
  DATA_PREPARE_CONFIG_INCOMPLETE_CODE,
  TrainingApiRequestError,
  isDataPrepareConfigIncompleteError
} from './trainingApi';

describe('数据准备接口错误识别', () => {
  it('会识别后端返回的数据准备配置不完整错误码', () => {
    const error = new TrainingApiRequestError(
      '数据准备配置不完整',
      DATA_PREPARE_CONFIG_INCOMPLETE_CODE,
      200
    );

    expect(isDataPrepareConfigIncompleteError(error)).toBe(true);
  });

  it('不会把普通参数错误误判为配置不完整', () => {
    const error = new TrainingApiRequestError('参数错误', 400, 200);

    expect(isDataPrepareConfigIncompleteError(error)).toBe(false);
  });
});
