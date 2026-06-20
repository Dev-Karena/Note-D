# NoteD Security Policy & Architecture Guide

This document outlines the security architecture, data protection principles, threat mitigation strategies, and responsible vulnerability disclosure process for NoteD, engineered by **DEV KARENA**. NoteD is built around a privacy-first, zero-cloud dependency model, keeping all user data isolated on the physical device.

---

## 1. Core Security Architecture

NoteD implements a layered defense-in-depth model across the application sandbox, physical storage layer, and OS interaction boundaries.

```
┌────────────────────────────────────────────────────────┐
│               Security Gate Sheath                     │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌───────────────────────┐    ┌──────────────────────┐ │
│  │   Biometric Sensor    │    │ Manual Numeric PIN   │ │
│  │     (Class 3 ID)      │    │  (PBKDF2 SHA-256)    │ │
│  └───────────┬───────────┘    └───────────┬──────────┘ │
│              │                            │            │
│              └─────────────┬──────────────┘            │
│                            ▼                           │
│              ┌───────────────────────────┐             │
│              │ Android Keystore Provider │             │
│              └─────────────┬─────────────┘             │
│                            ▼                           │
│              ┌───────────────────────────┐             │
│              │ EncryptedSharedPreferences │             │
│              └───────────────────────────┘             │
└────────────────────────────────────────────────────────┘
```

---

## 2. Integrated Security Features

### 2.1 Hardware-Backed Biometric Authentication
NoteD utilizes the modern Android Biometrics API to lock access to user data:
*   **Sensor Class Requirements:** Demands Class 3 Biometrics (facial and fingerprint checks on modern handsets), rejecting weaker classifications.
*   **Lifecycle Isolation:** The scanning prompt is bound to the parent `FragmentActivity` lifecycle using recursive context-unwrapping patterns, preventing memory leaks during configuration changes.

### 2.2 Local Security PIN Lock
NoteD includes a secure, custom PIN Lock fallback:
*   **PBKDF2 Cryptographic Hashing:** Rather than storing plaintext PINs, verification uses custom PBKDF2 iterations with HMAC-SHA256, protecting login state against memory analysis.
*   **Strict UI Lock Gate:** If a PIN is active, the app blocks the presentation layer with a visual lock screen on cold starts and app resume.

### 2.3 Cryptographically Encrypted SharedPreferences
Application configurations, locked statuses, and hashed PIN variables are cryptographically encoded:
*   **Encryption Algorithm:** Uses symmetric **AES-256 GCM** encryption.
*   **Keystore Management:** Master keys are generated inside the on-device secure element (TEE), making them unexportable even on rooted systems.

### 2.4 Physical Database Sandbox Isolation
All notes, search keywords, categories, and alarm configurations write directly to a local, isolated SQLite database file inside the app's secure files sandbox (`/data/data/com.example/databases/`).

---

## 3. Threat Mitigation Matrix

NoteD is resilient against common physical and digital attack vectors:

| Active Threat | Target System Vector | NoteD Countermeasure |
| :--- | :--- | :--- |
| **Unauthorized Physical Access** | Unauthorized user gains access to an active or unlocked handset. | **Auto-Lock on Resume:** Background monitoring triggers biometric prompt covers on application resume. |
| **Root Compromise & Sandbox Access** | Rooted device allows attackers to bypass OS file access limits. | **Hardware KeyStore Isolation:** Encrypted keys remain isolated inside the HSM, keeping raw files unreadable. |
| **Memory Dump Analysis** | Exposing plaintext credentials via live RAM tracing or debugger dumps. | **Memory Security:** Plaintext secrets are quickly cleared from RAM after check validations, and crypt hashes are checked out-of-line. |
| **Over-the-Air Leaks** | Back-end database requests intercepted via man-in-the-middle attacks. | **Air-Gapped Isolation:** Complete lack of Internet networks permissions precludes any theoretical remote leaks. |

---

## 4. Security Reporting Guidelines

If you discover a security vulnerability in NoteD, please report it to us following the disclosure guidelines below:

### Reporting Process
1.  **Do Not Create Public Issues:** Please do **not** log security vulnerabilities in public issue trackers.
2.  **Submit Privately:** Reach out to the developer directly at **karenadev000@gmail.com** with details of the vulnerability.
3.  **Provide Reproduction Steps:** Include clear steps, a proof-of-concept (PoC), and target device criteria to help us reproduce the issue.

### Our Commitment
*   **Prompt Acknowledgment:** We will acknowledge receipt of your report within 48 hours.
*   **Vulnerability Remediation:** Once verified, we will work to address the vulnerability, pushing the patch to the main branch immediately.
*   **Responsible Coordination:** We ask for a reasonable public disclosure delay (e.g., 30 - 60 days) model, publishing details only after a patch is available.
