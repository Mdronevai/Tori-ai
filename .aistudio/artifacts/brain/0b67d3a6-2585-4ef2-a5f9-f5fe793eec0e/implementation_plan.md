# Tori AI Assistant UI & Experience Redesign (Dola-Inspired Holographic HUD)

Complete visual and architectural redesign of Tori AI Assistant inspired by cutting-edge voice AI interfaces (Dola AI reference image). This upgrade elevates Tori with an animated iridescent nebula orb, an OLED black aesthetic, an immersive live voice mode with dynamic fluid audio waveforms, quick-command suggestion rows, an expandable AI Creation & Tools bottom sheet, and an expanded bilingual (Bangla + English) command library.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your answers and the uploaded reference design, the following choices have been confirmed:

- **Confirmed Decision 1 (Visual Theme & Central Orb)**: Pure OLED dark canvas (`#070709` / `#000000`) with an animated, fluid iridescent nebula orb (`NebulaOrbComposable`) utilizing dynamic Canvas shaders, pulsing gradients, and micro-rotations.
- **Confirmed Decision 2 (Immersive Live Voice Mode)**: Full-screen interactive voice session screen featuring multi-frequency undulating fluid waveforms (`FluidWaveformVisualizer`), live duration timer (`02:48`), radial bloom aura, animated ripple mic button, and live speech transcription.
- **Confirmed Decision 3 (Quick Command Chips & Creation Sheet)**: Two-row scrollable quick chips for Passwords, Tasks, Web Scan, Translate, and AI Prompt Creation, plus an expandable Creation & Tools modal sheet with tabs and one-tap generation.
- **Confirmed Decision 4 (Bilingual Command Vocabulary)**: Native parsing and responses for regular conversational commands in Bangla and English (e.g., *"Amar Facebook password ki?"*, *"Notun password banao"*, *"Remind me tomorrow at 4 PM"*, *"Translate this text"*).

---

## 1. Overview & Core Concept

- **What It Does**: Transforms Tori into an ultra-modern, interactive AI assistant experience matching high-end consumer AI apps. Users are greeted by a living holographic nebula orb, instant contextual quick-action chips, a voice mode with fluid audio waveforms, and seamless access to local vault secrets, tasks, and clipboard history.
- **Target Audience / Persona**: Users seeking a futuristic, privacy-first personal assistant with a visually stunning HUD, frictionless voice interaction, and rapid tool execution.
- **Key Value**: Delivers the visual delight and conversational responsiveness of top-tier AI apps (like Dola/ChatGPT Voice) while maintaining 100% on-device hardware encryption (AES-256) and local data sovereignty.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Primary Assistant Dashboard**:
     - *Header*: Frosted glass navigation pills—left menu button (`☰`), center glowing badge (`✨ Tori AI Assistant`), right audio output selector (`🔊`).
     - *Centerpiece*: Animated holographic nebula orb that responds with breathing, pulsing, and color shifts based on assistant states (Idle, Listening, Thinking, Responding).
     - *Greeting & Context*: Dynamic personalized header (*"Hi, I'm Tori!"*, *"How can I help you today?"*).
     - *Two-Tier Quick Action Chips*:
       - Row 1: `Fast AI`, `Homework / Tasks`, `AI Creation`, `Vault Secrets`
       - Row 2: `Scan Page`, `Edit Prompt`, `Translate`, `Find Password`, `API Keys`
     - *Bottom Floating Bar*: Pill-shaped input capsule with `+` tool drawer button, message field, scanner/camera button, and radiant gradient mic orb (`#E11D48` to `#9333EA`).
  2. **Immersive Live Voice Mode Screen**:
     - Tap the mic orb or say voice trigger -> Full-screen dark violet bloom canvas appears.
     - Fluid undulating multi-strand waveform visualizer undulates in real time according to speech input.
     - Live duration counter (`00:00` -> `02:48`).
     - Concentric pulse rings surrounding the glowing central mic button.
     - Controls for Pause (`⏸`), Mute, and Exit (`✕`).
  3. **Expandable AI Creation & Tools Sheet**:
     - Tap `+` or `AI Creation` chip -> Frosted dark modal bottom sheet slides up with tabs (`Image/Vision`, `Prompt`, `Vault Tool`, `Code`).
     - Model badge indicator (`Gemini 3.5 Flash / Local Engine`), style options, and single-click full-width gradient pill button (`Execute Command`).
  4. **Integrated Side Navigation Drawer**:
     - Instant drawer access to Vault Entries, Permanent Clipboard, Reminder Tasks, and System Settings.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Cyber-Luxury OLED Minimalist (Pure Blacks + Iridescent Holographic Accents).
  - *Color Tokens*:
    - Background: `#060709` (OLED Obsidian)
    - Surface / Cards: `#12151D` (Frosted Dark Glass with 1dp border `#1E2536`)
    - Neon Accents: `#A855F7` (Electric Violet), `#EC4899` (Magenta Rose), `#06B6D4` (Holographic Cyan), `#F59E0B` (Amber Flame)
    - Gradient Mic Orb: Linear gradient `[#FF007A, #7928CA, #0070F3]`
  - *Typography*: Monospace for system stats/timers; Modern rounded sans-serif for greetings and bilingual prompts.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Canvas Nebula Orb vs. Heavy Video Assets**
  - *Chosen Approach*: Lightweight procedural Canvas-based fluid nebula orb using Compose `drawWithCache` and animated trigonometry rings + radial blur shaders.
  - *Why*: Ultra-smooth 60fps performance without bloating APK size or consuming excessive memory.
