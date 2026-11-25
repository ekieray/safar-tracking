# Security Specification — KonvoiKu Multi-User Convoy Sync

## 1. Data Invariants
1. **Master Source of Truth**: `/rooms/{roomId}` is the root authority for convoy rooms (`leaderUid`, `status`, route geometry, tolerances). All subcollections (`/members`, `/checkpoints`, `/messages`) belong strictly to a valid parent room via `exists(/databases/$(database)/documents/rooms/$(roomId))`.
2. **PII Isolation**: `emergencyContact` and email details reside strictly in `/users/{userId}` (owner-only read/write) and are NEVER replicated into public or shared `/rooms/{roomId}/members/{memberId}` documents.
3. **Role & Privilege Integrity**: Only `leaderUid` can create a room with `role == 'TOUR_LEADER'`, alter destination/route/straggler/off-route thresholds, add/remove checkpoints, assign `isSweeper`, or set `status == 'FINISHED'`. Regular members can only join with `role == 'MEMBER'` and `isSweeper == false`, update their own telemetry/condition fields, trigger/resolve SOS alerts, and post messages as `senderUid == request.auth.uid`.
4. **Terminal State Locking**: Once a room reaches `status == 'FINISHED'`, all subsequent updates to the room or subcollection GPS writes are rejected.
5. **Temporal Integrity**: `createdAt`, `joinedAt`, and `updatedAt` are validated strictly against `request.time`.
6. **Room Checklist Ownership**: Only the room creator (`leaderUid` / `actingLeaderUid`) or admin can create, edit, or delete `/rooms/{roomId}/activities/{activityId}`. Group members (`isRoomMember` or leader) can read activities and record/toggle their own completion document at `/rooms/{roomId}/activityProgress/{request.auth.uid}` without modifying other members' records.

## 2. The "Dirty Dozen" Attack Payloads (All Must Yield PERMISSION_DENIED)
1. **Unauthenticated Room Creation**: Writing `/rooms/KNV-9999` with `request.auth == null`.
2. **Unverified Email Write**: Creating `/rooms/KNV-9999` with `email_verified: false`.
3. **Ghost Field / Shadow Update**: Creating or updating `/rooms/KNV-8492` with an undeclared field `isVerified: true`.
4. **Leader Impersonation on Create**: Creating `/rooms/KNV-8492` where `leaderUid != request.auth.uid`.
5. **ID Poisoning**: Creating `/rooms/invalid$id/../hack` or a 200-char string doc ID.
6. **Self-Role Escalation on Join**: Member (`uid != leaderUid`) creating `/rooms/KNV-8492/members/{uid}` with `role: 'TOUR_LEADER'` or `isSweeper: true`.
7. **Self-Sweeper Escalation on Update**: Regular member updating `isSweeper: true` or `role: 'TOUR_LEADER'` on their own `/members/{uid}` doc.
8. **Cross-Member Telemetry Tampering**: Member A attempting to overwrite GPS `latitude`/`longitude` on Member B's `/members/{memberB}` doc.
9. **Member Overriding Official Route**: Non-leader member attempting to update `destinationName` or `maxStragglerDistanceMeters` on `/rooms/KNV-8492`.
10. **Message Sender Spoofing**: Member A writing `/rooms/KNV-8492/messages/msg-1` with `senderUid: 'user-B'`.
11. **PII Cross-User Read**: User A attempting to `get` `/users/user-B` to read User B's `emergencyContact`.
12. **Terminal State Violation & Client Timestamp Spoofing**: Updating a room where `status == 'FINISHED'`, or passing a client timestamp instead of `request.time`.
13. **Member Creating/Deleting Leader Checklist Activity**: Non-leader member creating or deleting `/rooms/KNV-8492/activities/act-1`.
14. **Cross-Member Checklist Progress Spoofing**: Member A creating or updating `/rooms/KNV-8492/activityProgress/member-B` or setting `uid: 'member-B'`.

