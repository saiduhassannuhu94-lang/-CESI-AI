# Failure record: direct WhatsApp draft parser source review

Date: 2026-10-15
Pull request: #40, direct WhatsApp message draft

## Exact problem

The first generated version of the new Kotlin raw-string regular expressions contained doubled backslashes (for example, `\\\\s` in the raw source). Kotlin triple-quoted strings do not process escapes, so these expressions would look for literal backslashes instead of whitespace. A first source-review pass also showed that malformed direct-message commands needed to be blocked from falling through into the existing latest-notification reply parser.

## Root cause

The source was assembled through a JavaScript string layer and then written as Kotlin source. Escaping was applied for the generator string rather than the final Kotlin raw-string literal. The initial command parsing also assumed that every near-match would be rejected harmlessly, without checking how the generic `tura` reply parser would interpret it afterward.

## Why it was missed

The first review focused on command coverage and the new Android intent path, not the exact final regex characters or parser fall-through behavior. The prior WhatsApp reaction work had documented the same raw-string escaping hazard, but the prevention process had not yet made a final-source inspection mandatory for every generated regex change.

## Fix

- Re-read the actual file fetched back from GitHub and reduced raw-string regex tokens to a single backslash.
- Recognize malformed direct-message command prefixes and return an explicit invalid-action result instead of allowing them to be interpreted as a reply to the latest notification.
- Added tests for English and Hausa commands, international number formats, empty message bodies, local-number rejection, and malformed-number fall-through.

## Prevention

- For Kotlin raw strings, inspect the final fetched source and confirm regex tokens such as `\s` contain exactly one source backslash.
- Test both a valid command and near-miss/invalid variants before adding a new parser branch.
- Review the full parser order, not only the new helper, for unintended fall-through into existing actions.
- Keep direct-message sending user-mediated: opening a prefilled chat is not proof of sending, and CESI must never report a send success unless a real send action is verified.

## Reusable lesson

Parser correctness includes rejection behavior. A command that almost matches a new feature must not accidentally become a different action, especially one that sends a reply to the wrong conversation.
