# Firebase Cloud Messaging (FCM) Setup Guide

This document provides a step-by-step guide to configuring Firebase Cloud Messaging for the SyAi project, enabling background image processing notifications between the **Backend (Spring Boot)** and the **Android App**.

## Prerequisites
- Access to the [Firebase Console](https://console.firebase.google.com/).
- A Google account.

---

## 1. Firebase Project Configuration

1.  **Create/Select Project**: Open the Firebase Console and select your project.
2.  **Add Android App**:
    *   Click on **Add app** > **Android**.
    *   Package Name: `com.mato.syai`.
    *   Follow the wizard and download the `google-services.json` file.
3.  **Enable Cloud Messaging**:
    *   Go to **Project Settings** > **Cloud Messaging**.
    *   Ensure the **Firebase Cloud Messaging API (V1)** is enabled.

---

## 2. Android App Setup (Client)

### Placement of Configuration
1.  Locate the downloaded `google-services.json`.
2.  Place it in the `app/` directory of the project:
    `SyAi-Public/app/google-services.json`

### Implementation Details (Already Integrated)
- **Dependencies**: The `firebase-messaging` library has been added to `app/build.gradle.kts`.
- **Service**: `SyAiFirebaseMessagingService` is registered in `AndroidManifest.xml`.
- **Token Transmission**: When you trigger image generation, the `NoteEditorViewModel` retrieves the latest FCM token using `FirebaseMessaging.getInstance().token` and sends it to the backend as part of the request.

### Runtime Permissions
On Android 13 (API 33) and above, you must grant the **Notification Permission** when prompted by the app to see the system notifications.

---

## 3. Backend Setup (Server)

The backend uses the **Firebase Admin SDK** to send notifications. It requires a service account key to authenticate.

### Generating Service Account Key
1.  In the Firebase Console, go to **Project Settings** > **Service Accounts**.
2.  Click on **Generate new private key**.
3.  A JSON file will be downloaded to your computer.

### Placement of Key
1.  Rename the downloaded JSON file to `service-account.json`.
2.  Place it in the backend's resources directory:
    `SyAi-Public/image-generator/src/main/resources/service-account.json`

> [!CAUTION]
> **Security Warning**: Never commit `service-account.json` to a public version control system (like GitHub). It is already added to `.gitignore` in this project.

---

## 4. How the Flow Works

1.  **Request**: The Android app sends a generation request containing:
    *   `prompt`: What to generate.
    *   `noteId`: ID of the current note.
    *   `pageNo`: The page where the image should be placed.
    *   `fcmToken`: The unique identifier for the device.
2.  **Processing**: The backend starts the Python-based image generation asynchronously.
3.  **Completion**: Once the image is ready (`COMPLETED`) or fails (`FAILED`), the backend:
    *   Constructs an FCM message with a data payload containing `jobId`, `noteId`, and `imageUrl`.
    *   Sends the message to the `fcmToken` provided in the request.
4.  **Delivery**:
    *   **App in Foreground**: The `SyAiFirebaseMessagingService` receives the message and triggers an internal event. If the note with `noteId` is currently open, the image is downloaded and inserted instantly.
    *   **App in Background**: The service shows a system notification. Clicking it opens the app, navigates to the specific note, and triggers the download/insertion logic.

---

## 5. Troubleshooting

- **No Notification Received**:
    *   Verify `service-account.json` is correctly placed and has the right permissions in the Firebase Console.
    *   Check backend logs for `Successfully sent message: ...` or error traces.
    *   Ensure the Android device has an active internet connection and Google Play Services.
- **Image Not Inserting on Load**:
    *   Check if the `GET /api/v1/images/jobs/note/{noteId}` endpoint returns the completed job.
    *   Verify the `LOCAL_IMAGE_GENERATOR_BASE_URL` in `local.properties` matches your backend's IP address.
