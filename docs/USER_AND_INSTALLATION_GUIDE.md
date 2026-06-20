# User Guide & Installation Manual (NoteD)

This document contains two complete parts: a visual, step-by-step User Guide for all application features, followed by a detailed technical Installation and Assembly Guide for developers.

---

# PART 1: NoteD User Guide

NoteD is designed to be highly intuitive, applying professional Android Material 3 design systems to present a beautiful, accessibility-conscious user experience.

```
┌────────────────────────────────────────────────────────┐
│  NoteD                                      [ Search ] │
├────────────────────────────────────────────────────────┤
│  [All]  [Work]  [Personal]  [Study]  [+] New Category  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌────────────────────────┐  ┌──────────────────────┐  │
│  │ ★ Project Draft        │  │ Grocery Shopping     │  │
│  │ Write architecture...  │  │ • Milk               │  │
│  │                        │  │ • Coffee             │  │
│  │ [Work]      10:30 AM   │  │ [Personal]   Yesterday  │
│  └────────────────────────┘  └──────────────────────┘  │
│                                                        │
│  ┌────────────────────────┐                            │
│  │ Weekly Sync Call       │                            │
│  │ Meet the team on...    │                            │
│  │                        │                            │
│  │ [Work]      05:00 PM   │                 [ + FAB ]  │
│  └────────────────────────┘                            │
└────────────────────────────────────────────────────────┘
```

## 1. Notes Management

### 1.1 Creating and Saving Notes
To create a note:
1. Tap the Floating Action Button (**+ FAB**) located at the bottom-right of the Home dashboard.
2. In the creation screen, type a **Title** and **Content**.
3. Select a **Category** from the horizontal tags bar or add a new category on the fly.
4. Tap the **Save** icon (checkmark) in the top-right corner. The app will validate your inputs and persist the note to the local database, immediately returning you to the dashboard with a smooth fade animation.

### 1.2 Pinning Important Notes
Notes can be pinned to the top of the interface for immediate visibility:
- On any note card in the dashboard, tap the **Pin** icon. This moves the note to a dedicated **Pinned** section at the top of the screen, floating above all other entries.
- Unpinned notes sit in the **All Notes** section below, sorted by creation timestamp.

### 1.3 Archiving Notes
If you want to declutter your workspace without permanently losing your data, use the **Archive** feature:
- Swiping a note card (or tapping the Archive icon in the note editor) transfers it immediately to the **Archive folder**.
- Access your archived notes through the navigation drawer menu. Archived entries can be fully restored to the main dashboard or permanently deleted.

---

## 2. Categories & Filters
Categories assign context and color to your cards.
- **Adding a Category:** In the horizontal category ribbon on the Home dashboard, tap **Add Category (+)**. Enter a descriptive name and save.
- **Filtering:** Tap any category name in the ribbon (e.g., *Work*, *Personal*). The note stream will instantly filter to match your selection.

---

## 3. Persistent Alarm Reminders
NoteD features built-in notification alerts:
1. Open a note and tap the **Set Alarm** icon.
2. Choose a date and time using the custom Material 3 Date and Time Picker dialogs.
3. Select a repetition frequency (e.g., **None**, **Daily**, **Weekly**, **Monthly**).
4. Tap **Confirm**. Tap **Delete Reminder** at any time to clear it.

Alarms persist across device reboots. At the scheduled time, the system will trigger a high-priority notification with options to **Dismiss** or **Snooze** directly from your lock screen.

---

## 4. PIN & Biometrics Application Lock
Protect your diary and project documents:
- Navigate to **Settings** (gear icon in the drawer/top-bar) and switch on **PIN Lock**.
- Set a secure 4-digit PIN.
- If your device has biometrics (fingerprint or face recognition), toggle **Enable Biometrics**.
- NoteD will now challenge you with a secure validation screen on every application start or when returning from the background.

---
---

# PART 2: Installation & Build Guide

This technical guide walkthrough assists you to compile, test, and release NoteD from source code using Android Studio or command-line developer tools.

## 1. System Engineering Constraints

To target compile-level configurations in your workspace, ensure you match these platform specs:

| Metric | Target Specification |
| :--- | :--- |
| **Java Development Kit** | JDK 17 (Preferred: JetBrains Runtime OpenJDK 17) |
| **Android Gradle Plugin** | Version 8.3 or newer |
| **Kotlin Compiler Version** | Version 1.9.22 (Tied to KSP compiler limits) |
| **Minimum Device Target** | API 26 (Android 8.0, Oreo) |
| **Active Target Target SDK** | API 34 (Android 14.0, Upside Down Cake) |

---

## 2. Multi-Step Build Instructions

Follow these exact inputs to compile the source code cleanly:

### Step 1: Secure Environment Dependencies
In compliance with enterprise-ready development, API keys or settings configurations are decoupled from our codebase files.
- Locate the `.env.example` at the project directory root.
- Copy it to create a new file named `.env`, configuring key variables as needed:
  ```bash
  cp .env.example .env
  ```

### Step 2: Open and Initialize inside Android Studio
1. Open Android Studio.
2. Select **File > Open**, browse to the cloned root folder, and select it.
3. Gradle will begin syncing. Review the **Build** console terminal in your bottom dock to confirm completion.
4. *Troubleshooting Sync Errors:* If the project fails to sync, select **File > Invalidate Caches / Restart**, and verify that your configured JDK version is pointing directly to **JDK 17**.

### Step 3: Compile via Gradle CLI
If you prefer building without Android Studio, you can compile the project using Gradle tasks directly from an interactive bash terminal:

- **Clean prior cache artifacts:**
  ```bash
  gradle clean
  ```
- **Compile the app in Debug Mode:**
  ```bash
  gradle assembleDebug
  ```
- **Run Unit and Mock Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```

---

## 3. Physical Device & Emulator Deployment

### 3.1 Establishing USB Debugging on a Physical Device
1. Open your physical phone's **Settings** app.
2. Navigate to **About Phone** and tap **Build Number** 7 times consecutively. This action unlocks the hidden **Developer Options** menu.
3. Go back to main settings, enter **Developer Options**, and toggle on **USB Debugging**.
4. Connect the handset to your development machine using a secure USB data cable.
5. Grant the permission challenge on your phone's screen when prompt.

### 3.2 Executing Installation & Run
1. In Android Studio, locate the target devices drop-down menu in the top toolbar (near the green run arrow) and choose your connected phone or emulator profile.
2. Press **Run** (`Shift + F10`) or run from your terminal:
   ```bash
   gradle installDebug
   ```
3. The APK will build, transfer, and execute immediately, opening with a splash animation on the device screen.
