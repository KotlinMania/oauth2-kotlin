# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/17 (17.6%)
- **Function parity:** 21/299 matched (target 159) — 7.0%
- **Class/type parity:** 9/77 matched (target 47) — 11.7%
- **Combined symbol parity:** 30/376 matched (target 206) — 8.0%
- **Average inline-code cosine:** 0.42 (function body across 3 matched files)
- **Average documentation cosine:** 0.83 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 2 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. error

- **Target:** `oauth2.Error`
- **Similarity:** 0.47
- **Dependents:** 8
- **Priority Score:** 8011005.5
- **Functions:** 5/6 matched (target 12)
- **Missing functions:** `fmt`
- **Types:** 4/4 matched (target 9)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 2. basic

- **Target:** `oauth2.Basic`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 91310.0
- **Functions:** 0/5 matched (target 21)
- **Missing functions:** `from_str`, `as_ref`, `deserialize`, `serialize`, `fmt`
- **Types:** 4/8 matched (target 14)
- **Missing types:** `BasicClient`, `BasicTokenResponse`, `BasicTokenIntrospectionResponse`, `BasicRevocationErrorResponse`

### 3. types

- **Target:** `oauth2.Types`
- **Similarity:** 0.79
- **Dependents:** 0
- **Priority Score:** 1702.1
- **Functions:** 16/16 matched (target 126)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 24)
- **Missing types:** _none_
- **Tests:** 7/7 matched

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `token.mod` | `token.Mod` | 0 | `token/mod.rs` | `token/Mod.kt` |

