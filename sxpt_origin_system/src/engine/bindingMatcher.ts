import type { NodeBinding, RecordedEvent } from '../types/domain';

export function matchesBinding(event: RecordedEvent, binding: NodeBinding): boolean {
  return normalizePath(event.url) === normalizePath(binding.url) && event.selector === binding.selector;
}

function normalizePath(url: string): string {
  try {
    return new URL(url).pathname;
  } catch {
    return url.split('?')[0];
  }
}
