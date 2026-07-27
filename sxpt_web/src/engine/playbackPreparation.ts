import { isSameBusinessPage } from './businessPageState';

export type PlaybackPreparation =
  | 'wait'
  | 'navigate'
  | 'play'
  | 'await-advance';

export interface PlaybackPreparationInput {
  frameReady: boolean;
  currentUrl: string;
  targetUrl: string;
  learning: boolean;
  playbackInFlight: boolean;
  currentStepPlayed: boolean;
}

export interface PlaybackReprepareInput {
  learning: boolean;
  autoPlaying: boolean;
  frameReady: boolean;
  currentStepPlayed: boolean;
  currentStepExists: boolean;
  currentStepChanged: boolean;
}

export function getPlaybackPreparation({
  frameReady,
  currentUrl,
  targetUrl,
  learning,
  playbackInFlight,
  currentStepPlayed
}: PlaybackPreparationInput): PlaybackPreparation {
  if (!frameReady) return 'wait';
  if (isSameBusinessPage(currentUrl, targetUrl)) return 'play';
  if (learning && (playbackInFlight || currentStepPlayed)) {
    return 'await-advance';
  }
  return 'navigate';
}

export function shouldReprepareAfterPlayback({
  learning,
  autoPlaying,
  frameReady,
  currentStepPlayed,
  currentStepExists,
  currentStepChanged
}: PlaybackReprepareInput): boolean {
  return (
    learning &&
    frameReady &&
    !currentStepPlayed &&
    currentStepExists &&
    (autoPlaying || currentStepChanged)
  );
}

export function shouldHandlePlaybackFailure(
  failedStepId: string,
  currentStepId?: string
): boolean {
  return Boolean(currentStepId && failedStepId === currentStepId);
}
