# Vyra — App Structure & Architecture

## 1. App Identity
- **Name:** Vyra (Virtual Your Reliable Assistant)
- **Type:** Local-first personal vault + AI assistant (Android, Kotlin)
- **Core principle:** Nothing auto-saves without user confirmation. No silent background capture. No auto-restore on install.

## 2. High-Level Layers

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  Lock Screen | Dashboard | Chat | Vault  │
├─────────────────────────────────────────┤
│           Assistant Logic Layer          │
│  Intent parser | Confirmation dialogs    │
│  Query handler | Reminder engine         │
├─────────────────────────────────────────┤
│             Data Layer (Room)            │
│  Vault entries | Tasks | Categories      │
├─────────────────────────────────────────┤
│           Security Layer                 │
│  Android Keystore | SQLCipher            │
│  Biometric/PIN auth                      │
├─────────────────────────────────────────┤
│        Backup/Export Layer               │
│  Encrypted JSON export/import (manual)   │
├─────────────────────────────────────────┤
│      Optional: Autofill Service          │
│  System-level credential suggestion      │
└─────────────────────────────────────────┘
```

## 3. Modules

| Module | Responsibility |
|---|---|
| `core.security` | Keystore key management, encryption/decryption, biometric gate |
| `core.database` | Room entities, DAOs, SQLCipher integration |
| `feature.vault` | CRUD for passwords/accounts/sites/APIs/notes |
| `feature.assistant` | Chat-style query interface, confirmation prompts |
| `feature.tasks` | To-do + reminders (local notifications) |
| `feature.backup` | JSON export/import, encryption of exported file |
| `feature.autofill` | Android AutofillService implementation (Phase 2) |

## 4. Tech Stack
- Kotlin + Jetpack Compose
- Room DB + SQLCipher (encryption at rest)
- Android Keystore (master key, never leaves device)
- WorkManager (reminders/tasks)
- AutofillService API (optional, Phase 2)
- No network/cloud SDK — backup is manual file export only

## 5. Data Flow (Save Example)
1. User taps "+" or says "save this"
2. Assistant asks: category? fields?
3. User confirms
4. Entry encrypted → written to Room DB
5. Nothing leaves the device automatically

## 6. Non-Goals (explicitly out of scope)
- No keystroke/typing capture
- No full device/screen control access
- No auto-upload or auto-restore across devices
- No silent background monitoring
