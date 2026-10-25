# Vyra — UI/UX Design Plan

## 1. Design Language
- Minimal, dark-first theme (easy on eyes, feels "assistant-like")
- Accent color: electric blue/teal (Jarvis-ish glow feel)
- Rounded cards, soft shadows, no clutter
- Typography: clean sans-serif (Inter / Poppins)

## 2. Screen List

### A. Lock Screen
- App logo + "Unlock Vyra"
- Biometric prompt (fallback PIN)
- No content visible until unlocked

### B. Home / Dashboard
- Greeting ("Hey Rony 👋")
- Quick stats: X passwords, Y accounts, Z tasks pending
- Shortcut buttons: + Add Entry | Ask Vyra | Tasks
- Recent activity list (last 5 added/edited entries)

### C. Chat / Assistant Screen
- Chat bubble UI (like a messaging app)
- User can type or speak: "save a password for X" / "what's my Gmail recovery email"
- Vyra responds, and if it's a save action → opens confirmation card inline
- Confirmation card: Category dropdown + fields + Save/Cancel buttons

### D. Vault Screen (Category Tabs)
```
[Passwords] [Accounts] [Sites/APIs] [Tasks] [Others]
```
- List view per category, search bar on top
- Tap entry → detail view (masked password, tap to reveal, copy button)
- Swipe to edit/delete

### E. Entry Detail / Add-Edit Form
- Category-specific fields:
  - Passwords: Site/App name, Username, Password, Notes
  - Accounts: Platform, Email, Recovery info, Notes
  - Sites/APIs: Name, API Key, Docs link, Notes
  - Tasks: Title, Due date/time, Reminder toggle, Notes
  - Others: Free-form title + note

### F. Tasks Screen
- Checklist style, grouped by Today / Upcoming / Done
- Tap to mark complete, long-press to edit

### G. Settings / Backup Screen
- Change master PIN/biometric
- Export Backup (generates encrypted JSON, opens system share/save sheet)
- Import Backup (pick file → enter password → restore, manual only)
- About / data location info

## 3. Interaction Rules
- Every destructive action (delete) → confirm dialog
- Every save action from chat → confirmation card, never silent
- Password fields masked by default, explicit tap-to-reveal

## 4. Navigation Pattern
Bottom nav bar: **Home | Chat | Vault | Tasks | Settings**
