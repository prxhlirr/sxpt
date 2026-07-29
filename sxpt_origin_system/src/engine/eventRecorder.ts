import type { RecordedEvent, StudentSession } from '../types/domain';

export type RecordedEventDraft = Omit<RecordedEvent, 'id'> & {
  id?: string;
};

export function recordEvent(
  session: StudentSession,
  event: RecordedEventDraft
): StudentSession {
  const normalizedEvent: RecordedEvent = {
    ...event,
    id: event.id ?? `event-${session.events.length + 1}`
  };

  return {
    ...session,
    events: [...session.events, normalizedEvent]
  };
}
