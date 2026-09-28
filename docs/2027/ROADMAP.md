# CESI 2027 Roadmap

## Phase A — Launch Candidate
- Android 16 target (API 36)
- stable Compose UI
- persistent conversation surface
- location fix
- natural-language fallback
- English voice baseline
- adaptive icon + splash
- unit tests + CI
- README + privacy/support site

## Phase B — CESI 1.0
- secure AI gateway
- structured tool calling
- context-aware planner
- confirmation/risk engine
- offline fallback
- release signing + AAB
- crash and ANR monitoring
- onboarding
- analytics with user consent

## Phase C — 2027 CESI 2.x
- Android VoiceInteractionService
- VoiceInteractionSessionService
- default-assistant role
- contextual assist / screenshot pipeline
- vision understanding
- long-term memory with controls
- routines and automations
- App Actions / deep links where available
- stronger Hausa + English mixed speech
- creator/social analytics through official APIs
- premium natural voice provider
- multi-device expansion

## Non-negotiable rules
- Never store AI provider secrets in the APK.
- Never silently perform high-risk actions.
- Never request sensitive permissions without a feature need.
- Never use AccessibilityService as an unrestricted autonomous action engine.
- Always show what CESI heard and what it plans to do.
- Always keep a local fallback for basic device functions.
