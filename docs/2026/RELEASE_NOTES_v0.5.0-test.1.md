# CESI AI v0.5.0-test.1

Release channel: **test prerelease**. This is not a production or store release.

## Included
- More robust Hausa/English WhatsApp reaction-command recognition, including speech-candidate recovery for `daura`.
- User-triggered WhatsApp reaction helper with explicit Accessibility disclosure.
- Conservative multi-task planning and regression tests.
- Compound YouTube/TikTok search-and-play command recognition.
- A 45-second, one-shot Accessibility attempt to select the first visible media result after the exact query is visible, limited to the app requested by the user.
- Versioned APK release publishing.

## WhatsApp reaction: requirements and boundaries
The parser covers commands such as `ka daura reaction na dariya`, `ka saka wa wannan sakon reaction na dariya`, and `react with funny emoji`.

To test a reaction, CESI must have Notification Access enabled and have a recent WhatsApp notification context for the target message. The separate Accessibility disclosure must also be accepted and the CESI Accessibility service enabled. If Hausa speech recognition is poor, select Hausa as the voice-input language in CESI Settings; English recognition can mishear Hausa words. The assistant should state when target-message context or permission is missing rather than implying the reaction was sent.

A successful UI tap is not proof that WhatsApp applied or synced the reaction. WhatsApp UI matching still requires physical-device validation.

## Media search-and-play: requirements and boundaries
Commands such as `open YouTube search comedy and play it` and `open TikTok search comedy and play it` are preserved as one structured request rather than split into unrelated actions.

When the user explicitly requests playback, CESI asks for the expanded Accessibility permission if it is not enabled. After consent and service enablement, it opens the requested platform search and attempts to select the first visible, plausibly sized result only when the query can be found in the accessible UI. The request expires after 45 seconds and is cleared when the service is interrupted, the user cancels, or the result is clicked.

If the app's UI does not expose a sufficiently clear result, CESI intentionally does not guess. Browser fallback still requires the user to tap a result manually. Even when a result is clicked, CESI cannot independently prove the video started or continued playing. YouTube/TikTok UI hierarchies vary by version and device, so this needs a real-device test.

## Verification
Unit tests, Android lint and APK compilation validate code/build contracts, not physical-device behavior. BrowserStack upload is not itself proof that the command succeeded in the target Android apps. This release remains a test prerelease until actual WhatsApp reaction and media selection are exercised on the target phone.
