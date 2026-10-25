# Vyra — Database Schema (Room / Local)

## 1. Tables

### `vault_entry`
| Field | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| category | String | passwords / accounts / sites / others |
| title | String | site/app/platform name |
| username | String? | nullable |
| secret | String (encrypted) | password / API key |
| extra_field | String? | recovery email, docs link, etc |
| notes | String? | |
| created_at | Long (timestamp) | |
| updated_at | Long (timestamp) | |

### `task`
| Field | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| title | String | |
| due_at | Long? | nullable, timestamp |
| reminder_enabled | Boolean | |
| is_done | Boolean | |
| notes | String? | |
| created_at | Long | |

### `chat_log` (optional, local only)
| Field | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| role | String | "user" / "assistant" |
| message | String | |
| timestamp | Long | |

## 2. Encryption Notes
- `secret` field encrypted with Android Keystore-derived key before insert
- Whole DB wrapped with SQLCipher (defense in depth)
- Decryption only happens in-memory after unlock, never logged

## 3. Category Enum
```kotlin
enum class VaultCategory {
    PASSWORDS, ACCOUNTS, SITES_APIS, OTHERS
}
```
(Tasks live in their own table, not vault_entry)

## 4. Backup JSON Structure
```json
{
  "export_date": "2026-10-25T12:00:00Z",
  "app_version": "1.0",
  "encrypted": true,
  "vault_entries": [ /* encrypted blobs */ ],
  "tasks": [ /* plain, non-sensitive */ ]
}
```
- File itself password-protected (user sets export password separately from app PIN)
- Import requires that export password to decrypt — nothing auto-restores

## 5. Indexing
- Index on `category` + `title` for fast search
- Index on `due_at` for task reminders query
