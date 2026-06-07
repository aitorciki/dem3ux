# dem3ux

dem3ux is an Android m3u demuxer for emulator launch workflows.

Its goal is to bridge launcher/frontends that request launching an `.m3u` playlist with emulators that do not directly support `.m3u` files and instead expect a concrete game image path.

The first target integration is ES-DE launching DuckStation, but the project should stay frontend-agnostic where possible.

## Problem

Some emulator frontends launch games by passing the selected ROM path to an Android emulator activity. For multi-disc systems, that selected ROM may be an `.m3u` playlist containing multiple disc/image entries.

Some emulators support `.m3u` directly. Others, such as DuckStation on Android, may need the frontend to pass a specific disc/image path instead.

dem3ux sits between the frontend and the emulator:

1. The frontend launches dem3ux with an `.m3u` path and target emulator information.
2. dem3ux parses the `.m3u` file.
3. dem3ux picks the last selected entry for that playlist, or the first entry by default.
4. dem3ux converts that selected entry into the path/URI form expected by the target emulator.
5. dem3ux launches the target emulator activity.

## Main Features

### Bridge Launch Mode

When launched by a frontend, dem3ux acts as a bridge:

- Accept an `.m3u` path from the frontend.
- Accept target emulator launch metadata.
- Parse playlist entries.
- Resolve relative entries against the `.m3u` location.
- Select the last-used entry for that playlist, or the first entry if no selection exists.
- Launch the target emulator with the selected entry instead of the original `.m3u`.

### App UI Mode

When launched as a regular Android app, dem3ux provides a small management UI:

- List `.m3u` playlists dem3ux has seen before.
- Show the entries for each playlist.
- Let the user change the selected/default entry.
- Persist the selected entry so future bridge launches use it by default.

## Android Package

The Android application ID/package namespace is:

```text
net.aitorciki.dem3ux
```

## Technology Direction

The intended Android stack is:

- Kotlin
- Jetpack Compose
- Room for persisted playlist state
- Explicit Android intents for emulator handoff

Room is preferred over DataStore because dem3ux has database-shaped state: playlists, entries, selected entries, timestamps, and likely future emulator-specific compatibility data. DataStore is better suited to simple key/value preferences.

## Development Checks

Run the full local verification task with:

```bash
./gradlew verify
```

This runs formatting checks, Android Lint, and unit tests.

To format Kotlin and Gradle Kotlin files:

```bash
./gradlew spotlessApply
```

The repository includes a pre-commit hook in `.githooks/pre-commit`. Enable it for a local clone with:

```bash
git config core.hooksPath .githooks
```

The hook runs `./gradlew verify` before each commit.

## Storage Model

The initial persisted model should be small:

- Playlist record: stable identity for a seen `.m3u` file.
- Entry records: resolved entries parsed from the playlist.
- Selected entry: the entry dem3ux should launch by default for that playlist.
- Timestamps: useful for sorting and future cleanup.

The exact schema can evolve, but the core invariant is that each known `.m3u` can remember one selected/default entry.

## Path And URI Handling

Frontends and emulators may use different path forms:

- Absolute filesystem paths.
- Android Storage Access Framework URIs.
- FileProvider content URIs.
- Emulator-specific extras or data fields.

Path handling is core product logic. dem3ux should not assume all emulators accept the same path form.

The first implementation should focus on one verified path from ES-DE to dem3ux to DuckStation, then add compatibility targets incrementally.

## m3u Parsing

Initial parsing rules:

- Treat the `.m3u` as a plain text playlist.
- Ignore blank lines.
- Ignore comment/metadata lines beginning with `#`.
- Resolve relative paths relative to the directory containing the `.m3u` file.
- Preserve entry order.
- Use the first valid entry when no selected entry has been persisted.

## Frontend Integration

dem3ux should be designed around a generic bridge contract rather than ES-DE-specific internals.

A frontend should be able to provide:

- The `.m3u` input path or URI.
- The target emulator package/activity or a known target profile.
- The target path/URI mode, when known.
- Any emulator-specific intent action, data, MIME type, categories, extras, or flags required for launch.

