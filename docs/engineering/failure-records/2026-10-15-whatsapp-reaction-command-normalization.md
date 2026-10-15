# Failure record: WhatsApp reaction command not recognized

Date: 2026-10-15
Related change: WhatsApp reaction execution follow-up

## Exact problem

The user reported that CESI did not recognize their WhatsApp reaction command. The reaction execution path existed, but the message parser only matched bare command phrases such as `react with 😂` or `yi reaction da dariya`.

## Root cause

`ContextTaskEngine.execute()` calls `MessageCopilotParser.parse()` before the general `IntentEngine` normalization. The message parser did not strip the CESI wake phrase or polite lead-ins, so voice transcripts such as `Hey CESI, please react with laughing emoji` did not match the anchored reaction expression. Some natural Hausa forms were also absent from the reaction grammar.

## Why it was missed

Earlier tests exercised bare parser phrases but did not cover realistic wake-word speech transcripts or natural Hausa phrasing. Passing unit tests for a parser's narrow examples did not establish that the full voice-to-command input would be recognized.

## Fix

- Normalize the CESI wake phrase and common polite prefixes at the message-parser boundary.
- Add Hausa reaction patterns for `ka/ki yi reaction da ...`, `yi wa saƙon nan reaction da ...`, and `saka reaction na/da ...`.
- Add regression tests for wake-word English commands, polite lead-ins, Hausa phrasing, and whitespace variation.

## Prevention

- Every voice-command feature must test the transcript as heard by the app, including wake phrase, punctuation, filler/polite prefixes, casing, and realistic Hausa/English code-switching.
- Test the full dispatch boundary, not only the inner parser's idealized phrase.
- When a user reports a command is not recognized, trace the raw transcript through wake-word normalization, command routing, parser matching, and execution before changing the executor.

## Reusable lesson

A working executor is useless if the command never reaches it. Verify the entire voice-to-action path, not just the feature-specific action handler.
