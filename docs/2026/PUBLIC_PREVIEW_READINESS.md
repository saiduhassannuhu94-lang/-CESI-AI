# CESI Public Preview Readiness Checklist

**Current decision:** CESI is a development/test project. Do not distribute a public-preview APK as a finished assistant until the safety, privacy, and real-device gates below have evidence.

## Rules
- A missing CI result is unknown, not a pass.
- A browser simulation does not prove an Android action works.
- Recognizing a command or clicking a UI control does not prove the requested side effect happened.
- Do not publish privacy promises until they are verified against actual code and configuration.
- Do not merge failing CI. Do not claim device testing unless it actually happened.

## Gate A: engineering and safety
- [x] Safety confirmation gate is merged; it is single-use and expires/cancels safely.
- [ ] Verify current `main` unit tests, lint, and debug APK build against the exact current SHA before cutting a release.
- [x] High-impact calls and direct notification replies require confirmation in the current implementation.
- [x] Structured execution results distinguish success, failure, blocked, partial, unknown, and confirmation-needed states.
- [x] Per-capability verification avoids treating `NOT_CHECKED` as success.
- [ ] Record each release artifact against its exact commit SHA and checksum.

## Gate B: real Android validation
- [ ] Install the candidate on the target Infinix device; record Android version and app version.
- [ ] Test first launch, microphone permission grant/denial, and recovery from denied permissions.
- [ ] Test notification access as an explicit opt-in, including denial.
- [ ] Test advertised camera, location, contacts, call, messaging, and device-control paths only with required permissions.
- [ ] Test confirmation success, duplicate confirmation, decline, cancel, unclear reply, and expiry.
- [ ] Test WhatsApp direct-message drafts and notification replies; verify the UI state and confirm that drafts are not sent automatically.
- [ ] Test WhatsApp reactions against the installed WhatsApp version, including repeated identical messages, missing notifications, denied Accessibility access, and a reaction that cannot be uniquely found.
- [ ] Verify each action against the actual device; record failures and limitations.
- [ ] Test screen-off/wake behavior without assuming Android or OEM restrictions can be bypassed.
- [ ] Ensure CESI never reports an unverified side effect as successful.

## Gate C: privacy and security
- [x] WhatsApp notification text is kept in process memory with a five-minute logical TTL, rather than the old persistent message-context preferences.
- [x] General assistant history records a generic WhatsApp notification event rather than the sender or message body.
- [x] Speaking WhatsApp notification content is a separate user preference and defaults off.
- [x] Android automatic backup is disabled and explicit backup/device-transfer exclusions are configured for CESI history/context preferences.
- [x] Unneeded call-log/answer-call/wake-lock declarations were removed in the privacy hardening change.
- [ ] Review remaining locally stored conversation/topic context, retention, and user-controlled deletion.
- [ ] Inventory every remaining declared permission and trace it to code that uses it.
- [ ] Review exported components, microphone foreground services, notification listener, contacts, camera, location, overlays, and call-state behavior.
- [ ] Verify no provider/API secrets are present in source, build outputs, or APK.
- [ ] Publish a privacy policy that accurately describes processing, storage, retention, sharing, and deletion.
- [ ] Publish support/contact information and a safe bug-report process.
- [ ] Review relevant Google Play policy and target API requirements before store distribution.

### Current privacy boundaries
The merged notification privacy work minimizes storage, but it does not prove secure memory zeroization or forensic erasure at the five-minute TTL boundary. Conversation/topic context remains locally stored, though configured to be excluded from backup/transfer; user-controlled retention and deletion still need review. Notification access remains a powerful OS-level permission and must be disclosed accurately.

## Gate D: public preview package
- [ ] Public description identifies CESI as a limited preview/test build, not a finished general-purpose assistant.
- [ ] Every feature is labelled implemented, partial, conditional, or not implemented based on evidence.
- [ ] Privacy policy, support page/contact, and installation instructions are accessible before download.
- [ ] Candidate is built from a known commit; checksum and version are recorded.
- [ ] Release signing and Android App Bundle pipeline are verified before any production/store release.
- [ ] Rollback/revocation plan exists for a problematic build.

## Failure-learning record
For every failed test or release blocker, record:
1. Exact observed failure.
2. Root cause.
3. Why the earlier check missed it.
4. Fix and regression test.
5. Prevention/safeguard.
6. Lesson carried forward.

## Release decision
**Limited public preview:** only after the safety gate, passing required CI for the exact candidate, real-device smoke tests for the advertised scope, and accurate privacy/support disclosures.

**Production/store release:** only after all applicable gates above, including release signing, privacy/security review, compatibility testing, and store-policy review.
