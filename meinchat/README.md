# vbwd-android-meinchat

A feature **plugin** for the [vbwd-android](https://github.com/vbwd-platform/vbwd-android-core)
plugin-host platform — the Kotlin · Jetpack Compose · Hilt port of the vbwd-ios
SDK. Plugin id: `meinchat` · version `1.1.0`.

Peer-to-peer messaging with image attachments and token transfers, plus bot rich-choice rendering.

## What it registers

Through the `PlatformSdk` facade (the single extension seam) this plugin contributes:
the prefix-matched `/meinchat` route, a profile nickname section, a menu item, the limits/retention/cache stores, an auth-aware FCM token sink, translations — and exposes the secure-messaging seam meinchat-plus fills.

It touches **no core internals** — it depends on the public `:core` module only
(Open/Closed). Depends on **`:core`** only.

## Consume it

As a standalone module the plugin is published to GitHub Packages and consumed by
Maven coordinate:

```kotlin
// settings.gradle.kts — add the GitHub Packages repo (PAT with read:packages)
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/vbwd-platform/vbwd-android-meinchat")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// build.gradle.kts — register it in the host's available-plugins list
dependencies {
    implementation("com.vbwd:vbwd-android-meinchat:1.1.0")
}
```

Then add it to the host's `provideAvailablePlugins` list and to
`app/src/main/assets/plugins.json` (the enable/disable manifest).

## Build & test

```bash
./gradlew check        # ktlint + detekt + unit tests
```

## Docs

- [`docs/architecture.md`](docs/architecture.md) — how this plugin is wired.
- Original sprint report: `docs/dev_log/20260619/reports/11-A09-meinchat-plugin.md` in the umbrella repo.

## License

BSL 1.1 (Business Source License). Part of the **vbwd-platform** SDK.
