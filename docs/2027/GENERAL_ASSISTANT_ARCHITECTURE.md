# CESI General Assistant Architecture

## Goal

CESI is intended to behave as a general-purpose Android voice assistant. The user should be able to speak naturally in Hausa, English, or mixed language without learning a fixed command vocabulary.

The system should translate a user's goal into a safe, executable Android action plan, execute the plan through capabilities that are actually available on the device, and verify the result where verification is possible.

## Core pipeline

Voice / text
-> speech recognition
-> request normalization
-> intent and goal understanding
-> context resolution
-> action planning
-> capability and permission checks
-> risk / confirmation policy
-> Android execution
-> result verification
-> response / TTS
-> context update

## Architectural layers

### 1. Understanding
- No dependency on exact command phrases.
- Hausa, English, and mixed Hausa/English support.
- Preserve names, messages, search queries, and quoted text.
- Unknown requests remain explicit unknowns.
- Future AI/LLM reasoning must use a secure gateway; provider secrets never belong in the APK.

### 2. Context
Resolve references such as "open it", "search that", "call him", and "send it to her".
Context needs explicit lifetime and privacy controls.

### 3. Planning
Convert a structured request into ordered actions.
The planner must support dependencies, required capabilities, missing information, side-effect classification, and plan presentation when confirmation is required.

### 4. Capability layer
Reason from capabilities actually implemented and available on the device, such as app launch, media, camera, microphone, contacts, telephony, messaging, notifications, location, web search, device settings, and carefully scoped accessibility interaction.

Recognition of a command is never proof that a capability exists.

### 5. Execution
Every capability needs a real Android executor with clear inputs, permission checks, predictable success/failure results, and no fake success responses.

### 6. Safety and confirmation
High-impact external actions must not silently execute. This includes calls, messages, USSD, purchases, destructive actions, and security-sensitive settings.

### 7. Verification
Verify observable outcomes where Android provides a reliable signal. Do not report success merely because an API call returned.

### 8. Voice and background operation
Long-term target:

screen off -> wake -> speech -> understanding -> planning -> execution -> response

Use Android-supported assistant/background mechanisms and respect microphone, foreground-service, battery, and lock-screen restrictions.

## Testing strategy

The CESI web test lab and Android APK have different responsibilities.

### Web test lab
Test request normalization, semantic understanding, context resolution, planning, capability selection, risk decisions, plan visualization, and regression cases.

The web lab must never pretend to prove that an Android hardware action actually happened.

### Android APK
Test microphone, speech recognition, wake behavior, permissions, actual Android actions, notifications, camera, media, phone, messaging, screen-off behavior, and device-specific restrictions.

## Implementation order

1. Stabilize and document the current architecture.
2. Define structured request, plan, capability, execution-result, verification-result, and risk models.
3. Separate semantic understanding from Android execution.
4. Expand capability registry and executors.
5. Add a real planner with dependency handling.
6. Add context resolution with explicit TTL and privacy controls.
7. Add verification for supported actions.
8. Build the web test lab around the same pure planning/understanding contracts.
9. Add a secure AI gateway for broader natural-language reasoning.
10. Strengthen VoiceInteractionService/default-assistant and screen-off operation.
11. Add device-matrix testing and release hardening.

## Non-negotiable engineering rules

- Do not add one parser branch for every new sentence.
- Do not claim a capability works until its real executor is implemented and tested.
- Do not merge failing CI.
- Do not use AccessibilityService as an unrestricted autonomous engine.
- Do not put provider/API secrets in the APK.
- Do not silently perform high-risk external actions.
- Do not treat the web test lab as proof of Android execution.
- Keep basic device actions available through deterministic local fallbacks where practical.
- Every architectural change must have regression tests.