ES-DE is the first integration example because it already provides Android emulator activity rules and command variables.

## ES-DE Background

ES-DE Android uses two relevant configuration files:

- `es_find_rules.xml`: maps emulator names to Android package/activity entries.
- `es_systems.xml`: defines systems and command strings that ES-DE converts into Android intents.

Important ES-DE Android command variables:

- `%EMULATOR_NAME%`: resolves an emulator entry from `es_find_rules.xml`.
- `%ACTION%=value`: sets the Android intent action.
- `%CATEGORY%=value`: sets an Android intent category.
- `%MIMETYPE%=value`: sets the MIME type.
- `%DATA%=value`: sets the intent data value.
- `%EXTRA_key%=value`: adds a string extra named `key`.
- `%ROM%`: absolute filesystem path to the selected ROM.
- `%ROMSAF%`: Storage Access Framework URI for the selected ROM.
- `%ROMPROVIDER%`: ES-DE FileProvider URI for the selected ROM; useful for single-file launches, not multi-file sets.
- `%ROMRAW%`: raw absolute ROM path.
- `%GAMEDIRRAW%`: raw directory containing the selected game.
- `%ROMPATHRAW%`: raw configured ROM root.
- `%BASENAME%`: filename stem without extension.

ES-DE command values are space-delimited unless quoted with double quotes. XML escaping still applies.

## ES-DE Example: dem3ux Emulator Rule

This proposed `es_find_rules.xml` entry lets ES-DE resolve dem3ux as an Android launch target:

```xml
<emulator name="DEM3UX">
    <rule type="androidpackage">
        <entry>net.aitorciki.dem3ux/.BridgeActivity</entry>
    </rule>
</emulator>
```

## ES-DE Example: DuckStation Through dem3ux

This proposed `es_systems.xml` command is intentionally experimental. It defines the bridge contract dem3ux should initially implement and may need adjustment after on-device testing.

```xml
<command label="DuckStation via dem3ux">%EMULATOR_DEM3UX% %ACTION%=android.intent.action.VIEW %EXTRA_dem3ux.target.package%=com.github.stenzek.duckstation %EXTRA_dem3ux.target.activity%=com.github.stenzek.duckstation.EmulationActivity %EXTRA_dem3ux.target.name%=DUCKSTATION %EXTRA_dem3ux.input.path%=%ROM% %EXTRA_dem3ux.input.saf%=%ROMSAF% %EXTRA_dem3ux.output.mode%=saf</command>
```

The known ES-DE DuckStation activity rule is:

```xml
<emulator name="DUCKSTATION">
    <rule type="androidpackage">
        <entry>com.github.stenzek.duckstation/.EmulationActivity</entry>
    </rule>
</emulator>
```

The first compatibility milestone is for dem3ux to reproduce a working DuckStation launch using the selected `.m3u` entry instead of the original `.m3u` playlist.

## Proposed Bridge Extras

The initial dem3ux bridge activity should accept these extras:

- `dem3ux.target.package`: target emulator package name.
- `dem3ux.target.activity`: target emulator activity class name.
- `dem3ux.target.name`: human-readable or profile key for compatibility handling.
- `dem3ux.input.path`: frontend-provided `.m3u` absolute path, when available.
- `dem3ux.input.saf`: frontend-provided `.m3u` SAF URI, when available.
- `dem3ux.output.mode`: preferred output path mode, such as `absolute`, `saf`, or `provider`.

These names are not stable API yet. They document the first integration contract to build and test.

## First Milestone

The first useful version should:

- Build a minimal Android app using Kotlin and Jetpack Compose.
- Provide `BridgeActivity` for external launches.
- Provide a regular app UI for known playlists and selected entries.
- Parse `.m3u` files with relative path resolution.
- Persist selected entries with Room.
- Launch DuckStation from ES-DE through dem3ux.
- Include unit tests for playlist parsing and selection behavior.

## Non-Goals For The First Version

- Replacing emulator frontends.
- Editing playlist files.
- Scraping game metadata.
- Supporting every emulator launch format upfront.
- Hiding Android storage complexity behind untested assumptions.

## License

dem3ux is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
