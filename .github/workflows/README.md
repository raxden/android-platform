# GitHub Configuration

This document describes everything that must be configured in GitHub for the CI/CD workflows to work correctly.

All items are set under **Settings → Secrets and variables → Actions** of this repository, unless stated otherwise.

---

## Overview

| Name | Type | Required by |
|---|---|---|
| `CENTRAL_PORTAL_USERNAME` | Secret | `ci_publish.yml` |
| `CENTRAL_PORTAL_PASSWORD` | Secret | `ci_publish.yml` |
| `SIGNING_KEY` | Secret | `ci_publish.yml` |
| `SIGNING_KEY_ID` | Secret | `ci_publish.yml` |
| `SIGNING_PASSWORD` | Secret | `ci_publish.yml` |
| `SSH_PRIVATE_KEY` | Secret | `ci_publish.yml`, `ci_bump.yml` |
| `CODECOV_TOKEN` | Secret | `ci_reports.yml` |
| `CI_GIT_USER_NAME` | Variable | `ci_publish.yml`, `ci_bump.yml` |
| `CI_GIT_USER_EMAIL` | Variable | `ci_publish.yml`, `ci_bump.yml` |

---

## Secrets

Secrets are **encrypted** and used for sensitive data. They are never visible in logs.

### `CENTRAL_PORTAL_USERNAME`

Username for authenticating with [Maven Central Portal](https://central.sonatype.com/).

Used by `ci_publish.yml` when publishing libraries and the BOM to Maven Central.

---

### `CENTRAL_PORTAL_PASSWORD`

Password associated with `CENTRAL_PORTAL_USERNAME` for Maven Central Portal authentication.

Used by `ci_publish.yml` when publishing libraries and the BOM to Maven Central.

---

### `SIGNING_KEY`

The GPG private key (ASCII-armored) used to sign the published artifacts. Maven Central requires all artifacts to be signed.

**Step 1 — List your existing GPG keys:**
```bash
gpg --list-secret-keys --keyid-format LONG
```
Example output:
```
sec   rsa4096/AABBCCDD11223344 2024-01-01 [SC]
uid   Your Name <your@email.com>
```
The value after `rsa4096/` (e.g. `AABBCCDD11223344`) is your `KEY_ID`.

If you don't have a GPG key yet, create one first:
```bash
gpg --gen-key
```

**Step 2 — Export the private key:**
```bash
gpg --armor --export-secret-keys AABBCCDD11223344
```
Copy the **entire output** (including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and footer) and save it as this secret.

Used by `ci_publish.yml` via the `publish` action.

---

### `SIGNING_KEY_ID`

The short ID of the GPG key (last 8 characters of the full key ID).

From the example above, if the full key ID is `AABBCCDD11223344`, the short ID is `11223344`.

To find it:
```bash
gpg --list-secret-keys --keyid-format SHORT
```

Used by `ci_publish.yml` via the `publish` action.

---

### `SIGNING_PASSWORD`

The passphrase that protects the GPG key defined in `SIGNING_KEY`.

Used by `ci_publish.yml` via the `publish` action.

---

### `SSH_PRIVATE_KEY`

An SSH private key with **write access** to this repository. Required so that the CI can commit and push version bumps back to the repo after a successful build.

**Step 1 — Generate a dedicated deploy key:**
```bash
ssh-keygen -t ed25519 -C "ci-deploy-key" -f deploy_key
```
This creates two files: `deploy_key` (private) and `deploy_key.pub` (public).

**Step 2 — Add the public key as a Deploy Key:**

Go to **Settings → Deploy keys → Add deploy key**, paste the contents of `deploy_key.pub`, and enable **Allow write access**.

**Step 3 — Add the private key as this secret:**

Paste the contents of `deploy_key` as the value of this secret.

Used by `ci_publish.yml` (jobs `bump`, `publish-libraries`, `bump-bom`, `publish-bom`) and `ci_bump.yml` (jobs `bump`, `bump-bom`).

---

### `CODECOV_TOKEN`

Token for uploading code coverage reports to [Codecov](https://codecov.io).

Obtain it from your Codecov dashboard after linking this repository.

Used by `ci_reports.yml` via the `reports` action.

---

## Variables

Variables are stored as **plain text** and used for non-sensitive configuration data.

### `CI_GIT_USER_NAME`

Display name used by the CI bot when creating Git commits (e.g. version bump commits).

Example value: `Raxden Robot`

Used by `ci_publish.yml` and `ci_bump.yml` via the `bump_module_version` action (`git-user-name` input).

---

### `CI_GIT_USER_EMAIL`

Email address used by the CI bot when creating Git commits.

Example value: `raxden.dev@gmail.com`

Used by `ci_publish.yml` and `ci_bump.yml` via the `bump_module_version` action (`git-user-email` input).
