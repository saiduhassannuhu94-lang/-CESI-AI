# Failure record: WhatsApp reaction commands not recognized

Date: 2026-10-15

## Exact problem

The user reported that CESI did not recognize their WhatsApp reaction command even though the reaction feature had been merged and the CI build had passed.

## Root cause

The parser accepted only a narrow set of command prefixes such as `react with` and `yi reaction da`. It did not cover natural variants such as `React to this message with laughing emoji`, `saka reaction da dariya`, or `ka saka masa reaction da soyayya`. The generic `react to` branch could also mistake a target phrase such as `this message` for the reaction name.

## Why it was missed

Tests covered the original canonical phrases and emoji-name mappings, but did not model the exact spoken-command variation and target wording that occurs in real use. A green build proved compilation and existing tests, not broad command understanding or real WhatsApp UI execution.

## Fix

- Add explicit English and Hausa command variants for reaction selection.
- Parse the reaction target from phrases such as `react to this message with laughing emoji`.
- Normalize common emoji names, leading articles, and trailing speech punctuation.
- Reject `react to this message` when no emoji or recognized reaction is specified instead of treating the target phrase as an emoji.
- Add regression tests for these commands and the ambiguous-target case.

## Prevention

- For each voice feature, tests must cover canonical wording, realistic speech variants, Hausa wording, punctuation, missing arguments, and ambiguous targets.
- Keep parser recognition tests separate from Android UI execution tests; success in one does not imply success in the other.
- Fetch the actual changed source back from GitHub and inspect it before treating the branch as ready.
- Do not call the feature successful on-device until a real Android/WhatsApp test confirms the workflow.

## Reusable lesson

A passing build only proves the cases that were tested. Voice-command features need realistic phrase coverage and an explicit boundary between understanding a command and executing it.


## Follow-up test failure during speech-candidate recovery

- **Observed failure:** the new test `doesNotGuessWhenRecognizedAlternativesDisagreeOnEmoji` failed on the first CI run.
- **Root cause:** the fixture's expected string had been changed to an invalid reaction label, but the first input candidate still contained `react with heart reaction`, which the improved normalizer correctly maps to ❤️. The selector therefore correctly kept the first recognized candidate; the test data, not the selector, was inconsistent.
- **Why missed:** the test edit changed only the expectation and did not update the corresponding input list. The full suite caught this mismatch.
- **Fix:** update the first candidate itself to `react with maybe reaction`, leaving the competing recognized alternatives as different emojis.
- **Prevention:** for selector tests, verify both the expected value and the exact candidate list after fixture edits; re-read the changed test from the branch before pushing.
- **Reusable lesson:** when a test fails, determine whether the implementation or the fixture violates the intended contract before changing production logic.
