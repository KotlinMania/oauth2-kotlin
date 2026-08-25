# port-lint Proposed Changes

**Generated:** 2026-08-25
**Source:** tmp/oauth2/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/oauth2

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/oauth2/Error.kt` | `// port-lint: source oauth2/src/error.rs` | `// port-lint: source error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'oauth2/src/error.rs' vs expected 'error.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/oauth2/ErrorTest.kt` | `// port-lint: tests oauth2/src/error.rs` | `// port-lint: tests error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:oauth2/src/error.rs' vs expected 'error.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/oauth2/Types.kt` | `// port-lint: source oauth2/src/types.rs` | `// port-lint: source types.rs` | `types.rs` | `port-lint provenance header matched only after fallback normalization: 'oauth2/src/types.rs' vs expected 'types.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/oauth2/TypesTest.kt` | `// port-lint: tests oauth2/src/types.rs` | `// port-lint: tests types.rs` | `types.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:oauth2/src/types.rs' vs expected 'types.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/oauth2/Basic.kt` | `// port-lint: source oauth2/src/basic.rs` | `// port-lint: source basic.rs` | `basic.rs` | `port-lint provenance header matched only after fallback normalization: 'oauth2/src/basic.rs' vs expected 'basic.rs'` |
