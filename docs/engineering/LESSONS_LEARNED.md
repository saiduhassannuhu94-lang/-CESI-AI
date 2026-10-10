# CESI Lessons Learned

This document records failures, root causes, fixes, prevention rules, and reusable engineering lessons from the CESI project.

## Lesson 001 — Required Model Field Compile Failure

### Problem
A required `risk` field was added to `PlannedAction`, but one constructor usage was not updated. CI failed during Kotlin compilation.

### Root Cause
The model contract changed without auditing every usage.

### Why We Missed It
The implementation moved forward before all constructor/reference sites were searched and compiled.

### Fix
Updated the remaining `PlannedAction` constructor to provide the required risk value.

### Prevention
Before changing a required model field:
- Search all usages and constructors.
- Update every affected call site.
- Add or update regression tests.
- Run the relevant tests and build.
- Require green CI before merge.

### Lesson
A required model change is never local. Its usages must be audited before the change is considered complete.

## Lesson 002 — Syntax Error in New Architecture Model

### Problem
The `AssistantGoal` enum was closed with `) ` instead of `}`, causing Kotlin compilation to fail in CI.

### Root Cause
A structural syntax typo was introduced while adding the new request-understanding layer.

### Why We Missed It
The file was committed before a local compile-level verification caught the syntax error.

### Fix
Closed the enum correctly with `}`.

### Prevention
- Keep changes small and review the exact edited file.
- Compile/test immediately after structural Kotlin changes.
- Treat CI as the final gate, not the first discovery mechanism.
- Do not proceed to dependent architecture work while the branch is red.

### Lesson
Even a tiny syntax error can block the whole pipeline. Verification must happen immediately after structural changes.

## Lesson 003 — Existing Sealed-Class Variant Used as an Object

### Problem
CI run #401 failed while compiling `ContextTaskEngine.kt`. The `MessageAction.React` branch was written as if `React` were an object, even though it is a `data class React(val emoji: String)`.

### Root Cause
The existing `MessageAction` declaration was not re-verified before modifying the existing `when` branch.

### Why We Missed It
The implementation relied on prior context instead of inspecting the current source declaration and its exact type shape.

### Fix
Changed the branch to `is MessageAction.React` and retained access to `action.emoji`.

### Prevention
Before modifying an existing sealed-class `when`:
- Inspect the current sealed-class declaration.
- Verify whether each relevant variant is an object or data class.
- Search existing usages when the type shape is unclear.
- Compile immediately after the change.
- Add a regression test where practical.

### Lesson
Do not trust remembered type shapes. The repository is the source of truth.

## Lesson 004 — Private Notification Content Persisted Outside Its Narrow Purpose

### Problem
The WhatsApp notification listener copied sender names and message bodies into general assistant history, kept reply context in persistent preferences, and spoke new message text without a separate opt-in. Dismissing a notification also needed careful separation from invalidating the short-lived context used to suggest a reply.

### Root Cause
Notification handling treated message text as ordinary debug/history content. Persistence, backup/restore, speech output, retention, and notification-action lifetime were not designed as one privacy boundary.

### Why We Missed It
The feature was judged primarily on its ability to read messages and suggest/reply. The review did not trace the message body end to end through the listener, context store, assistant response, HistoryStore, backup, text-to-speech, and notification removal lifecycle.

### Fix
- Store only a generic WhatsApp event marker in general history.
- Keep the latest message context in process memory with a monotonic five-minute TTL; clear it on expiry, blank replacement, listener disconnect, or service shutdown.
- Separate the speech preference from notification access and default speech to off.
- Delete the legacy SharedPreferences message-context file at application startup.
- Disable backup and explicitly exclude private history/context preferences for legacy backup and Android 12+ extraction/transfer rules.
- Remove the restricted call-log permission until a compliant, implemented role-based use is established.
- Restrict the incoming-call receiver and ensure temporary TTS resources are cleaned up on every completion/error/timeout path.
- Add regression tests for content-free history and ephemeral-context expiry/clearing.

### Prevention
- Trace sensitive content end-to-end before accepting notification, clipboard, email, or messaging integrations.
- For each sensitive field, document its purpose, destination, retention, deletion trigger, backup eligibility, and whether it may be spoken aloud.
- Make audible disclosure an explicit opt-in separate from OS-level notification access.
- Test normal dismissal separately from invalidating a stale reply action.
- Review Android backup rules and permission-to-code mappings in the same security review.
- Do not claim encryption, immediate physical erasure, or device-level behavior without evidence.

### Lesson
A feature can work functionally while leaking more data than it needs. Privacy requirements must shape the data path from ingestion through memory, history, speech, backup, and deletion, not be added as a final UI toggle.

## Permanent Principle

Every failure should improve the system, not merely return it to its previous state:

**Problem → Root Cause → Why Missed → Fix → Prevention → Lesson**

When a new failure reveals a reusable safeguard, add that safeguard to the engineering process and apply it to future CESI work.
