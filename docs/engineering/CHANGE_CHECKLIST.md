# CESI Change Checklist

## Before Coding

- [ ] Understand the current architecture.
- [ ] Inspect the affected files and existing behavior.
- [ ] Search usages and dependencies of anything whose contract will change.
- [ ] Identify likely regressions.
- [ ] Define how the change will be verified.

## During Coding

- [ ] Keep the change focused.
- [ ] Preserve existing behavior unless intentionally changing it.
- [ ] Add or update regression tests where useful.
- [ ] Do not assume unsupported Android capabilities.
- [ ] Keep Android execution separate from pure planning/understanding logic where possible.

## After Coding

- [ ] Review the exact diff.
- [ ] Run relevant unit tests.
- [ ] Run lint.
- [ ] Build the APK when the change affects the app.
- [ ] Check GitHub Actions.
- [ ] Verify runtime/device behavior when the capability requires it.
- [ ] Record any significant failure in `LESSONS_LEARNED.md`.
- [ ] Add a prevention rule when the failure reveals a reusable safeguard.

## Before Merge

- [ ] CI is green.
- [ ] No known regression is unresolved.
- [ ] Verification evidence exists.
- [ ] Architecture documentation is updated when needed.
- [ ] The change is small enough to understand and safely review.

## Failure Rule

Never use “fixed” to mean only “the error disappeared.”

A fix is complete when:
**root cause is understood + behavior is corrected + regression is checked + recurrence is made less likely.**
