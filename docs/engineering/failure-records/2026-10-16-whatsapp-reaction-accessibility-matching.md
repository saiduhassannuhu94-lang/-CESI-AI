# Failure record: WhatsApp reaction target matching

Date: 2026-10-16
Branch: `fix/whatsapp-reaction-accessibility-matching-v2`

## Exact problem

The reaction command could be recognized and the reaction request created, yet the UI executor could still do nothing. The implementation required a byte-for-byte exact visible message text node and counted matching text nodes instead of unique message bubbles. Reaction selection also recognized only literal emoji glyphs and required three glyph labels, even though Android accessibility labels can expose names such as “Laughing Face” or “React with Heart”.

## Root cause

Command parsing and UI execution were treated as one feature, but the executor depended on WhatsApp-specific accessibility tree details that vary by version and locale. Duplicate descendants within one bubble could be mistaken for multiple messages; whitespace differences could prevent matching; text-only labels could make the reaction picker appear absent.

## Why it was missed

Earlier CI exercised the parser and request lifecycle, not an actual WhatsApp accessibility tree. A green APK build was incorrectly easy to interpret as end-to-end success, despite the PR explicitly lacking physical-device validation.

## Fix

- Normalize visible message whitespace and Unicode before exact comparison.
- Search the bounded visible accessibility tree and deduplicate text nodes by the bounds of their actionable long-click target.
- Continue to refuse ambiguous repeated-message matches rather than guessing.
- Map common accessible reaction labels to canonical emoji and deduplicate matching options by their clickable target.
- Keep the reaction picker visibility gate and one-shot request TTL.
- Add pure unit tests for accessible label aliases, custom emoji, message whitespace normalization, and exact-match safety.

## Prevention

- Test command parsing, request lifecycle, accessibility-label normalization, and device-level UI behavior as separate layers.
- Treat CI green as compilation/unit/lint evidence only, not proof that WhatsApp accepted a reaction.
- Require a physical-device test for the final claim that the reaction was applied.
- On a UI mismatch, capture a privacy-safe accessibility-tree sample without storing or logging private message bodies.

## Reusable lesson

A recognized command is not a completed action. Every external UI feature needs a tested path from normalized intent to a uniquely identified target and an honest verification result.
