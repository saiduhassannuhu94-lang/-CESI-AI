# Failure record: WhatsApp command parsing and confirmation flow

Date: 2026-10-15

## Observed problems
- CI failed compiling the WhatsApp command-recognition change at `MessageCopilotEngine.kt` because a normal Kotlin string used the unsupported escape `\s`.
- Multi-action requests were still split only on a small set of sequencing words; comma/and boundaries from the planner work had not been integrated into the active `ContextTaskEngine`.
- A pending safety confirmation treated every non yes/no/cancel utterance as an answer to the old prompt. This could make unrelated new commands appear to require the same confirmation repeatedly.
- Dialer, USSD, WhatsApp draft, Messenger draft, and alarm-UI actions were classified too broadly as external side effects even though the user still performs the final call/send/save action in those flows.

## Root causes
- The final Kotlin source was not compiled after generating a regex string with escaping rules from another language.
- The multi-action planner remained in a stale pull request rather than being wired into the active execution engine.
- Confirmation resolution had no explicit path for a new recognized command while a previous confirmation was pending.
- Risk classification described the destination or intent label, not the actual side effect performed by the current executor.

## Why it was missed
- Review focused on adding command variants and tests, not on compiling the exact final branch head.
- Planner unit tests existed separately, but the runtime engine still used its old splitter.
- Confirmation tests covered yes/no/expiry, but not the user issuing a different valid command while confirmation was pending.
- Safety review did not consistently trace intent through to the actual Android executor behavior.

## Fix
- Correct the Kotlin regex escape and keep parser regression tests.
- Integrate the conservative multi-action planner into `ContextTaskEngine`.
- If a recognized new command arrives while a confirmation is pending, cancel the stale pending request and process the new command; ambiguous text still leaves the prompt pending.
- Require explicit confirmation only for actions that can directly cause an external side effect in the current implementation, notably calls and direct notification replies. Keep user-controlled draft/dialer/alarm UI actions at low risk.
- Add tests for planner boundaries, confirmation replacement, clock rollback, and low-risk action profiles.

## Prevention
- Compile/test the exact head SHA after the final source edit, not a preceding commit.
- Add an integration assertion whenever a planner is created so the runtime engine cannot silently keep its old parser.
- Test a sequence: high-risk action prompts, then a different valid low-risk command; verify the old pending action is cancelled and the new command is handled.
- Maintain a capability-to-executor-to-risk mapping. Classify based on the actual side effect, not the natural-language command label.
- Review escaped literals in the target language after every generated code edit.

## Reusable lesson
A feature is not integrated because its helper and unit tests exist. It is integrated only when the active runtime calls it, the final source compiles, and the state transitions are covered by tests.
