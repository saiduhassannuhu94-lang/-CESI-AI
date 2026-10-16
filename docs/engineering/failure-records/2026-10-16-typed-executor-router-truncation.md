# Failure record: typed-executor branch contained a truncated ActionRouter

Date: 2026-10-16

## Exact problem

The earlier `feature/typed-low-risk-executors` branch failed Kotlin compilation at the first line of `ActionRouter.kt`. Fetching the actual branch file confirmed that it began with `fun routeResult(...)` and omitted the package declaration, imports, class declaration, and executor fields. This was not a downstream type error in the three controller changes.

## Root cause

The branch contained a method fragment in place of the complete source file. The implementation attempt changed controller return types but left the router source malformed, so the compiler could not resolve Android and project symbols.

## Why it was missed

The branch was not re-read from GitHub after the router edit before it was pushed. The first full-file validation was CI, and work stopped at the red build rather than reconstructing the change from the known-good main source.

## Fix

- Created a fresh branch from the current `main`, not from the broken branch.
- Reapplied the typed Flashlight, Volume, and Battery executor implementations.
- Rebuilt the router change by transforming the complete current `ActionRouter.kt`, preserving its package, imports, class, and all unrelated action routes.
- Routed the migrated capabilities directly through `ExecutionResult`; left unmigrated capabilities behind the existing transitional classifier.
- Kept legacy string-returning `route()` compatibility by exposing the typed result's user-facing message.

## Prevention

- Fetch and inspect the complete changed file after edits to core routing or other high-fan-out files.
- Compare the changed file's beginning, class declaration, public method signatures, and end against the base before pushing.
- Run the exact branch's CI after the final commit; never infer that controller-level edits prove router integration.
- For typed migrations, change the executor and router contract together in one reviewed branch, then run tests, lint, and the Android build.

## Reusable lesson

A compiler failure in a central router is not an invitation to patch random unresolved symbols. First verify the file's integrity and compare it to the known-good base. Rebuild from the complete source, preserve unrelated routes, and only proceed when the exact final branch passes CI.
