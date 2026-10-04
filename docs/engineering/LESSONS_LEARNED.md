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

## Permanent Principle

Every failure should improve the system, not merely return it to its previous state:

**Problem → Root Cause → Why Missed → Fix → Prevention → Lesson**

When a new failure reveals a reusable safeguard, add that safeguard to the engineering process and apply it to future CESI work.
