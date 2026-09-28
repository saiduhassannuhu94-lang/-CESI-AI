# CESI Product Audit — Launch Candidate to 2027

## Executive finding

CESI has a real Android foundation, but it is not yet comparable to a mature consumer assistant.

The project already has useful ingredients: voice input, foreground services, overlay UI, context storage, task execution, Android actions, TTS, notification integration, permission flows, and a Compose UI.

The largest gap is not "more commands". It is the brain-to-action architecture: the foreground UI and background service have historically used different execution paths, and the current understanding layer is deterministic pattern matching rather than a true semantic AI layer.

## What is strong

### Product concept
The proposition is clear: a voice-first Android assistant with a strong African/Hausa-aware direction.

### Feature surface
The codebase covers more device actions than a toy voice-command demo: camera/selfie, calls, contacts, location, messaging foundations, Google/YouTube search, volume, flashlight, battery, settings, notifications, background response and local history.

### Context foundation
The project has conversation context and ContextTaskEngine. This is the right direction for follow-up commands.

### UI foundation
Jetpack Compose is a sensible long-term UI choice. The current redesign moves the UI toward a dark assistant surface with a persistent transcript/response area and a reactive orb.

## Critical weaknesses

### 1. No real semantic AI brain yet
Natural-language fallback can improve phrase tolerance, but it cannot replace an LLM or other semantic model.

For the 2027 architecture, define a structured AI contract:

- intent
- entities
- steps
- capabilities
- risk
- confirmation
- expected result

The APK must never contain a private provider API key. A backend or secure proxy should own provider credentials.

### 2. Not yet a true system assistant
The current background approach is based on foreground microphone services and Android SpeechRecognizer.

The 2027 architecture should add Android VoiceInteractionService and VoiceInteractionSessionService so users can select CESI as the system assistant.

### 3. One orchestration path is required
The product should have one pipeline for all surfaces:

Voice -> Brain -> Context -> Plan -> Security -> Execute -> Verify -> Respond.

Avoid maintaining separate foreground and background execution logic.

### 4. Production release pipeline is incomplete
The current CI creates a debug APK. Production needs:
- release signing
- AAB
- versioning
- Play upload
- crash/ANR monitoring
- release notes
- privacy policy
- Data safety declaration
- device-matrix tests

### 5. Permissions must stay contextual
CESI touches microphone, contacts, location, calls, call logs and notifications.

Each permission should be requested just before the feature needs it, with a clear explanation and a fallback path.

### 6. Accessibility cannot become unrestricted automation
Android/Google Play places strict limits on autonomous use of AccessibilityService. CESI should not rely on AccessibilityService for general autonomous planning and execution in a public Play release.

### 7. Brand system needs consistency
The launch candidate should have:
- one icon system
- one mark
- one type scale
- consistent cyan/indigo assistant palette
- consistent motion language
- clear idle/listening/thinking/speaking/error states
- no random emoji as primary navigation

## UX direction

The assistant screen should feel like a focused conversation surface, not a settings dashboard.

Priority hierarchy:
1. CESI identity
2. current state
3. transcript
4. response
5. primary microphone interaction
6. secondary actions
7. settings/history

The orb should communicate state through motion:
- idle: slow breathing
- listening: microphone-reactive
- thinking: flowing motion
- speaking: wave/pulse
- error: restrained warning state

## 2027 product model

### CESI 0.5 — Launch Candidate
Stable core controls, permissions, contextual follow-ups, English voice, location, conversation display, CI and clean branding.

### CESI 1.0 — Public Assistant
Production AI backend, structured tool calling, secure confirmation, release AAB, privacy/support, telemetry, onboarding and crash recovery.

### CESI 2.x — 2027 Assistant Platform
Default-assistant integration, vision/context, memory, personalized routines, app integrations, creator analytics and deeper Hausa/English mixed-language support.

## Success definition

A user should be able to speak naturally, have CESI understand the intention, ask a clarification only when genuinely necessary, perform the action safely, verify the result, and explain what happened in normal human language.
