# Repository security and merge requirements

This repository follows [BurpBridge core](https://github.com/BurpBridge/burpbridge-core)
and [OSSAfrica SkillGuard](https://github.com/OSSAfrica/skillguard), adapted for Android.

## Bootstrap

1. Review and merge the signed setup PR after Android and CodeQL checks pass.
2. The metadata-only `pull_request_target` workflow becomes available once it is
   on `main`. Open a signed, sign-off-compliant PR and verify `DCO and GPG` passes.
3. Add `DCO and GPG` (GitHub Actions app ID 15368) to the active required checks.
   Verify the live ruleset afterwards. Do not require an unavailable workflow
   before merging the setup itself; that would block its own bootstrap.
4. Verify the first main-branch Scorecard and dependency submission runs.

The intended policy requires `Android checks`, `DCO and GPG`, CodeQL results
(security high or higher; ordinary errors and warnings), signed commits, one
review, dismissal of stale reviews, last-push approval, resolved conversations,
and an up-to-date branch. Merge commits preserve signatures; squash and rebase
merging are disabled. No bypass actors are configured. Android checks and
CodeQL protection can be activated after successful setup-PR analysis; the DCO
requirement needs step 2. Inspect the live ruleset to establish actual enforcement.

The merge maintainer must add their own matching `Signed-off-by` trailer to the
GitHub merge message. The PR check validates incoming commits, not the merge
commit GitHub creates afterwards. Never sign off on another person's behalf.

## Trust and permissions

The commit-policy workflow checks out the trusted base commit only and posts its
status on the exact PR head. It never runs contributor code with a privileged
token. `.github/scripts/check_commits.py` and its tests are runtime enforcement
files, not disposable setup helpers. Keep them available on `main`.

CI and CodeQL build PR code with read-only contents permissions. CodeQL declares
security-events write for its supported result upload. Only trusted main-branch
Scorecard and dependency submission jobs publish with additional permissions.
External actions are pinned to full hashes; Dependabot proposes weekly updates.
Bot commits follow the same DCO/GPG policy and may need maintainer replacements.

Secret scanning, push protection, private vulnerability reporting, and Dependabot
security updates are enabled. Organization policy requires web commit sign-offs,
uses read-only default workflow tokens, and prevents Actions PR approvals.
Organization-wide rulesets require a paid plan, so protection is repository-level.

## Limits

Android CI builds a debug APK, runs JVM tests and lint, and retains reports. It
does not sign production releases or exercise a real VPN on a device. The native
Go AAR is checked in; CodeQL analyzes Kotlin/Java, Actions, and policy Python,
not the embedded Go binary. The issue backlog tracks these coverage gaps.
