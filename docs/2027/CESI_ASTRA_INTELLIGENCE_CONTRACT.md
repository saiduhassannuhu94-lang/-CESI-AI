# CESI Astra Intelligence Contract

## Purpose
Introduce a provider-independent AI reasoning interface to CESI before choosing or deploying the remote gateway. This allows the app's task engine to remain independent of provider details and supports offline or unconfigured fallbacks.

## Contract
- `CesiBrain` accepts an `AssistantRequest` and optional conversation turns.
- The brain returns one of: `Conversation`, `ActionProposal`, `Clarification`, or `Unavailable`.
- Action proposals use the existing `ActionType` enum and parameter map. They are proposals only, never execution results.
- CESI must validate every proposed action against its local capability registry and current device state.
- Risk must be recalculated locally. Never trust the model-provided risk value.
- High-impact or external side effects still go through the existing confirmation manager.
- Action execution and verification remain in the local ActionRouter pipeline.
- Offline/not-configured/provider/rate-limit/invalid-response states are explicit so the deterministic parser can be used as a fallback.

## Integration sequence
1. Contract and tests (this milestone).
2. Gateway response schema and server implementation, without provider secrets in the APK.
3. Client gateway adapter with bounded timeouts, response-size limits, and safe error mapping.
4. Connect AI understanding to task planning behind an opt-in switch.
5. Test prompt injection, mixed Hausa/English, malformed responses, unsupported capabilities, ambiguous contacts, multi-step actions, timeouts, and offline fallback.
6. Validate on the target device before describing the feature as active.

## Model name and access
The existing gateway source contains a model identifier `gpt-6-astra`. That identifier and provider access must be confirmed against the provider's current API availability before deployment. The Android app must not assume a model is reachable because the identifier exists in source.
