import { describe, expect, it } from 'vitest';
import {
  getPlaybackPreparation,
  shouldHandlePlaybackFailure,
  shouldReprepareAfterPlayback
} from './playbackPreparation';

describe('getPlaybackPreparation', () => {
  it('waits until the iframe reports ready', () => {
    expect(
      getPlaybackPreparation({
        frameReady: false,
        currentUrl: '',
        targetUrl: '/page-b',
        learning: true,
        playbackInFlight: false,
        currentStepPlayed: false
      })
    ).toBe('wait');
  });

  it('plays when the ready page matches the current step', () => {
    expect(
      getPlaybackPreparation({
        frameReady: true,
        currentUrl: '/page-b',
        targetUrl: '/page-b',
        learning: true,
        playbackInFlight: false,
        currentStepPlayed: false
      })
    ).toBe('play');
  });

  it('does not rewind a learning step whose click is navigating', () => {
    expect(
      getPlaybackPreparation({
        frameReady: true,
        currentUrl: '/page-b',
        targetUrl: '/page-a',
        learning: true,
        playbackInFlight: true,
        currentStepPlayed: false
      })
    ).toBe('await-advance');
    expect(
      getPlaybackPreparation({
        frameReady: true,
        currentUrl: '/page-b',
        targetUrl: '/page-a',
        learning: true,
        playbackInFlight: false,
        currentStepPlayed: true
      })
    ).toBe('await-advance');
  });

  it('navigates to a mismatched page before an unplayed step', () => {
    expect(
      getPlaybackPreparation({
        frameReady: true,
        currentUrl: '/page-a',
        targetUrl: '/page-b',
        learning: false,
        playbackInFlight: false,
        currentStepPlayed: false
      })
    ).toBe('navigate');
  });

  it('re-prepares an unplayed current step after an in-flight request settles', () => {
    expect(
      shouldReprepareAfterPlayback({
        learning: true,
        autoPlaying: true,
        frameReady: true,
        currentStepPlayed: false,
        currentStepExists: true,
        currentStepChanged: false
      })
    ).toBe(true);
  });

  it('does not re-prepare a completed, paused, stale, or not-ready step', () => {
    const base = {
      learning: true,
      autoPlaying: true,
      frameReady: true,
      currentStepPlayed: false,
      currentStepExists: true,
      currentStepChanged: false
    };

    expect(
      shouldReprepareAfterPlayback({ ...base, currentStepPlayed: true })
    ).toBe(false);
    expect(
      shouldReprepareAfterPlayback({ ...base, autoPlaying: false })
    ).toBe(false);
    expect(
      shouldReprepareAfterPlayback({ ...base, currentStepExists: false })
    ).toBe(false);
    expect(
      shouldReprepareAfterPlayback({ ...base, frameReady: false })
    ).toBe(false);
  });

  it('prepares a manually selected next step after the old request settles', () => {
    expect(
      shouldReprepareAfterPlayback({
        learning: true,
        autoPlaying: false,
        frameReady: true,
        currentStepPlayed: false,
        currentStepExists: true,
        currentStepChanged: true
      })
    ).toBe(true);
  });

  it('ignores a late failure from a step that is no longer current', () => {
    expect(shouldHandlePlaybackFailure('step-a', 'step-b')).toBe(false);
    expect(shouldHandlePlaybackFailure('step-a', 'step-a')).toBe(true);
    expect(shouldHandlePlaybackFailure('step-a', undefined)).toBe(false);
  });
});
