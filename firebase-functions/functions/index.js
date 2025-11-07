const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// This triggers whenever a new document is created in Firestore collection "alerts"
exports.sendAlertToTopic = functions.firestore
  .document("alerts/{alertId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (!data) return null;

    const location = data.location || "general";
    const topic = `alerts_${location.replace(/\s+/g, "").toLowerCase()}`;
    const title = data.title || "Emergency Alert";
    const body = data.body || "Please stay alert.";
    const timestamp = data.timestamp || Date.now();

    const payload = {
      notification: {
        title: title,
        body: body,
      },
      data: {
        location: location,
        timestamp: String(timestamp),
        click_action: "OPEN_ALERT_ACTIVITY",
      },
    };

    const options = {
      priority: "high",
    };

    try {
      const response = await admin.messaging().sendToTopic(topic, payload, options);
      console.log("✅ FCM sent successfully:", response);
      return null;
    } catch (error) {
      console.error("❌ Error sending FCM:", error);
      return null;
    }
  });
