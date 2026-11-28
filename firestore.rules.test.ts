/**
 * Phase 0 Adversarial Security Rule Verification Suite (KonvoiKu)
 * Verifies all "Dirty Dozen" payloads yield PERMISSION_DENIED against firestore.rules
 * when executed with @firebase/rules-unit-testing + Firebase Emulator.
 */

export interface SecurityTestCase {
  id: number;
  name: string;
  collectionPath: string;
  operation: 'get' | 'list' | 'create' | 'update' | 'delete';
  expected: 'PERMISSION_DENIED';
  payload?: Record<string, unknown>;
}

export const DIRTY_DOZEN_CASES: SecurityTestCase[] = [
  {
    id: 1,
    name: 'Unauthenticated room create',
    collectionPath: 'rooms/KNV-9999',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { roomId: 'KNV-9999', leaderUid: 'attacker' },
  },
  {
    id: 2,
    name: 'Unverified email user write',
    collectionPath: 'rooms/KNV-9999',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 3,
    name: 'Ghost field injection on room update',
    collectionPath: 'rooms/KNV-8492',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { isAdminOverride: true },
  },
  {
    id: 4,
    name: 'Leader UID spoofing on room create',
    collectionPath: 'rooms/KNV-8492',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { roomId: 'KNV-8492', leaderUid: 'victim-uid' },
  },
  {
    id: 5,
    name: 'Invalid document ID poisoning',
    collectionPath: 'rooms/bad$room..id',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 6,
    name: 'Self-assigned TOUR_LEADER role on member join',
    collectionPath: 'rooms/KNV-8492/members/member-1',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { uid: 'member-1', role: 'TOUR_LEADER', isSweeper: true },
  },
  {
    id: 7,
    name: 'Member self-promoting to Sweeper on update',
    collectionPath: 'rooms/KNV-8492/members/member-1',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { isSweeper: true },
  },
  {
    id: 8,
    name: 'Cross-member GPS coordinate tampering',
    collectionPath: 'rooms/KNV-8492/members/member-2',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { latitude: 0, longitude: 0 },
  },
  {
    id: 9,
    name: 'Non-leader altering official convoy destination',
    collectionPath: 'rooms/KNV-8492',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { destinationName: 'Hijacked Destination' },
  },
  {
    id: 10,
    name: 'Chat message senderUid spoofing',
    collectionPath: 'rooms/KNV-8492/messages/msg-1',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { senderUid: 'other-rider', textContent: 'Spoofed' },
  },
  {
    id: 11,
    name: 'Unauthorized read of another user private PII doc',
    collectionPath: 'users/victim-uid',
    operation: 'get',
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 12,
    name: 'Mutating a FINISHED room or forging client timestamp',
    collectionPath: 'rooms/KNV-8492',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { destinationName: 'After Finish' },
  },
  {
    id: 13,
    name: 'Non-leader member creating room activity checklist item',
    collectionPath: 'rooms/KNV-8492/activities/act-1',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { id: 'act-1', roomId: 'KNV-8492', createdByUid: 'member-1', title: 'Hijack Activity' },
  },
  {
    id: 14,
    name: 'Member modifying another member checklist progress',
    collectionPath: 'rooms/KNV-8492/activityProgress/member-2',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { completedActivityIds: ['act-1'] },
  },
  {
    id: 15,
    name: 'External user self-issuing family grant without claiming active link',
    collectionPath: 'family_grants/attacker-uid',
    operation: 'create',
    expected: 'PERMISSION_DENIED',
    payload: { familyUid: 'attacker-uid', inviteCode: 'FAM-FAKE12', roomId: 'KNV-8492', targetUid: 'member-1', active: true },
  },
  {
    id: 16,
    name: 'Family monitor reading unrelated member location doc',
    collectionPath: 'rooms/KNV-8492/members/member-2',
    operation: 'get',
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 17,
    name: 'Family monitor editing target member checklist progress',
    collectionPath: 'rooms/KNV-8492/activityProgress/member-1',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { completedActivityIds: ['act-1'] },
  },
  {
    id: 18,
    name: 'External user hijacking already claimed family invite link',
    collectionPath: 'family_links/FAM-ABC123',
    operation: 'update',
    expected: 'PERMISSION_DENIED',
    payload: { claimedByUid: 'attacker-uid', claimedByName: 'Hijacker' },
  },
];

