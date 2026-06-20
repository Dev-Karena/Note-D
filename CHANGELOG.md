# Changelog

All notable changes to **NoteD** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-06-20

### Added
*   **Dynamic Note Orchestration UI:** Launched a beautiful responsive dual-panel list/grid dashboard in Jetpack Compose incorporating priority note pinning, customized background color picking, soft-archiving, and category tag assignments.
*   **Category Organiser Stream:** Integrated horizontal sliding navigation chips for immediate on-the-fly category generation and database query filtering.
*   **Hardware Biometrics Verification:** Added biometric prompt gateways using Android `BiometricPrompt` supporting face and fingerprint scanners (Class 3) to lock access to note list displays.
*   **Cryptographic PIN Gate Lock:** Created a custom local PIN lock fallback utilizing PBKDF2 with HMAC-SHA256 iterations to encrypt credentials.
*   **Secure Preferences Storage:** Configured Jetpack Security `EncryptedSharedPreferences` utilizing symmetric AES-256 GCM master keys isolated within on-device hardware Secure Elements.
*   **Reliable Alerts via WorkManager:** Added custom background reminder scheduling utilizing Google's modern `WorkManager` API, ensuring Scheduled alerts are delivered on time without background throttling.
*   **Dynamic Boot Receiver Synchronization:** Integrated custom `BootReceiver` that intercepts device startup intents, automatically rescheduling reminders to survive device reboots.
*   **Full-Text Elastic Search:** Implemented instant search capabilities scanning note bodies and folder properties, linked directly to reactive StateFlow streams.
*   **Accessible Modern Materials:** Seamless Material 3 theme configurations with dynamic dark mode options, scalable typography, and touch target sizes conforming to Android standards.

### Changed
*   **Offline-First SQL Schema:** Abstracted physical SQLite storage structures using the Room ORM database engine, configured with Write-Ahead Logging (WAL) for responsive transaction speeds.
*   **UDF MVVM Execution State:** Configured and deployed Unidirectional Data Flow pattern, replacing legacy design models to decouple rendering from query calculations.

### Fixed
*   **Safe Context Casting:** Resolved biometric prompt crashes occurring on screen layout changes by building context wrappers that traverse nested UI levels up to the parent `FragmentActivity`.
*   **Safe DB Migrations:** Implemented incremental migration engines (`MIGRATION_3_4`, `MIGRATION_4_5`), successfully resolving potential database crashes during app updates.

---

[1.0.0]: https://github.com/DEV/NoteD/releases/tag/v1.0.0
