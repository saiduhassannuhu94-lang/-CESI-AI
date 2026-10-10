# Failure record: multi-action separator overlap

Date: 2026-10-10
Branch: feature/phase6-multi-action-planner-v2
CI run: https://github.com/saiduhassannuhu94-lang/-CESI-AI/actions/runs/38068452346

## Exact problem

Three MultiTaskPlanner tests failed: English sequencing with a comma before `then`, Hausa sequencing, and `open WhatsApp, then turn on flashlight`. The Kotlin code compiled, but the task-splitting assertions failed.

## Root cause

The initial implementation combined commas and word connectors in one regex and iterated with non-overlapping `findAll()`. When the first candidate was a comma followed by whitespace, the regex consumed whitespace immediately before `then`/`sai`; that made the subsequent word-separator match unavailable. A rejected comma candidate therefore hid the correct next boundary.

## Why it was missed

Review focused on command-start recognition and message-content protection but did not consider overlapping separator candidates. The first unit-test run exposed the flaw; all three failures share the same candidate-tokenization root cause rather than three independent intent-parser bugs.

## Fix

Search comma separators and word separators independently, combine their candidate matches, sort by source position, and only then choose the earliest boundary whose right side starts with a recognizable command and whose left side is not protected message content.

## Prevention

- When a parser has overlapping token patterns, do not let an invalid early candidate consume text needed by a later candidate.
- Keep separator detection independent from semantic boundary validation.
- Add regression tests for punctuation followed by explicit connectors, multiple connectors, and message text containing connectors.
- Re-run the full unit test suite, lint, and APK build before merge.

## Reusable lesson

Lexical matching and semantic acceptance are separate decisions. A rejected token candidate must not prevent an overlapping valid candidate from being considered.