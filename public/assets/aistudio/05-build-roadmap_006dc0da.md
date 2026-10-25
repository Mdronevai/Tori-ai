# Vyra — Build Roadmap (for Google AI Studio / Gemini Agent)

Use these as sequential prompts to the Kotlin App Builder Gemini agent. Build and test one step before moving to the next.

## Step 1 — Project Shell
> "Create a new Kotlin Android app using Jetpack Compose called Vyra. Set up basic navigation with a bottom nav bar: Home, Chat, Vault, Tasks, Settings. Dark theme, accent color teal (#14B8A6)."

## Step 2 — Database Layer
> "Add Room database with two entities: VaultEntry (id, category, title, username, secret, extraField, notes, createdAt, updatedAt) and Task (id, title, dueAt, reminderEnabled, isDone, notes, createdAt). Create DAOs with insert, update, delete, getAll, search-by-title."

## Step 3 — Security Layer
> "Integrate Android Keystore to generate and store a master encryption key. Encrypt the `secret` field of VaultEntry before saving to DB and decrypt only when displayed. Add a PIN/biometric lock screen shown on app launch using BiometricPrompt API."

## Step 4 — Vault UI
> "Build the Vault screen with category tabs (Passwords, Accounts, Sites/APIs, Others), a search bar, and a list of entries per category. Tapping an entry opens a detail view with masked secret and a 'tap to reveal' button. Add a floating '+' button to open an Add Entry form matching the category fields."

## Step 5 — Chat/Assistant Screen
> "Build a chat-style screen. When the user types a save-intent message (e.g. 'save a password for X'), show an inline confirmation card asking for the required fields before writing to the database. When the user asks a retrieval question (e.g. 'what is my X password'), search the vault and show a masked result card."

## Step 6 — Tasks
> "Build the Tasks screen with Today/Upcoming/Done grouping. Add task creation with optional due date and reminder toggle. Use WorkManager to schedule a local notification at the due time."

## Step 7 — Backup/Export/Import
> "Add a Settings screen with 'Export Backup' and 'Import Backup' buttons. Export should serialize all vault entries and tasks into a JSON file, encrypt it with a user-provided export password, and open the system share/save sheet. Import should let the user pick a JSON file, enter the export password, decrypt, and merge into the local database."

## Step 8 — Polish
> "Add swipe-to-delete and edit on list items, empty states for each screen, and a dashboard summary on Home showing entry counts per category and pending task count."

## Step 9 — (Optional, later) Autofill Service
> "Implement Android's AutofillService so Vyra can suggest saved credentials when the user focuses a login field in another app, matching domain/package name to saved vault entries. Require unlock before any suggestion is shown."

---

**Reminder while prompting the agent:** at no point ask it to add background listening, accessibility-service-based input capture, or any auto-sync/auto-restore-on-install behavior — those aren't in this plan and would turn the app into something closer to spyware.
