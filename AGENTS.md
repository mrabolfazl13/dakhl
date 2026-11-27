# AGENTS — DakhloKharj

## Absolute rules

1. Android builds run **only** in GitHub Actions (`.github/workflows/android-ci.yml`).
   Never run `gradle assemble`, `bundle`, `test` or any build task on the workstation.
   Local work is limited to source editing, `git`, `gh` and pushing.
2. No backend, no server, no accounts, no analytics, no ads. The app must not request INTERNET permission.
3. All money is `Long` minor units (Rial). Never use Float/Double for money.
4. Every financial mutation runs inside a Room `@Transaction` and updates account balances atomically.
5. Persian/RTL first. All user-facing strings are Persian.
6. Keep `CURRENT_STATE.md` current after every meaningful step.

## Layers

- `core/` — pure Kotlin utilities (money, Jalali calendar, formatting, finance engines).
- `data/local/` — Room entities, DAOs, database, converters, seed.
- `data/repository/` — atomic use-case oriented repositories.
- `domain/` — framework-free models and engines (unit-testable on the JVM).
- `ui/` — Compose design system, components, screens, navigation.
- `backup/`, `notifications/`, `security/` — platform services.

## Workflow

1. Edit source.
2. `git add` + commit + push.
3. Watch Actions: `gh run list`, `gh run watch <id>`.
4. Fix failures, repeat until green.
5. Download artifacts from the green run.
