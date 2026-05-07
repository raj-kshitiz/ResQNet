import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();
const db = admin.firestore();

// ── Haversine distance in km ──────────────────────────────────────────────────
function haversineKm(
  lat1: number, lon1: number,
  lat2: number, lon2: number
): number {
  const R = 6371;
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * (Math.PI / 180)) *
    Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ── Send FCM to available volunteers within a radius ring ─────────────────────
// minRadiusKm = 0 on first send (include everyone up to maxRadiusKm).
// On expansions, only include the newly added ring (minRadiusKm, maxRadiusKm].
async function notifyVolunteers(
  sosId: string,
  sos: admin.firestore.DocumentData,
  minRadiusKm: number,
  maxRadiusKm: number
): Promise<void> {
  const sosLocation = sos.location as admin.firestore.GeoPoint | undefined;
  if (!sosLocation) return;

  const snapshot = await db
    .collection("users")
    .where("role", "==", "VOLUNTEER")
    .where("isAvailable", "==", true)
    .get();

  const tokens: string[] = [];

  for (const doc of snapshot.docs) {
    const user = doc.data();
    if (!user.fcmToken || doc.id === sos.requesterId) continue;

    const lastLoc = user.lastLocation as admin.firestore.GeoPoint | undefined;
    if (lastLoc) {
      const dist = haversineKm(
        sosLocation.latitude, sosLocation.longitude,
        lastLoc.latitude, lastLoc.longitude
      );
      // Initial send: [0, maxRadius]. Expansion: (minRadius, maxRadius].
      const inRing =
        minRadiusKm === 0 ? dist <= maxRadiusKm : dist > minRadiusKm && dist <= maxRadiusKm;
      if (inRing) tokens.push(user.fcmToken as string);
    } else if (minRadiusKm === 0) {
      // Location unknown — include on first broadcast only (conservative)
      tokens.push(user.fcmToken as string);
    }
  }

  if (tokens.length === 0) return;

  const emergencyType = (sos.emergencyType as string)
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (c: string) => c.toUpperCase());

  const body = sos.addressHint
    ? `${sos.requesterName as string} needs help near ${sos.addressHint as string}`
    : `${sos.requesterName as string} needs help — searching ${maxRadiusKm} km radius`;

  await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: `🚨 ${emergencyType} Alert`,
      body,
    },
    data: {
      sosId,
      type: "SOS_ALERT",
      emergencyType: sos.emergencyType as string,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "sos_alerts",
        priority: "max",
        defaultSound: true,
        defaultVibrateTimings: true,
      },
    },
  });
}

// ── Trigger: new SOS created — notify volunteers in initial radius ─────────────
export const onSosCreated = functions.firestore
  .document("sos_requests/{sosId}")
  .onCreate(async (snap, context) => {
    const sos = snap.data();
    const radiusKm = (sos.radiusKm as number) || 3.0;
    await notifyVolunteers(context.params.sosId, sos, 0, radiusKm);
    // Advance to NOTIFIED so requester's timeline progresses
    await snap.ref.update({ status: "NOTIFIED" });
  });

// ── Trigger: radius expanded by Android client — notify the new ring ──────────
export const onSosUpdated = functions.firestore
  .document("sos_requests/{sosId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    const prevRadius = before.radiusKm as number;
    const newRadius = after.radiusKm as number;

    // Only act when radius grew AND SOS is still waiting for a response
    if (newRadius <= prevRadius) return;
    if (after.status !== "PENDING" && after.status !== "NOTIFIED") return;

    await notifyVolunteers(
      context.params.sosId,
      after,
      prevRadius,
      newRadius
    );
  });