- **Decision 2: Dual-Mode Navigation (Orb Home + Drawer)**
  - *Chosen Approach*: Streamline the bottom bar into the unified floating control bar from the reference design, while providing an aesthetic navigation drawer for Vault, Clipboard, Tasks, and Settings.
  - *Why*: Eliminates clutter from having too many tabs visible simultaneously while putting the interactive AI persona front and center.
- **Decision 3: Expanded Natural Language Command Matcher**
  - *Chosen Approach*: Enhanced regex & keyword intent parser in `ToriAssistantEngine` supporting everyday Bengali & English commands (passwords, emails, tasks, web scanning, translations, prompt drafting) with instant offline execution and Gemini AI fallback.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Tori Redesigned UI Shell                        │
│   (OLED Obsidian Background • Frosted Glass Pills • Edge-to-Edge)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
           ┌────────────────────────┼────────────────────────┐
           ▼                        ▼                        ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│  NebulaOrbComposable │ │  LiveVoiceModeScreen │ │   CreationToolSheet  │
│ - Animated Shaders   │ │ - Fluid Waveform     │ │ - Tool Drawer Tabs   │
│ - Breathing / Pulse  │ │ - Radial Glow Aura   │ │ - Model Selector     │
│ - Holographic Mesh   │ │ - Concentric Rings   │ │ - Action Execution   │
└──────────────────────┘ └──────────────────────┘ └──────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   Unified Assistant Quick Chip Rails                   │
│  [Fast AI] [Homework/Tasks] [Vault] [Scan Page] [Translate] [API Keys] │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│              ToriAssistantEngine & GeminiService Layer                 │
│  - Bilingual Bangla/English Natural Language Parsing                   │
│  - Vault & Task Auto-Actions • Gemini 3.5 Flash Reasoning              │
└────────────────────────────────────────────────────────────────────────┘
```

### Components to Create / Update:

1. **`NebulaOrbComposable.kt`** (`com.example.ui.components`):
   - Procedural animated glowing nebula sphere with dynamic multi-color gradient sweeps, breathing scale, and rotation.
2. **`LiveVoiceModeScreen.kt`** (`com.example.feature.voice`):
   - Immersive full-screen voice UI with dynamic fluid audio waveform visualizer, timer, radiant ambient aura, and control buttons.
3. **`CreationToolSheet.kt`** (`com.example.feature.tools`):
   - Bottom sheet for quick creation, prompt templating, vision scanner, and tool execution.
4. **`DashboardScreen.kt`**:
   - Upgraded to feature the reference design layout: top frosted header, central nebula orb, dynamic greeting, two-tier suggestion chips, and floating pill input with glowing gradient mic.
5. **`ToriAssistantEngine.kt`**:
   - Expanded with common everyday commands (Bangla + English) for instant retrieval, task creation, password generation, translation, and clipboard analysis.
6. **`Theme.kt`**:
   - Updated palette to match the reference deep OLED black, holographic violet, magenta, and cyan accents.
