# CESI Public Preview Readiness Checklist

**Purpose:** track the evidence required before CESI is introduced publicly as a limited preview, and the stricter gates required before calling it production-ready.

**Current decision:** preparation can be public, but do not distribute the current debug APK as a production release or describe CESI as a finished general-purpose assistant.

## Rules for this checklist

- Mark a gate complete only when there is observable evidence: a passing required CI run, reviewed code, a published page, or a recorded device test.
- A missing workflow result is unknown, not a pass.
- A browser simulation does not prove an Android action works.
- A command being recognized does not prove its requested side effect happened.
- Do not publish a privacy promise until the app's actual behavior has been audited.
- Do not publish signing keys, API keys, tokens, or other secrets in the repository or APK.

## Gate A: engineering safety and build

- [ ] Safety confirmation gate PR is reviewed, required checks pass, and the change is merged.
- [ ] Current main branch passes unit tests.
- [ ] Current main branch passes lint.
- [ ] Current main branch builds a debug APK.
- [ ] CI results and APK artifact are tied to the exact commit intended for testing.
- [ ] Transitional string-to-result classification is removed only after every relevant executor returns structured results and regression tests cover the migration.

**Evidence to record:** commit SHA, workflow run URL/status, test/lint/build results, artifact name and checksum.

## Gate B: real Android validation

- [ ] Install the candidate APK on the target Infinix device and record Android version.
- [ ] First launch and onboarding complete without a crash.
- [ ] Microphone permission granted and denied paths are tested.
- [ ] Notification access is opt-in, clearly explained, and denial does not crash the app.
- [ ] Camera, location, contacts, calls, and messaging flows are tested only with the permissions each flow needs.
- [ ] High-impact actions ask for confirmation and cannot be triggered twice by one confirmation.
- [ ] Decline, cancel, unclear confirmation, and confirmation expiry all stop execution.
- [ ] App launch, flashlight, volume, battery/time queries, and other low-risk actions are checked against actual device behavior.
- [ ] Screen-off/wake behavior is tested without assuming Android or OEM battery restrictions can be bypassed.
- [ ] No unsupported action is reported as successful.

**Evidence to record:** device model, Android version, steps, expected outcome, actual outcome, screenshots/logs where appropriate, and known limitations.

### Permission audit note

- `READ_CALL_LOG` is used by the caller-ID permission flow alongside phone state and contacts. Do not remove it solely because a text search found no call-log query; Android's incoming-number delivery has platform restrictions, and the caller-ID path needs device validation. Its use must be checked against current Google Play eligibility/policy before store distribution.
- `ANSWER_PHONE_CALLS` had no source-code use in the repository search and has been removed from the manifest. Reintroduce it only if an implemented, tested capability actually needs it.

## Gate C: privacy and security audit

- [ ] Inventory every requested permission and document the feature that requires it.
- [ ] Audit microphone foreground services, wake behavior, notification listener, call-state receiver, contacts/call-log access, camera, location, and overlay permission.
- [ ] Review whether each permission is essential; remove unnecessary permissions only after checking all usages and tests.
- [ ] Review exported components and intent filters against the app's actual requirements.
- [ ] Review backup behavior and whether sensitive local data could be included in backups.
- [ ] Document what data is processed on-device, sent to network services, stored, retained, and deleted. Verify each claim in code/configuration.
- [ ] Verify there are no provider/API secrets in source, build outputs, or APK.
- [ ] Add an accurate privacy policy and support contact/page before external distribution.
- [ ] Review applicable Google Play policy and target API requirements before store publication.

**Important:** do not write a generic privacy policy that promises “no data collection” or “all processing stays on-device” unless an audit proves it.

## Initial source review: privacy/security items requiring a decision

These are observations from the current source, not claims that a vulnerability has been proven. They must be resolved or explicitly accepted with evidence before external distribution.

- `AndroidManifest.xml` declares `android:allowBackup="true"`. Review which app data can enter Android backup/transfer flows and add explicit backup/data-extraction rules if required by the app's data sensitivity.
- The WhatsApp notification listener reads notification title/text, passes incoming content to `MessageCopilotEngine.rememberIncoming(...)`, writes a message containing the notification text to `HistoryStore`, and may speak the content through TTS. Confirm the feature is clearly opt-in, explain that messages can be stored and spoken aloud, define retention/deletion behavior, and prevent sensitive content from being spoken unexpectedly.
- The manifest declares sensitive permissions including contacts, precise/coarse location, phone state, call log, call placement/answering, camera, microphone, notification access, and overlay. Audit each permission against actual call sites and remove unused permissions only after checking usages and regression tests.
- `IncomingCallReceiver` is declared exported. Review whether exporting it is necessary and whether its broadcast inputs can be trusted; add the narrowest appropriate protection and tests if required. Do not assume the exported flag alone proves exploitability.
- The current source includes user-facing permission explanations, but copy alone is not evidence that the runtime permission flow, denial handling, data lifecycle, and privacy disclosures are complete.

No permission or backup configuration has been changed as part of this documentation pass. Changes to security-sensitive Android behavior need a usage search, regression tests, and real-device validation.

## Gate D: public-preview package and communication

- [ ] Public description says this is a limited preview / test build and clearly lists known limitations.
- [ ] Public feature claims distinguish implemented, partial, conditional, and not-yet-implemented capabilities.
- [ ] Distribution method and installation instructions are documented.
- [ ] A versioned candidate is produced from a known commit.
- [ ] Release signing and AAB build are verified before any production/store release.
- [ ] Support/bug-report path is available and does not request passwords, access tokens, or unnecessary personal data.
- [ ] A rollback/revocation plan exists for a problematic build.

## Release decision

### Limited public preview
Proceed only after the safety gate, passing required CI, real-device smoke tests for the advertised preview scope, and an accurate privacy/support disclosure are complete.

### Production / store release
Proceed only after all applicable gates above are complete, including release signing, privacy/security review, compatibility testing, and store-policy review.

## Failure-learning record

For each failed test or launch blocker, record:

1. Exact observed failure.
2. Root cause.
3. Why the earlier check did not catch it.
4. Fix and regression test.
5. Prevention/safeguard.
6. Lesson carried forward.

This record is part of release readiness, not optional paperwork.
