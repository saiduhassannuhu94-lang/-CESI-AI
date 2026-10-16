# Failure record: WhatsApp reaction command matching and permission continuation

Date: 2026-10-16
Branch: `fix/whatsapp-command-permission-resume-v1`

## Observed problem

Users reported that CESI did not recognize natural WhatsApp reaction commands. The existing parser contained several specific patterns, but it selected the first regex that matched even when that pattern captured an entire phrase such as an emoji name plus a message target. If normalization could not turn that capture into a known emoji, the parser returned `null` without trying later, more specific patterns.

The permission flow also asked the user to enable Accessibility, then immediately finished the disclosure activity. It did not resume the pending one-shot reaction after the user returned from Android Settings. When Notification Access was missing, reply/suggestion/reaction commands could stop at a generic “no message context” response instead of opening the relevant settings page.

The reply executor could also classify a prepared WhatsApp draft too positively if the notification action was unavailable.

## Root cause

- CI run 596 exposed a Kotlin compile failure because `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS` is not exposed as a constant in this compile SDK. The app already uses the stable setting key string `enabled_notification_listeners` elsewhere; the new code incorrectly assumed a public constant existed.
- Pattern selection used `firstNotNullOfOrNull` to choose a syntactic match, rather than the first match that produced a valid reaction action.
- Accessibility settings were opened without a return/resume callback, so the user's original request was not continued.
- Missing Notification Access was not handled at the message-command boundary.
- Reply execution relied on a legacy string classifier instead of a structured outcome distinguishing a notification reply from a draft.

## Why it was missed

- Existing tests covered direct reaction forms and several Hausa forms, but not collisions where a broad pattern matched before a later target-specific pattern.
- The test suite did not model the full user journey through Android Settings and back.
- Reply behavior was tested mainly as text classification rather than as distinct “notification action accepted” versus “draft prepared” outcomes.

## Fix applied in this branch

- Continue scanning reaction patterns until a candidate actually normalizes to a supported emoji/reaction.
- Add regression cases for English references to “his message” / “the latest WhatsApp message” and Hausa possessive/message-target wording.
- Keep the pending reaction in memory while the user reviews the disclosure and enables Accessibility, then resume opening the latest WhatsApp conversation when they return.
- Give the pending reaction a two-minute window to accommodate manual Settings navigation; the Accessibility service remains WhatsApp-only and one-shot.
- Open Android Notification Access settings when message context is missing because CESI has not been granted notification access; tell the user to repeat the command after enabling it.
- Route replies through structured outcomes so a RemoteInput action is not confused with opening a draft. A draft is explicitly reported as partial, not sent.
- Add this record so the root cause and prevention remain reusable.

## Prevention

- Add regression tests for exact voice transcripts and for broad-pattern/specific-pattern collisions.
- Review parser control flow, not just individual regexes: a failed normalization must not stop later valid patterns from running.
- Test the entire permission journey: command → disclosure → OS settings → return → action continuation, plus decline and timeout paths.
- Treat Android settings grants as special-access workflows, not as ordinary runtime permission dialogs.
- Keep “notification reply accepted”, “draft opened”, and “message delivered” as distinct outcomes.
- Do not claim the reaction was sent merely because an accessibility node was clicked. A physical-device test on supported WhatsApp versions is still required.

## Reusable lesson

For natural-language parsers, a regex match is not the same as a valid intent. Continue until semantic validation succeeds. For permission-gated actions, permission approval must be connected to a safe resume path, and execution reports must distinguish actual actions from prepared drafts.
