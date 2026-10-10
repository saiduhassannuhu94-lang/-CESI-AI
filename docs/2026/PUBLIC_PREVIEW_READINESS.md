# CESI Public Preview Readiness Checklist

**Current decision:** CESI can be introduced as a development project, but external distribution of a public-preview APK must wait until the safety, privacy, and real-device gates below are supported by evidence.

## Rules
- A missing CI result is unknown, not a pass.
- A browser simulation does not prove an Android action works.
- Recognizing a command does not prove the requested side effect happened.
- Do not publish privacy promises until they are verified against actual code and configuration.
- Do not merge failing CI. Do not claim device testing unless it actually happened.

## Gate A: engineering and safety
- [ ] Safety confirmation gate is merged and required CI checks pass.
- [ ] Current main passes unit tests, lint, and debug APK build.
- [ ] High-impact actions require confirmation; confirmation is single-use and decline/cancel/expiry stop execution.
- [ ] Each executor returns a structured result; NOT_CHECKED is not treated as success.
- [ ] Release candidate and test evidence are tied to exact commit SHA.

## Gate B: real Android validation
- [ ] Install the candidate on the target Infinix device; record Android version and app version.
- [ ] Test first launch, microphone permission grant/denial, and recovery from denied permissions.
- [ ] Test notification access as an explicit opt-in, including denial.
- [ ] Test advertised camera, location, contacts, call, message, and device-control paths only with required permissions.
- [ ] Test confirmation success, duplicate confirmation, decline, cancel, unclear reply, and expiry.
- [ ] Verify each action against the actual device; record failures and limitations.
- [ ] Test screen-off/wake behavior without assuming Android or OEM restrictions can be bypassed.
- [ ] Ensure CESI never reports an unverified side effect as successful.

## Gate C: privacy and security
- [ ] Inventory every declared permission and trace it to the code that uses it.
- [ ] Review microphone foreground services, wake behavior, notification listener, contacts/call-log, camera, location, overlays, and call-state receiver.
- [ ] Review exported components and ensure the narrowest appropriate exposure.
- [ ] Review local history, conversation context, data retention, deletion behavior, and backup/transfer behavior.
- [ ] Verify no provider/API secrets are present in source, build outputs, or APK.
- [ ] Publish a privacy policy that accurately describes processing, storage, retention, sharing, and deletion.
- [ ] Publish support/contact information and a safe bug-report process.
- [ ] Review relevant Google Play policy and target API requirements before store distribution.

### Initial source findings
- The WhatsApp notification listener reads notification title/text, remembers the latest message for reply suggestions, and may speak the message aloud through TTS.
- Previously, the listener also wrote the sender and message body into general assistant history. A focused remediation is proposed in PR #32; do not count it complete until its checks pass and it is merged.
- Local conversation context is stored in SharedPreferences with a five-minute logical TTL. Expired data is cleared when that context is accessed after expiry; immediate physical erasure at the TTL boundary and encryption at rest are not established.
- Automatic backup was enabled in the manifest. PR #32 proposes disabling it; verify the final merged manifest and Android-version behavior.
- Sensitive permissions and the exported incoming-call receiver still need a complete usage/security review. Their presence alone does not prove a vulnerability.

These are observations from source review, not a claim that every item is an exploitable vulnerability. Resolve each item with code evidence and tests, or document a reasoned risk acceptance.

## Gate D: public preview package
- [ ] Public description identifies the app as a limited preview/test build, not a finished general-purpose assistant.
- [ ] Features are labelled implemented, partial, conditional, or not implemented based on evidence.
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
**Limited public preview:** only after the safety gate, passing required CI, real-device smoke tests for the advertised scope, and accurate privacy/support disclosures.

**Production/store release:** only after all applicable gates above, including release signing, privacy/security review, compatibility testing, and store-policy review.
