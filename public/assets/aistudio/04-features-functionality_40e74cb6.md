# Vyra — Features & Working System

## 1. Core Feature: Confirmation-Based Save
**Flow:**
1. User says/types: "Save my Facebook password"
2. Vyra parses intent → category = Accounts
3. Vyra asks: "Email ar password diye dao" (fields needed)
4. User provides → Vyra shows confirmation card
5. User taps "Save" → encrypted → stored
6. **No step is skipped. No auto-save ever.**

## 2. Core Feature: Ask/Retrieve
**Flow:**
1. User: "Amar Netflix password ki?"
2. Vyra searches vault by title match
3. If found → shows masked result with "tap to reveal" (re-auth may trigger for sensitive reveal)
4. If multiple matches → asks which one

## 3. Core Feature: Task & Reminders
- Add task via chat or Tasks screen
- Local notification via WorkManager at due time
- Mark done / snooze / delete

## 4. Core Feature: Search
- Global search bar (Vault screen) across all categories
- Fuzzy match on title/username

## 5. Core Feature: Manual Backup/Export
- "Export Backup" button → generates encrypted JSON
- User manually shares to Google Drive / saves locally
- **No automatic cloud sync, no background upload**

## 6. Core Feature: Manual Restore
- "Import Backup" → pick JSON file → enter export password → data merges into local DB
- Always requires explicit user action + password, even on same device

## 7. Optional Phase 2: Autofill Integration
- Implements Android's `AutofillService`
- When user opens login field in another app/browser, Android OS offers Vyra's saved credential as suggestion (same mechanism as Google Password Manager)
- User still taps to confirm — no silent fill
- This replaces the need for "typing capture" entirely, safely

## 8. Assistant Personality/Behavior Rules
- Always confirms before writing data
- Always masks sensitive fields by default
- Never claims to have "full device access"
- If asked to do something outside scope (e.g., "control my screen"), responds that it's out of scope

## 9. Future Ideas (not in v1)
- Voice-only mode
- Category tagging/favorites
- Password strength checker + breach check (local-only, no API call unless user opts in)
