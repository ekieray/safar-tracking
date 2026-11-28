import { AvatarPreset, ChatMessageType, MemberConditionStatus } from '../types/convoy';
import {
  publishMemberTelemetry,
  sendPriorityRoomBroadcast,
  sendRoomMessage,
} from './convoySyncService';

const OFFLINE_QUEUE_KEY = 'safar_offline_sync_queue_v1';

export interface QueuedTelemetryItem {
  kind: 'telemetry';
  roomId: string;
  uid: string;
  queuedAtMs: number;
  patch: {
    latitude: number;
    longitude: number;
    speedKmh: number;
    heading: number;
    accuracyMeters: number;
    routeProgress: number;
    xFraction: number;
    yFraction: number;
    lastLocationText?: string;
    conditionStatus?: MemberConditionStatus;
  };
}

export interface QueuedMessageItem {
  kind: 'message';
  roomId: string;
  queuedAtMs: number;
  msg: {
    id: string;
    senderUid: string;
    senderName: string;
    senderVehicle: string;
    senderAvatarPreset: AvatarPreset;
    type: ChatMessageType;
    textContent: string;
    voiceDurationSec?: number;
    voiceAudioDataUrl?: string;
  };
}

export interface QueuedBroadcastItem {
  kind: 'broadcast';
  roomId: string;
  queuedAtMs: number;
  payload: {
    id: string;
    senderUid: string;
    senderName: string;
    senderVehicle: string;
    senderAvatarPreset: AvatarPreset;
    title: string;
    message: string;
    atLabel: string;
  };
}

export type OfflineQueueItem =
  | QueuedTelemetryItem
  | QueuedMessageItem
  | QueuedBroadcastItem;

function readQueue(): OfflineQueueItem[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(OFFLINE_QUEUE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as OfflineQueueItem[]) : [];
  } catch {
    return [];
  }
}

function writeQueue(items: OfflineQueueItem[]): number {
  const trimmed = items.slice(-40);
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(trimmed));
    } catch {
      // Ignore storage quota errors
    }
  }
  return trimmed.length;
}

export function getOfflineQueueCount(): number {
  return readQueue().length;
}

export function enqueueTelemetryUpdate(
  item: Omit<QueuedTelemetryItem, 'kind' | 'queuedAtMs'>
): number {
  const current = readQueue().filter(
    (q) =>
      !(q.kind === 'telemetry' && q.roomId === item.roomId && q.uid === item.uid)
  );
  current.push({
    kind: 'telemetry',
    roomId: item.roomId,
    uid: item.uid,
    queuedAtMs: Date.now(),
    patch: item.patch,
  });
  return writeQueue(current);
}

export function enqueueRoomMessage(
  item: Omit<QueuedMessageItem, 'kind' | 'queuedAtMs'>
): number {
  const current = readQueue();
  if (
    current.some(
      (q) => q.kind === 'message' && q.roomId === item.roomId && q.msg.id === item.msg.id
    )
  ) {
    return current.length;
  }
  current.push({
    kind: 'message',
    roomId: item.roomId,
    queuedAtMs: Date.now(),
    msg: item.msg,
  });
  return writeQueue(current);
}

export function enqueuePriorityBroadcast(
  item: Omit<QueuedBroadcastItem, 'kind' | 'queuedAtMs'>
): number {
  const current = readQueue();
  current.push({
    kind: 'broadcast',
    roomId: item.roomId,
    queuedAtMs: Date.now(),
    payload: item.payload,
  });
  return writeQueue(current);
}

let isFlushingQueue = false;

export async function flushOfflineSyncQueue(): Promise<{
  flushed: number;
  remaining: number;
}> {
  if (isFlushingQueue) {
    return { flushed: 0, remaining: readQueue().length };
  }
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    return { flushed: 0, remaining: readQueue().length };
  }

  const queue = readQueue();
  if (queue.length === 0) {
    return { flushed: 0, remaining: 0 };
  }

  isFlushingQueue = true;
  let flushed = 0;
  const remainingItems: OfflineQueueItem[] = [];

  try {
    for (let i = 0; i < queue.length; i++) {
      const entry = queue[i];
      try {
        if (entry.kind === 'telemetry') {
          await publishMemberTelemetry(entry.roomId, entry.uid, entry.patch);
        } else if (entry.kind === 'message') {
          await sendRoomMessage(entry.roomId, entry.msg);
        } else if (entry.kind === 'broadcast') {
          await sendPriorityRoomBroadcast(entry.roomId, entry.payload);
        }
        flushed += 1;
      } catch (err) {
        const msg = err instanceof Error ? err.message.toLowerCase() : '';
        const isPermissionDenied =
          msg.includes('permission') || msg.includes('permission_denied');
        if (isPermissionDenied) {
          // Drop permanently unauthorized items so they don't block the queue
          continue;
        }
        remainingItems.push(entry);
        // Stop flushing further items on network failure and preserve remaining order
        if (i + 1 < queue.length) {
          remainingItems.push(...queue.slice(i + 1));
        }
        break;
      }
    }
    const remaining = writeQueue(remainingItems);
    return { flushed, remaining };
  } finally {
    isFlushingQueue = false;
  }
}
