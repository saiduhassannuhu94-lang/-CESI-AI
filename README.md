# CESI AI

**Your Voice. Your Device.**

CESI is an Android voice-assistant project focused on natural voice interaction, device actions, contextual follow-up, and Hausa-aware language handling.

## Project status

**Development project. Public-preview preparation, not a production release.**

The repository includes working foundations and partial implementations, but the current APK must not be treated as a finished, production-ready general-purpose assistant. Availability depends on the exact command, Android version, permissions, default apps, and device restrictions.

A recognized command is not proof that the requested action executed. Some capabilities are partial, conditional on permissions, or open another app rather than verifying an external side effect. Do not rely on CESI for emergency actions or other critical tasks.

## Current engineering baseline

- Kotlin + Jetpack Compose
- Android minimum SDK 26; target SDK API 36
- Context-aware task execution foundation
- Deterministic natural-language fallback
- English speech recognition used for launch testing
- Device TTS voice selection with quality-aware fallback
- Floating assistant orb and voice-wake service foundation
- Foundations for location, calling, contacts, messaging, web search, YouTube search, device controls, and history
- Risk-aware action planning and confirmation workflow

## Before external distribution

CESI still needs passing safety/build checks, real Android device validation, a complete permissions and data-lifecycle audit, accurate privacy and support information, and a verified release package.

Track the evidence and remaining blockers in the [Public Preview Readiness Checklist](docs/2026/PUBLIC_PREVIEW_READINESS.md). A debug build passing CI does not equal a signed production release or proof of real-device behavior.

## Build

Use Android Studio / Gradle with JDK 17.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

## Architecture direction

Voice Input -> Speech Recognition -> Request Understanding -> Context + Memory -> Task Planner -> Safety / Confirmation -> Action Router -> Android Skills -> Verification -> Response + TTS

## Release principle

Do not describe a capability as complete until it has been tested on the relevant Android device and its result is verified. Do not publish privacy claims until they are confirmed against actual app behavior.
