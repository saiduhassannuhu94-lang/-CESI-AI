# Failure record: doubled escapes in WhatsApp raw regex patterns

Date: 2026-10-15
Pull request: #39, WhatsApp reaction execution

## Exact problem

A source review caught that newly written Kotlin raw-string regular expressions used doubled backslashes such as `"""\\s+..."""`. In Kotlin raw strings, backslashes are not escaped, so the regular expression would look for a literal backslash followed by `s` instead of matching whitespace. Reaction commands, sticker/GIF commands, image commands, and some reply forms would fail to parse.

## Root cause

The source was authored using the escaping rules for ordinary Kotlin strings and JavaScript template strings, then copied into Kotlin triple-quoted raw strings without reducing the backslashes to one.

## Why it was missed

The initial review focused on the new emoji-name mapping and did not re-read the generated source after the tool-based file update. The issue was found during a second inspection before the latest head was accepted; no claim is made that this defect passed tests.

## Fix

Normalize only the raw-string regex patterns to use a single backslash for regex tokens such as `\s`. Keep the ordinary Kotlin string `Regex("\\s+")` correctly double-escaped. Add parser regression tests for English emoji names, Hausa names, and preservation of custom emoji.

## Prevention

- Inspect the actual file fetched back from GitHub after writing generated source.
- Review regex escaping according to the target language's literal type, not the language used to generate the source.
- Run parser unit tests on the latest commit, not an earlier head.
- Include both positive and negative cases for command parsing.

## Reusable lesson

When code passes through more than one string-literal layer, validate the final source representation before trusting the intended regex.
