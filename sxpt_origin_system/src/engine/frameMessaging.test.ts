import { describe, expect, it } from 'vitest';
import {
  isMessageFromBusinessFrame,
  resolveCurrentFrameTargetOrigin,
  resolveFrameTargetOrigin
} from './frameMessaging';

describe('frameMessaging', () => {
  it('uses the teaching app origin for same-origin iframe paths', () => {
    expect(
      resolveFrameTargetOrigin('/business-sdk-demo.html', 'http://localhost:5173')
    ).toBe('http://localhost:5173');
  });

  it('uses the business origin for cross-origin iframe urls', () => {
    expect(
      resolveFrameTargetOrigin(
        'https://business.example.com/purchase/apply',
        'http://teach.example.com'
      )
    ).toBe('https://business.example.com');
  });

  it('keeps the bound business origin when ready messages report relative urls', () => {
    expect(
      resolveCurrentFrameTargetOrigin(
        'https://business.example.com/purchase/apply',
        '/purchase/approve#detail',
        true,
        'http://teach.example.com'
      )
    ).toBe('https://business.example.com');
  });

  it('falls back to wildcard when the frame src cannot be parsed', () => {
    expect(
      resolveFrameTargetOrigin('http://[bad-url', 'http://teach.example.com')
    ).toBe('*');
  });

  it('accepts only messages from the current business frame window', () => {
    const frameWindow = { name: 'business-frame' } as unknown as Window;
    const otherWindow = { name: 'other-frame' } as unknown as Window;

    expect(isMessageFromBusinessFrame(frameWindow, frameWindow)).toBe(true);
    expect(isMessageFromBusinessFrame(otherWindow, frameWindow)).toBe(false);
  });

  it('rejects messages from an unexpected iframe origin', () => {
    const frameWindow = { name: 'business-frame' } as unknown as Window;

    expect(
      isMessageFromBusinessFrame(
        frameWindow,
        frameWindow,
        'https://business.example.com',
        'https://business.example.com'
      )
    ).toBe(true);
    expect(
      isMessageFromBusinessFrame(
        frameWindow,
        frameWindow,
        'https://evil.example.com',
        'https://business.example.com'
      )
    ).toBe(false);
  });
});
