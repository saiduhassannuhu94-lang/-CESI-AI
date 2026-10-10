# Failure record: expression-body return in notification reply suggestions

Date: 2026-10-10
Pull request: #34, notification privacy and least privilege

## Exact problem

CI failed at Kotlin compilation in ContextTaskEngine.kt with: “Returns are prohibited for functions with an expression body. Use block body '{...}'.” The failure was introduced when the privacy change rewrote the MessageAction.Suggest branch to leave an Elvis-return expression as a standalone expression inside executeMessageCopilotAction's expression-bodied when.

## Root cause

The branch needed a non-null message context to produce safe generic suggestions. The implementation wrote the null-check as a standalone expression instead of binding the context to a local value. Kotlin's compiler rejects that return form in this expression-bodied function.

## Why it was missed

The source edit was reviewed for data-flow/privacy behavior but was not locally compiled before pushing. The first authoritative syntax/type check therefore occurred in GitHub Actions.

## Fix

Bind the result of MessageCopilotEngine.latest() to a local context variable using the established Elvis-return pattern, then generate suggestions directly from that context's message. This keeps the incoming body out of persistent assistant history while satisfying Kotlin's expression-body constraints.

## Prevention

- After edits to Kotlin expression-bodied functions or sealed-class branches, run compile/tests immediately.
- Prefer small contract-preserving edits; when a return is needed, inspect the containing function body form.
- Treat compiler feedback as a required gate, not as something to paper over by changing the behavior.
- Re-run the whole PR workflow after the fix; do not infer success from the source diff alone.

## Reusable lesson

Source review and privacy reasoning do not replace compilation. The fix is complete only after the affected code compiles and the full relevant CI checks pass. No merge should happen while the run is red.