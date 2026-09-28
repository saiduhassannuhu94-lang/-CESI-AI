# CESI AI

**Your Voice. Your Device.**

CESI is an Android voice-assistant project focused on natural voice interaction, device actions, contextual follow-up, Nigerian/Hausa-aware language handling, and a future system-assistant architecture.

## Current engineering baseline

- Kotlin + Jetpack Compose
- Android 16 target baseline (API 36)
- Context-aware task execution
- Deterministic natural-language fallback
- English speech recognition for launch testing
- Device TTS voice selection with quality-aware fallback
- Floating assistant orb
- Voice wake service foundation
- Location, calling, contacts, messaging, web search, YouTube search, device controls and history foundations

## Important product truth

The current APK is a launch-candidate / test build, not yet a finished mass-market system assistant.

The biggest remaining upgrades are:
1. A real AI brain/provider layer for open-ended language understanding.
2. Android VoiceInteractionService + VoiceInteractionSessionService for true default-assistant integration.
3. Production privacy, consent, account/data controls and store compliance.
4. Release signing + AAB pipeline.
5. Device matrix testing and crash/ANR monitoring.
6. A production product website and support/privacy pages.

## Build

Use Android Studio/Gradle 8.9+ with JDK 17.

    gradle testDebugUnitTest
    gradle assembleDebug

## Architecture direction

Voice Input -> Speech Recognition -> AI / Intent Brain -> Context + Memory -> Task Planner -> Security / Confirmation -> Action Router -> Android Skills -> Verification -> Response + TTS

## Repository

GitHub is the source of truth for code, CI and release history.

For public launch, the repository should be paired with a product website, privacy policy and support page. The website can live in website/ and be deployed through GitHub Pages or Vercel.

See docs/2027/ for the product audit and 2027 roadmap.
