import { describe, expect, it } from 'vitest';
import { shouldEmitRecorderEvent } from './recorderEventPolicy';

describe('recorderEventPolicy', () => {
  it('does not record a step when the user only focuses a form control', () => {
    expect(shouldEmitRecorderEvent('text-control', 'click')).toBe(false);
    expect(shouldEmitRecorderEvent('text-control', 'blur')).toBe(false);
  });

  it('records form controls only when their value changes', () => {
    expect(shouldEmitRecorderEvent('text-control', 'change')).toBe(true);
    expect(shouldEmitRecorderEvent('select', 'change')).toBe(true);
  });

  it('does not record clicks on form containers', () => {
    expect(shouldEmitRecorderEvent('form', 'click')).toBe(false);
  });

  it('records command clicks and native submits', () => {
    expect(shouldEmitRecorderEvent('command', 'click')).toBe(true);
    expect(shouldEmitRecorderEvent('form', 'submit')).toBe(true);
  });
});
