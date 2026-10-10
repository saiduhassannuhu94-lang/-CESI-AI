# CESI AI

**Your Voice. Your Device.**

CESI is an Android voice-assistant project focused on natural voice interaction, device actions, contextual follow-up, and Hausa-aware language handling.

## Project status

**Development / public-preview preparation. Not a production release.**

CESI is being prepared for a limited public preview, but the current repository and debug APK must not be treated as a finished, production-ready assistant. Features listed below include foundations and partial implementations; availability depends on the exact command, Android version, permissions, default apps, and device restrictions.

## Current engineering baseline

- Kotlin + Jetpack Compose
- Android API 26 minimum SDK; API 36 target SDK
- Context-aware task execution foundation
- Deterministic natural-language fallback
- English speech recognition used for launch testing
- Device TTS voice selection with quality-aware fallback
- Floating assistant orb
- Voice wake service foundation
- Foundations for location, calling, contacts, messaging, web search, YouTube search, device controls, and history
- Risk-aware action planning and confirmation work in progress

**Important:** a recognized command is not proof that the requested action executed. Some capabilities are partial, conditional on permissions, or open another app rather than verifying an external side effect. Do not rely on CESI for emergency actions or other critical tasks.

## What must be true before a public production release

- [ ] Safety confirmation gate is merged with required checks passing.
- [ ] Unit tests, lint, and debug build pass on the current release candidate.
- [ ] Critical user flows are tested on real Android hardware, including permission denial and recovery.
- [ ] Actual execution and verification behavior is documented per capability.
- [ ] Data collection, retention, processing, notification access, microphone use, and permissions are audited and accurately disclosed.
- [ ] Privacy policy, support contact/page, and public product information are published.
- [ ] Release signing and Android App Bundle (AAB) pipeline are verified.
- [ ] Compatibility, crash, and ANR checks are completed.
- [ ] Public-facing feature claims match observed behavior.

See the [Public Preview Readiness Checklist](docs/2026/PUBLIC_PREVIEW_READINESS.md) for the tracked launch gates.

## Build

Use Android Studio / Gradle with JDK 17.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

A successful debug build is not equivalent to a signed production release or real-device validation.

## Architecture direction

Voice Input -> Speech Recognition -> Request Understanding -> Context + Memory -> Task Planner -> Safety / Confirmation -> Action Router -> Android Skills -> Verification -> Response + TTS

## Repository

GitHub is the source of truth for code, CI, and release history. Public launch preparation is tracked in the readiness checklist linked above.
