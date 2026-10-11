# Improvement plans

Improvement plans for the Compose Catalog framework, produced from a full architecture
review on 2026-10-08. Backward compatibility is **not** a constraint: APIs may be changed or
removed freely; each plan says which public surface it touches.

Priority: **P0** = user-visible performance (the Samsung A22 sample-app lag), **P1** =
architecture / API quality, **P2** = hygiene.

| # | Plan | Priority | Area |
|---|------|----------|------|
| [001](001-scroll-jank-root-page.md) | Root-page scroll jank (A22) | P0 | `PreferencePageScreen` rendering |
| [002](002-scroll-jank-detail-page-recomposition.md) | Detail-page per-frame recomposition | P0 | `PreferencePageScreen` state design |
| [003](003-persistence-full-rewrite.md) | Persistence: full rewrite of every key on every change | P1 | `PreferenceFlow.android` |
| [004](004-api-surface-unification.md) | API surface unification (4 styles per type, key overloading) | P1 | public API |
| [005](005-search-index-architecture.md) | Search index: thread-global recorder, rebuild-on-recomposition | P1 | `SearchIndex`, `PreferencePageScreen` |
| [006](006-preferencepagescreen-refactor.md) | Decompose the 1200-line `PreferencePageScreen`; library logging | P1 | `PreferencePageScreen` |
| [007](007-maintainability-cleanup.md) | Dead code, missing tests, CI hardening | P2 | repo |
| [008](008-performance-benchmark-plan.md) | Performance verification: macrobenchmark + Perfetto on A22 | P0 (enabler) | tooling |
| [009](009-vocabulary-naming.md) | Vocabulary & naming: define Screen/Page/Row/Store, rename public API | P1 | public API |
| [010](010-card-surface-tone.md) | Filled cards read brighter than the flat page rows (demo styling) | P3 | demo |
| [011](011-single-root-page-architecture.md) | One root Page per catalog; pages are collections of items (`subPage` item, no root hacks) | P1 | public API |
| [012](012-row-action-api-reconciliation.md) | Row action rows: caller misuse (placeholder page + onClick), not an API defect | P2 | callers (`demo`, azahar `SettingsPages`), optional guard |

Suggested order: **008 first** (baseline on the A22), then **001 + 002** (the reported lag),
then **003 → 005 → 004 → 009 → 006**, with **007** interleaved.
**009** (rename) sits after **004** (API unification) so the rename doesn't re-do
that work; it also unblocks the naming questions **003** and **004** reference
(`Preferences` vs store).
