# CESI Failure Prevention Rules

These rules exist to prevent known failure patterns from recurring.

1. **Inspect before changing.**
   Understand the current architecture, affected files, dependencies, and existing behavior before editing.

2. **Search before changing contracts.**
   Before changing a required model field, constructor, interface, or public method, search all usages and update every affected call site.

3. **Fix root causes, not symptoms.**
   Do not stop at the first visible error. Identify why it happened and whether the same failure can occur elsewhere.

4. **Verify immediately.**
   After structural code changes, run the smallest relevant tests or compile check as early as possible.

5. **Green CI is a merge requirement.**
   Never merge a pull request with failing or unverified CI.

6. **No unsupported claims.**
   Do not call a feature working, tested, or verified without evidence. Browser tests do not prove Android hardware execution.

7. **Capabilities require real executors.**
   A declared capability is not an implemented capability until the real executor exists and has appropriate tests.

8. **Protect existing behavior.**
   Architecture improvements must preserve existing functionality unless an intentional behavior change is documented and tested.

9. **Use regression tests for important fixes.**
   A bug fix should receive a test when practical so the same failure is caught automatically later.

10. **Record reusable lessons.**
    If a failure reveals a reusable engineering lesson, document it and turn it into a prevention rule.

11. **Do not rush dependent work.**
    Do not start the next architectural layer while the current change is failing or its verification is incomplete.

12. **Prefer the smallest safe change.**
    Minimize unrelated edits and keep each change easy to inspect, test, and revert.

13. **Trace sensitive data end to end.**
    Before integrating notifications, messages, clipboard, or account content, trace ingestion, in-memory use, persistence, assistant responses/history, speech output, retention/expiry, deletion, backup, and data transfer. Default sensitive speech to off and require an explicit opt-in separate from OS permission grants.

14. **Audit permissions against platform and store rules.**
    Keep a permission-to-code mapping. Do not request restricted permissions such as call-log access until an implemented eligible use and the applicable default-handler/role requirements are verified.

## Required Failure Review

For every significant failure, record:

- Exact problem
- Root cause
- Why it was missed
- Fix
- Prevention/safeguard
- Reusable lesson
