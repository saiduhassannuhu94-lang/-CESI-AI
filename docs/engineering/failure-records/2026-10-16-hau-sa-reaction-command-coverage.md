# Failure record: Hausa WhatsApp reaction command was not recognized

Date: 2026-10-16  
Pull request: #53

## Exact problem

A natural Hausa voice command using `daura reaction na dariya` could be rejected even though related forms using `dora`, `saka`, or `yi reaction` were covered. The word `funny` was also not consistently mapped to the laughing reaction.

## Root cause

The deterministic parser had several overlapping regex patterns, but the verb alternatives were incomplete for a common Hausa spelling/wording variant. Reaction normalization mapped `dariya` and `laughing`, but omitted `funny` as a synonym.

## Why it was missed

Earlier tests covered several Hausa word orders but did not include the exact `daura` spelling or the `funny emoji` wording. Tests were biased toward the variants already present in the parser.

## Fix

- Add `daura` alongside `dora` in the relevant reaction-command patterns.
- Map `funny` to 😂.
- Add regression tests for `ka daura reaction na dariya`, `ka daura wa wannan sakon reaction na dariya`, and `react with funny emoji`.

## Prevention

- Maintain a phrase matrix for Hausa/English mixed-language voice commands, including spelling and speech-recognition variants.
- Every user-reported “command not recognized” issue should add the exact utterance as a parser regression test.
- Keep parser success separate from executor success: passing a parser test does not prove the Accessibility service successfully reacted in WhatsApp.

## Reusable lesson

Natural-language support fails at the edges: one missing synonym or spelling variant can make a feature appear completely broken. Expand tests from actual utterances, not only from the developer's preferred wording.
