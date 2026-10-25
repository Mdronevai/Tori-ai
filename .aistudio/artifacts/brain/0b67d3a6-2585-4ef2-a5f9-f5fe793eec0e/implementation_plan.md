# Automated Credential Auto-Save & Categorized Permanent Clipboard Vault

Implementation plan for automated background password/email/site credential detection with silent auto-saving and system notifications, alongside a permanent categorized clipboard management system supporting language detection (Bangla/English), text length filters, date ranges, and smart categorization.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your responses, the following architectural choices have been confirmed:

- **Confirmed Decision 1 (Silent Credential Saving & Notification)**: Credentials and autofilled accounts will be captured automatically in the background and saved securely to the encrypted local vault. The system will issue a heads-up system notification: *"Password saved securely for [App/Website]"* without blocking your current app flow.
- **Confirmed Decision 2 (Multi-Filter Categorized Clipboard Vault)**: All copied text clips will be permanently stored in a Room SQLite database and tagged by text length (*Short/Medium/Long*), language (*Bangla/English/Mixed*), date range (*Today/This Week/1 Month Ago+*), and content category (*Passwords, Emails, APIs, Links, Text*).
- **Confirmed Decision 3 (Foreground Monitoring & Boot Auto-Start)**: A persistent Android Foreground Service will run continuously in the background, listening for system clipboard changes and input field autofill events, with a `RECEIVE_BOOT_COMPLETED` broadcast receiver so the assistant auto-starts upon device reboot.

---

## 1. Overview & Core Concept

- **What It Does**: Provides hands-free automated security & clipboard organization. Credentials (passwords, emails, app/site names, URLs, timestamps) are captured silently in background, triggering a system notification confirmation. Simultaneously, a permanent Clipboard Manager archives every copied text fragment with language detection (Bangla/English), text size metrics, date filters (including 1+ month historical recall), and custom category tags.
- **Target Audience / Persona**: Power users desiring zero-effort password archiving and instant retrieval of historical copied snippets across apps and browser sessions.
- **Key Value**: 100% automated local-first protection—no manual entry required, zero silent missed clips, and instant multi-criteria search for clips copied even months ago.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Automated Credential Capture**: User fills or copies credentials in any app/site -> Background Foreground Service detects credential pattern -> Encrypts & auto-saves to Room DB -> Triggers system notification *"Password saved securely for Example.com"*.
  2. **Clipboard Categorized Vault View**: User opens Tori Assistant -> Navigates to "Clipboard Vault" tab -> Sees categorized cards with metadata tags (`[Bangla]`, `[Short]`, `[1 month ago]`, `[API Key]`) -> Filters using Chip Groups (All, Bangla, English, Long, Passwords) or Date Picker -> One-tap copy or delete.
  3. **Background Service Control & Permissions**: Settings tab provides toggles for Notification Permissions, Background Service status, Boot Auto-Start toggle, and Clipboard Auto-Capture on/off.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Cyberpunk-utilitarian HUD with sleek dark slate cards, emerald accent badges for saved passwords, and indigo chips for clipboard categories.
  - *Color Palette*: Primary `#6366F1` (Indigo), Accent `#10B981` (Emerald), Dark Background `#0F172A` (Slate 900), Surface `#1E293B` (Slate 800), Warning `#F59E0B` (Amber).
  - *Typography*: Monospace font for copied code/tokens, high-contrast medium sans-serif for Bangla script and English clip previews.
  - *Interactive Feedback*: Smooth entry animations for new clipboard items, swipe-to-dismiss clip cards, filter count badges, and copy confirmation toasts.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Room Database Schema Expansion vs. Separate Tables**
  - *Chosen Approach*: Create a dedicated `ClipboardEntity` table alongside existing `VaultEntry` table in `ToriDatabase` with database version bump (Version 2) and migration.
  - *Why*: Isolates transient/permanent clipboard clips from formal high-entropy vault secrets while allowing fast indexing on language (`isBangla`), length category (`SHORT`/`MEDIUM`/`LONG`), category tag (`PASSWORD`/`EMAIL`/`API`/`TEXT`), and timestamp (`createdAt`).

- **Decision 2: Language & Text Classification Engine**
  - *Chosen Approach*: Lightweight regex-based Unicode block detector (`\u0980-\u09FF` for Bengali script) combined with string length analysis and key-pattern heuristics.
  - *Why*: Instant, zero-latency execution on every copy event without network calls or battery drain.

- **Decision 3: Foreground Service with System Notification Channel**
  - *Chosen Approach*: `ToriBackgroundService` running as a Android Foreground Service with notification channel `tori_credential_channel` and runtime permission checks for Android 13+ (`POST_NOTIFICATIONS`).
  - *Why*: Ensures Android OS does not kill the clipboard/credential listener in background and guarantees user feedback when passwords are saved.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Android OS System Events                        │
│   (ClipboardManager Listener / Boot Completed / Notification System)   │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
                    ▼                                ▼
┌──────────────────────────────────────┐ ┌──────────────────────────────┐
│       ToriBackgroundService          │ │    ToriBootReceiver          │
│  - Listens to Clipboard Changes      │ │  - Auto-starts service on    │
│  - Captures Credential Patterns      │ │    device reboot             │
│  - Displays Heads-Up Notifications   │ └──────────────────────────────┘
└───────────────────┬──────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        ToriRepository Layer                           │
│  - Classifies Clip: Bangla/English, Short/Medium/Long, Category        │
│  - Encrypts Credentials & Inserts to Room Database                     │
└───────────────────┬────────────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Room SQLite Database (ToriDatabase v2)                   │
│  - VaultEntity (Credentials, Passwords, Emails, APIs)                  │
│  - ClipboardEntity (Raw clips, Language, Category, Timestamp, Size)   │
└───────────────────┬────────────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Jetpack Compose UI & ViewModel                           │
│  - ClipboardVaultScreen (Category Chips, Search Bar, Date Range Filter)│
│  - SettingsScreen (Permissions, Auto-Start Toggle, Notification Test)  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model Entities

1. **`ClipboardEntity`**:
   - `id`: Long (Auto-generate Primary Key)
   - `content`: String (Raw text clip)
   - `language`: String (`"Bangla"`, `"English"`, `"Mixed"`)
   - `lengthCategory`: String (`"SHORT"`, `"MEDIUM"`, `"LONG"`)
   - `contentType`: String (`"PASSWORD"`, `"EMAIL"`, `"API_KEY"`, `"URL"`, `"GENERAL_TEXT"`)
   - `siteOrApp`: String? (Detected app package or URL if available)
   - `timestamp`: Long (System.currentTimeMillis())
   - `isPinned`: Boolean (Default: false)

2. **DAO Functions (`ClipboardDao`)**:
   - `fun getAllClips(): Flow<List<ClipboardEntity>>`
   - `fun filterClips(category: String?, language: String?, minDate: Long?): Flow<List<ClipboardEntity>>`
   - `suspend fun insertClip(clip: ClipboardEntity)`
   - `suspend fun deleteClip(id: Long)`
