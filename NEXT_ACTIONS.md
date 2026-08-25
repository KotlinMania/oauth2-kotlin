# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/17 (17.6%)
- **Function parity:** 1/299 matched (target 39) — 0.3%
- **Class/type parity:** 8/77 matched (target 30) — 10.4%
- **Combined symbol parity:** 9/376 matched (target 69) — 2.4%
- **Average inline-code cosine:** 0.00 (function body across 3 matched files)
- **Average documentation cosine:** 0.71 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 3 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. error

- **Target:** `oauth2.Error [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 8
- **Priority Score:** 8051010.0
- **Functions:** 1/6 matched (target 8)
- **Missing functions:** `new`, `error`, `error_description`, `error_uri`, `fmt`
- **Types:** 4/4 matched (target 9)
- **Missing types:** _none_
- **Tests:** 1/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `oauth2/src/error.rs` vs expected `error.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:oauth2/src/error.rs` vs expected `error.rs`
- **Proposed provenance header:** `// port-lint: source error.rs` (current: `// port-lint: source oauth2/src/error.rs`)
- **Proposed provenance header:** `// port-lint: tests error.rs` (current: `// port-lint: tests oauth2/src/error.rs`)
- **Lint issues:** 2

### 2. types

- **Target:** `oauth2.Types [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 171710.0
- **Functions:** 0/16 matched (target 10)
- **Missing functions:** `as_ref`, `new_random_sha256`, `new_random_sha256_len`, `new_random_len`, `from_code_verifier_sha256`, `new_random_plain`, `from_code_verifier_plain`, `as_str`, `method`, `test_secret_conversion`, `test_secret_redaction`, `test_code_verifier_too_short`, `test_code_verifier_too_long`, `test_code_verifier_min`, `test_code_verifier_max`, `test_code_verifier_challenge`
- **Types:** 0/1 matched (target 7)
- **Missing types:** `PkceCodeChallenge`
- **Tests:** 0/7 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `oauth2/src/types.rs` vs expected `types.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:oauth2/src/types.rs` vs expected `types.rs`
- **Proposed provenance header:** `// port-lint: source types.rs` (current: `// port-lint: source oauth2/src/types.rs`)
- **Proposed provenance header:** `// port-lint: tests types.rs` (current: `// port-lint: tests oauth2/src/types.rs`)
- **Lint issues:** 2

### 3. basic

- **Target:** `oauth2.Basic [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 91310.0
- **Functions:** 0/5 matched (target 21)
- **Missing functions:** `from_str`, `as_ref`, `deserialize`, `serialize`, `fmt`
- **Types:** 4/8 matched (target 14)
- **Missing types:** `BasicClient`, `BasicTokenResponse`, `BasicTokenIntrospectionResponse`, `BasicRevocationErrorResponse`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `oauth2/src/basic.rs` vs expected `basic.rs`
- **Proposed provenance header:** `// port-lint: source basic.rs` (current: `// port-lint: source oauth2/src/basic.rs`)
- **Lint issues:** 1

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

