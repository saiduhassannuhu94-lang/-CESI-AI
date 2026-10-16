# Failure record: malformed quoted reply fell through to generic reply parsing

Date: 2026-10-16  
Pull request: #52, quoted WhatsApp reply to older messages

## Exact problem

CI test `MessageCopilotParserTest.doesNotParseQuotedReplyWithoutBothQuotedTexts` failed. An incomplete command such as `reply to the old message "Hello" with` was not parsed as a `QuotedReply`, but a later broad reply pattern still accepted it as ordinary `TextReply`.

## Root cause

The parser correctly required both quoted strings for the new specific action, but it then continued into a legacy generic reply parser. The specific parser's rejection was not treated as a terminal rejection for input that clearly began a quoted-reply command.

## Why it was missed

The first tests proved that valid English/Hausa quoted replies were parsed and that the quoted-reply regex itself rejected an incomplete command. They did not assert the final result after all fallback parsers ran.

## Fix

After valid quoted-reply parsing, detect the quoted-reply command shape. If it has the quoted-reply prefix but is malformed, return no action instead of letting a generic reply parser reinterpret the entire command as reply text.

## Prevention

- Test the public parser's final output, not only an individual regex/helper.
- Add negative tests for malformed high-specificity commands that could fall through to broader legacy patterns.
- Keep command recognition conservative when the parser cannot safely distinguish target text from outgoing reply text.

## Reusable lesson

A specific parser returning no match does not mean the overall parser rejects the input. Every fallback path must be checked for accidental reinterpretation, especially before external side effects.
