# oauth2-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Foauth2--kotlin-blue.svg)](https://github.com/KotlinMania/oauth2-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/oauth2-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/oauth2-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/oauth2-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/oauth2-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`ramosbugs/oauth2-rs`](https://github.com/ramosbugs/oauth2-rs).

**Original Project:** This port is based on [`ramosbugs/oauth2-rs`](https://github.com/ramosbugs/oauth2-rs). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `ramosbugs/oauth2-rs`

> The text below is reproduced and lightly edited from [`https://github.com/ramosbugs/oauth2-rs`](https://github.com/ramosbugs/oauth2-rs). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## OAuth2

<a href="https://crates.io/crates/oauth2"><img src="https://img.shields.io/crates/v/oauth2.svg"></a>
[![Build Status](https://github.com/ramosbugs/oauth2-rs/actions/workflows/main.yml/badge.svg)](https://github.com/ramosbugs/oauth2-rs/actions/workflows/main.yml)

An extensible, strongly-typed implementation of OAuth2
([RFC 6749](https://tools.ietf.org/html/rfc6749)).

Documentation is available on [docs.rs](https://docs.rs/oauth2). Release notes are available on [GitHub](https://github.com/ramosbugs/oauth2-rs/releases).

For authentication (e.g., single sign-on or social login) purposes, consider using the
[`openidconnect`](https://github.com/ramosbugs/openidconnect-rs) crate, which is built on top of
this one.

## Minimum Supported Rust Version (MSRV)

The MSRV for *5.1* and newer releases of this crate is Rust **1.71**.

The MSRV for *5.0.y* releases of this crate is Rust **1.65**.

The MSRV for *4.x* releases of this crate is Rust 1.45.

Beginning with the 5.0.0 release, this crate will maintain a policy of supporting
Rust releases going back at least 6 months. Changes that break compatibility with Rust releases
older than 6 months will no longer be considered SemVer breaking changes and will not result in a
new major version number for this crate. MSRV changes will coincide with minor version updates
and will not happen in patch releases.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:oauth2-kotlin:0.1.0-SNAPSHOT")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`ramosbugs/oauth2-rs`](https://github.com/ramosbugs/oauth2-rs). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the oauth2-rs authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`ramosbugs/oauth2-rs`](https://github.com/ramosbugs/oauth2-rs) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
