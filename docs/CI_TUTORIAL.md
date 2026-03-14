# CI Tutorial — GitHub Actions Setup

## How CI works in this project

Every push / pull request to `main` or `develop` triggers `.github/workflows/ci.yml`, which runs:

```
mvn clean verify
```

This single command covers:
| Phase     | What it does                              |
|-----------|-------------------------------------------|
| `clean`   | Wipe previous build output               |
| `compile` | Compile production + test sources         |
| `test`    | Run all unit tests (JUnit 5 + Mockito)   |
| `package` | Build the JAR                             |
| `verify`  | JaCoCo enforces ≥ 80 % instruction coverage |

If any test fails **or** coverage drops below 80 %, the build turns red and the PR cannot merge.

---

## Step 1 — Add GitHub Actions Secrets

Go to your repository on GitHub:

```
Settings → Secrets and variables → Actions → New repository secret
```

Add **each** of the following secrets:

| Secret name         | Where to get the value                                              |
|---------------------|---------------------------------------------------------------------|
| `MONGODB_URI`       | Your MongoDB connection string, e.g. `mongodb+srv://user:pass@cluster.mongodb.net/esg` | # pragma: allowlist secret
| `JWT_SECRET`        | Any long random string (≥ 32 chars). Generate with: `openssl rand -hex 64` |
| `MINIO_ENDPOINT`    | URL of your MinIO server, e.g. `https://minio.example.com`         |
| `MINIO_ACCESS_KEY`  | MinIO access key (username)                                         |
| `MINIO_SECRET_KEY`  | MinIO secret key (password)                                         |
| `ADMIN_EMPLOYEE_ID` | Employee ID for the default admin account                           |
| `ADMIN_PASSWORD`    | Password for the default admin account                              |
| `USER_EMPLOYEE_ID`  | Employee ID for the default user account                            |
| `USER_PASSWORD`     | Password for the default user account                               |

> **Tip:** For CI you can point `MONGODB_URI` at a free MongoDB Atlas cluster and
> `MINIO_ENDPOINT` at a MinIO instance on Railway / Render / fly.io, so the pipeline
> never touches your production data.

---

## Step 2 — Verify the workflow file is present

```
.github/
└── workflows/
    └── ci.yml   ← must be committed to the repo
```

Push it and open **Actions** tab in GitHub to confirm the first run appears.

---

## Step 3 — Local pre-commit hook (blocks bad commits)

The hook runs the same `mvn clean verify` locally before every commit that touches `backend/src/`.

### One-time setup

```bash
# Install the pre-commit tool (once per machine)
pip install pre-commit

# Install hooks into .git/hooks/ (once per clone)
pre-commit install
```

### Local .env

The hook calls `scripts/run-tests.sh`, which loads `.env` from the repo root.

```bash
cp .env.example .env
# Edit .env with your local values (never commit this file)
```

### Skipping the hook in an emergency

```bash
git commit --no-verify -m "wip: skip tests this once"
```

Use sparingly — the CI pipeline will still catch failures on push.

---

## Step 4 — Read the JaCoCo report

After a CI run, the HTML report is uploaded as a build artifact:

```
Actions → <run> → Artifacts → jacoco-report
```

Download and open `index.html` to see per-class and per-line coverage.

Locally the same report is generated at:

```
backend/target/site/jacoco/index.html
```

---

## Quick reference

| Command                  | What it does                           |
|--------------------------|----------------------------------------|
| `scripts/run-tests.sh`   | Full local test run (loads .env)       |
| `mvn clean verify`       | Same, if env vars are already exported |
| `mvn test`               | Tests only — skips JaCoCo coverage gate|
| `pre-commit run --all-files` | Run all hooks manually             |
