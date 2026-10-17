# CESI media and WhatsApp command validation

## Observed failure and root cause

The combined request `open YouTube search comedy and play it` was processed as separate actions. The generic multi-task splitter separated the phrase at `and play it`, while the intent engine treated the first fragment as an app name rather than a media search and did not know how to execute the second fragment. The planner did not have an integration regression test for the exact compound command.

The WhatsApp parser/reaction improvements already merged to `main`, but the public release was still v0.4.0 from September. CI building `main` did not update that release, so installing the release APK could leave the user running old command-recognition code. The release workflow also pointed at a fixed older branch and version, making a current release awkward.

## Changes and safeguards

- Add a dedicated media-command parser and structured action so YouTube/TikTok search-and-play requests are interpreted before the generic splitter.
- Add regression tests for both exact platform examples, a common `serach` speech/text typo, a Hausa search form, and app-only commands.
- Keep playback status honest: opening search results is supported, but selecting and playing an arbitrary result automatically is not. A requested play is reported as partial, not success.
- Make release creation versioned and triggered by a `v*` tag or a deliberate `[publish-apk v...]` commit on `main`; test versions are published as prereleases.
- Keep WhatsApp reaction dependent on fresh notification context, Notification Access, explicit Android Accessibility consent, and physical-device verification.

## Regression commands

- `open YouTube search comedy and play it`
- `open TikTok search comedy and play it`
- `open TikTok serach comedy and play it`
- `search for Hausa comedy on YouTube`
- `bincika barkwanci Hausa a TikTok`
- `ka daura reaction na dariya`
- `ka saka wa wannan sakon reaction na dariya`
- `react with funny emoji`

## Verification boundaries

Unit tests, Android lint and APK compilation test code/build contracts, not actual interaction on an Android device. WhatsApp accessibility nodes differ across app versions, Android versions and OEMs. A UI click is not proof that a reaction was saved or synced. Before calling WhatsApp reaction or automatic playback production-ready, test on the target phone. No such physical-device result is claimed here.

## Reusable lesson

**A working unit parser is not enough when a request crosses planning, intent resolution and an Android executor.** Add the exact compound user utterance as an end-to-end regression test, and verify the APK release pipeline points at current code instead of assuming a green main build updates old downloads.


## CI failure record: normalized query casing

- **Observed failure:** the first CI run had one unit-test failure in `platformSearchWithoutPlaybackDoesNotInventPlayback`; all 160 tests ran and the other tests passed.
- **Root cause:** `IntentEngine.understand` normalizes its full input to lowercase before semantic parsing, but the newly written test expected the original uppercase `Hausa` in the extracted query. The dedicated `MediaCommandParser` preserves query case when called directly by `ContextTaskEngine`.
- **Why it was missed:** the test expectation was based on the user's original typed phrase, not the established `IntentEngine` normalization contract. The test cases did not distinguish the direct intent-engine path from the context engine's original-text fast path.
- **Fix:** correct the intent-engine test expectation to lowercase rather than changing established intent normalization and risking unrelated parser behavior.
- **Prevention:** maintain separate assertions for normalized intent-engine output and user-text-preserving context-engine parsing.
- **Lesson:** diagnose a failing test against the actual contract first; don't change correct runtime behavior simply to satisfy an inconsistent expected value.
