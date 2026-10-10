# CESI Privacy Notice — Internal Review Draft

> **NOT FOR PUBLICATION.** This document is an evidence-based working draft, not the final privacy policy. It contains unresolved items that must be verified and filled in by the product owner before CESI is distributed outside a controlled test group. Do not present this file as legal advice or as proof that the app complies with a particular law or store policy.

**Product:** CESI AI for Android  
**Version covered:** [confirm release version]  
**Effective date:** [insert before publication]  
**Responsible entity / data controller:** [insert legal name and country]  
**Privacy contact:** [insert monitored support/privacy email or web form]

## 1. What CESI does

CESI is an Android voice-assistant project. Depending on the enabled feature, it can use microphone-based speech recognition, spoken responses, local command history, saved contacts, location, camera launch/capture flows, device overlays, and WhatsApp notification access.

Features and permission behavior vary by Android version, device manufacturer, installed apps, and user-granted permissions. Some advertised capabilities are partial or still under development.

## 2. Information the current code can process

Source review has identified the following categories. The release owner must verify this inventory against the final build.

- **Voice input and transcripts:** CESI uses Android's SpeechRecognizer. The current recognition requests set EXTRA_PREFER_OFFLINE to false, so recognition may use a network-backed speech service selected by Android. The exact provider, transmission behavior, and provider retention terms depend on the device/configuration and have not yet been established for this release.
- **Command/response history:** CESI stores up to 50 recent history entries in local app preferences. Entries contain a user/command field, assistant response, and timestamp. The history UI has a Clear action.
- **WhatsApp notifications:** when notification access is enabled, the listener reads WhatsApp notification titles and text. The current implementation can store notification content in command history, save the latest sender/message in local copilot context, and speak the notification text and reply suggestions aloud.
- **Short-lived conversation context:** the copilot checks a five-minute time window, but expiry cleanup occurs when that context is read. The release must verify whether stale values can remain stored when the feature is not used again.
- **Conversation/topic context and settings:** CESI stores context used for follow-up commands and selected voice settings in local app preferences.
- **Contacts and caller identification:** when the related feature is used and permissions are granted, CESI can look up saved contacts and process phone-state/incoming-number information where Android provides it.
- **Location:** when requested and permitted, CESI reads device location.
- **Camera:** camera functionality is available through a camera flow when the user invokes it and grants permission.
- **Network processing:** the app declares Internet access. This draft does not yet establish a complete inventory of every network call or third-party SDK in the final build.

## 3. Where information is stored or sent

The code reviewed stores history and conversational context in Android app-private SharedPreferences. A proposed security change disables app backup and excludes selected context files from backup/device-transfer flows; that change must be merged and tested before this statement can describe a release build.

Speech recognition may be handled by an Android-selected recognition provider because offline-only recognition is not requested. The exact provider and its processing/retention practices must be identified for the supported device configuration.

CESI may use the Android-selected text-to-speech engine to speak responses. Notification text and caller names can therefore be audible to people nearby. Users should avoid enabling notification reading in private or shared spaces if that is not appropriate for them.

**Unresolved before publication:** complete network/SDK review, identify any processors and their terms, confirm whether any app data leaves the device, confirm backup behavior on supported Android versions/OEMs, and verify storage/deletion behavior in the release build.

## 4. Permissions and user control

CESI's manifest declares permissions related to microphone, notifications, camera, location, contacts, phone state, calling, call log, audio settings, overlay, and startup/background operation. Not every feature needs every permission, and availability depends on Android restrictions.

Users should grant only permissions needed for features they intend to use. They can review/revoke permissions and notification access in Android Settings. Revoking a permission can disable the related feature.

Before public release, provide a clear in-app explanation before asking for sensitive access, especially microphone/background listening, notification access, contacts/caller ID, location, and overlays.

## 5. Retention and deletion

The current history store retains up to 50 entries until entries are displaced by newer history or the user uses Clear, or app data is otherwise removed. The final build must be tested to confirm the actual behavior.

Conversation context is stored locally for follow-up commands. The five-minute copilot expiry is checked on read rather than guaranteed by a background deletion job. This behavior must be improved or described precisely before publication.

Do not promise that uninstalling or clearing history deletes copies held by a speech-recognition provider or other third party. Such retention depends on the relevant provider's terms and must be documented once identified.

## 6. Sharing and third parties

No complete third-party data-processing inventory has been approved yet. Before publication, identify the speech recognition provider(s), text-to-speech provider(s), any network services/SDKs, and the information each may receive. Link to their privacy terms where applicable.

Do not publish a statement such as “CESI never sends data off-device,” “CESI does not collect data,” or “all data stays private” unless a release-specific technical audit proves it.

## 7. Security

CESI uses Android app-private storage for the preferences described above. The release owner must confirm backup exclusions, permission boundaries, exported components, log behavior, network security, secret handling, and data deletion on the actual release artifact. No mobile app can promise absolute security.

## 8. Children, legal rights, and complaints

[Product owner/legal reviewer: specify intended age range, applicable jurisdictions, user rights, complaint route, and any legally required disclosures. Do not publish until reviewed.]

## 9. Changes and contact

This notice must identify the responsible entity and a monitored privacy contact before publication. Material changes to data processing should be reflected in an updated notice.

---

## Publication blockers (must all be resolved)

- [ ] Insert responsible legal entity, jurisdiction, privacy contact, version and effective date.
- [ ] Audit all source and dependencies for network requests, SDKs, analytics, crash reporting, and data transmission.
- [ ] Identify Android speech-recognition and TTS providers used by supported device configurations and review their data terms.
- [ ] Verify exact history/context retention and deletion behavior.
- [ ] Merge and test backup/data-extraction protections.
- [ ] Test notification access and message read-aloud behavior, including user opt-in, disable path, and privacy disclosure.
- [ ] Complete permission/component audit and Google Play policy review if Play distribution is planned.
- [ ] Review final notice with an appropriate legal/privacy reviewer.
- [ ] Publish the approved notice on a stable public URL and link it from the app/distribution page.

Until these blockers are closed, this file must remain an internal draft and CESI must not be described as production-ready.