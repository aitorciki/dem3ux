# dem3ux

dem3ux is an Android m3u demuxer and emulator launch bridge.

Its goal is to bridge launcher/frontends that request launching an `.m3u` playlist with emulators that do not directly support `.m3u` files and instead expect a concrete game image path.

The first target integration is ES-DE launching DuckStation, but the project should stay frontend-agnostic where possible.

## Problem

Some emulator frontends launch games by passing the selected ROM path to an Android emulator activity. For multi-disc systems, that selected ROM may be an `.m3u` playlist containing multiple disc/image entries.

Some emulators support `.m3u` directly. Others, such as DuckStation on Android, may need the frontend to pass a specific disc/image path instead.

dem3ux sits between the frontend and the emulator. For playlist inputs:

1. The frontend launches dem3ux with an `.m3u` path and target emulator information.
2. dem3ux parses the `.m3u` file.
3. dem3ux picks the last selected entry for that playlist, or the first entry by default.
4. dem3ux converts that selected entry into the path/URI form expected by the target emulator.
5. dem3ux launches the target emulator activity.

For direct game/image inputs that are not `.m3u` or `.m3u8` playlists, dem3ux skips parsing and proxies the original path/URI to the target emulator unchanged. This supports frontends such as ES-DE that route an entire system directory through one Android launch command, regardless of individual file extension.

## Main Features

### Bridge Launch Mode

When launched by a frontend, dem3ux acts as a bridge:

- Accept a ROM path or URI from the frontend.
- Accept target emulator launch metadata.
- If the input is an `.m3u` or `.m3u8`, parse playlist entries.
- Resolve relative playlist entries against the `.m3u` location.
- Select the last-used entry for that playlist, or the first entry if no selection exists.
- If the input is not a playlist, proxy the original path/URI unchanged.
- Request and persist Android SAF folder access when required.
- Launch the target emulator with the selected entry or proxied direct input.

### App UI Mode

When launched as a regular Android app, dem3ux provides a small management UI:

- List `.m3u` playlists dem3ux has seen before.
- Show the entries for each playlist.
- Let the user change the selected/default entry.
- Persist the selected entry so future bridge launches use it by default.
- Leave direct non-playlist launches out of the playlist database.

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

### SAF Folder Access

When ES-DE passes `%ROMSAF%`, dem3ux may need its own persisted SAF folder grant before it can read `.m3u` playlists or forward direct image URIs to the target emulator.

On first launch, dem3ux asks the user to select the ROMs folder, or a parent folder containing the system folder. The grant is persisted and reused for future launches. Clearing dem3ux app data removes this grant and will make dem3ux ask again.

To reset dem3ux app data, including persisted folder grants and known playlist state:

```bash
adb shell pm clear net.aitorciki.dem3ux
```

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

- The input ROM path or URI.
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

This `es_systems.xml` command has been tested with ES-DE launching DuckStation through dem3ux for both `.m3u` playlists and direct image paths. The bridge contract names are still not stable API yet.

```xml
<command label="DuckStation via dem3ux">%EMULATOR_DEM3UX% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF% %EXTRA_dem3ux.target.activity%=com.github.stenzek.duckstation/.EmulationActivity %EXTRA_dem3ux.input.path%=%ROMSAF% %EXTRABOOL_dem3ux.target.extra.resumeState%=false %EXTRA_dem3ux.target.extra.bootPath%=%ROMSAF% %EXTRABOOL_dem3ux.target.flag.clearTask%=true %EXTRABOOL_dem3ux.target.flag.clearTop%=true</command>
```

The known ES-DE DuckStation activity rule is:

```xml
<emulator name="DUCKSTATION">
    <rule type="androidpackage">
        <entry>com.github.stenzek.duckstation/.EmulationActivity</entry>
    </rule>
</emulator>
```

The first compatibility milestone has been validated with ES-DE launching DuckStation through dem3ux using SAF URIs. dem3ux launches the selected `.m3u` entry instead of the original playlist, and proxies non-playlist ROMs through the same command.

## Proposed Bridge Extras

The initial dem3ux bridge activity should accept these extras:

- `dem3ux.target.activity`: target emulator activity as a flattened Android component string, such as `com.github.stenzek.duckstation/.EmulationActivity`.
- `dem3ux.input.path`: frontend-provided ROM path or URI. Playlist inputs are demuxed; non-playlist inputs are proxied unchanged. If this extra is missing, dem3ux falls back to the intent data URI.
- `dem3ux.target.action`: optional target emulator intent action.
- `dem3ux.target.extra.*`: target emulator extras to forward after stripping the prefix.
- `dem3ux.target.flag.clearTask`: forwards `Intent.FLAG_ACTIVITY_CLEAR_TASK` when true.
- `dem3ux.target.flag.clearTop`: forwards `Intent.FLAG_ACTIVITY_CLEAR_TOP` when true.
- `dem3ux.target.flag.noHistory`: forwards `Intent.FLAG_ACTIVITY_NO_HISTORY` when true.

If a forwarded target extra value equals `dem3ux.input.path`, dem3ux replaces that value with the selected playlist entry before launching the emulator. For non-playlist inputs, the selected entry is the original input path, so the value is forwarded unchanged. This lets frontend commands mirror emulator-specific launch shapes such as DuckStation's `bootPath` extra while dem3ux swaps `.m3u` playlists for the selected disc image when needed.

When the input is a SAF URI, also pass it as `%DATA%` where the frontend supports it. Android URI read permissions are commonly tied to intent data. In the tested ES-DE flow, dem3ux still needs its own persisted SAF folder grant before it can read playlists or forward selected direct image URIs to DuckStation.

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
