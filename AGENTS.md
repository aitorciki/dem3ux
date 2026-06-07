# AGENTS.md

## Project

dem3ux is an Android m3u demuxer and emulator-launch bridge.

It bridges frontends that launch `.m3u` playlists with emulators that expect a concrete game image path. Keep the architecture frontend-agnostic; ES-DE is the first integration example, not the product boundary.

## Android Package

Use this application ID/package namespace:

```text
net.aitorciki.dem3ux
```

## Stack

- Kotlin
- Jetpack Compose
- Room
- Explicit Android intents for emulator launch handoff

## First Compatibility Target

Target DuckStation first.

Known ES-DE DuckStation activity:

```text
com.github.stenzek.duckstation/.EmulationActivity
```

## Engineering Priorities

- Prefer the smallest correct implementation.
- Keep launch bridge logic independent from any one frontend where practical.
- Treat Android path and URI handling as core domain logic.
- Do not assume all emulators accept the same path type or intent shape.
- Add emulator compatibility incrementally, one verified target at a time.
- Prefer explicit package/activity launches over broad implicit intents when handing off to emulators.
- Avoid compatibility abstractions until at least one emulator works end-to-end.

## Persistence

Use Room for persisted playlist state.

The model should support:

- Known `.m3u` playlists.
- Parsed playlist entries.
- One selected/default entry per playlist.
- Timestamps for future sorting and cleanup.

Do not use DataStore for playlist/entry state. DataStore is acceptable later for simple app preferences.

## Parsing

Initial `.m3u` behavior:

- Plain text input.
- Ignore blank lines.
- Ignore lines beginning with `#`.
- Resolve relative paths against the `.m3u` parent directory.
- Preserve entry order.
- Use the persisted selected entry if present, otherwise the first valid entry.

## Testing

Prioritize unit tests for:

- `.m3u` parsing.
- Relative path resolution.
- Selected-entry fallback rules.
- Launch intent construction.

Use instrumentation or manual device testing for real frontend-to-dem3ux-to-emulator launch flows.

## ES-DE Notes

ES-DE Android converts `es_systems.xml` command strings into Android intents.

Relevant variables include:

- `%EMULATOR_NAME%` for package/activity lookup from `es_find_rules.xml`.
- `%ACTION%=value`, `%CATEGORY%=value`, `%MIMETYPE%=value`, `%DATA%=value` for intent fields.
- `%EXTRA_key%=value` for string extras.
- `%ROM%` for absolute paths.
- `%ROMSAF%` for SAF URIs.
- `%ROMPROVIDER%` for ES-DE FileProvider URIs.
- `%ROMRAW%`, `%GAMEDIRRAW%`, `%ROMPATHRAW%`, `%BASENAME%` for raw path-derived values.

Keep README examples clearly labeled as examples, not as the only supported frontend contract.
