# CESI AI v0.5.0-test.1

Release channel: **test prerelease**. This is not a production or store release.

## Included
- Current merged WhatsApp reaction-command recognition and user-triggered WhatsApp Accessibility helper.
- Multi-task planner regression fixes.
- Compound YouTube/TikTok media search command recognition.
- Versioned APK release pipeline.

## Media behavior boundary
Commands such as `open YouTube search comedy and play it` and `open TikTok search comedy and play it` now remain a single structured media request. CESI opens platform-specific search results. In this test build it does not automatically choose a particular video or independently verify playback, so it asks the user to tap the intended result and does not claim autoplay succeeded.

## WhatsApp reaction testing
The parser recognizes Hausa/English reaction phrases including `ka daura reaction na dariya`. Actual reaction execution still needs a fresh WhatsApp notification context, Notification Access, explicit Accessibility consent and physical-device validation. A successful accessibility click is not proof that WhatsApp synced the reaction.

## Validation
The release workflow runs unit tests, Android lint and a debug APK build before publishing. BrowserStack upload and CI success do not replace real-device testing on the target phone.
