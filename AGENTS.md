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

Flycast has also been manually validated through the generic bridge path when the target action is forwarded with `dem3ux.target.action`.

## Engineering Priorities

- Prefer the smallest correct implementation.
- Keep launch bridge logic independent from any one frontend where practical.
- Treat Android path and URI handling as core domain logic.
- Do not assume all emulators accept the same path type or intent shape.
- Add emulator compatibility incrementally, one verified target at a time.
- Prefer explicit package/activity launches over broad implicit intents when handing off to emulators.
- Avoid compatibility abstractions until at least one emulator works end-to-end.
- Keep the generic bridge path available even when preset bridge aliases are available.

## Preset Bridge Aliases

Preset aliases let a frontend launch dem3ux as if it were a known emulator, while dem3ux launches the real emulator after selecting the correct playlist entry.

Current preset direction:

- Use one real `PresetBridgeActivity` plus manifest `activity-alias` entries such as `.presets.DuckStationBridgeActivity`.
- Keep aliases explicit in the manifest; Android cannot expose runtime-generated activity names.
- Preserve the emulator's native intent shape where possible, examples:
  - For DuckStation, read input from the native `bootPath` extra and forward `bootPath` with the selected entry.
  - For Flycast, read input from intent data and launch the real activity with `android.intent.action.VIEW` plus selected entry as target data.
- ES-DE preset integration should prefer overriding `es_find_rules.xml` emulator package rules over copying full `es_systems.xml` entries when possible.

## Bridge Contract

`BridgeActivity` is launched by a frontend and then launches the target emulator.

Current contract:

- The bridge input ROM path or URI is `Intent.data`.
- Do not reintroduce `dem3ux.input.path` unless there is a concrete frontend that cannot provide data.
- `dem3ux.target.activity` is required and must be a flattened Android component string, such as `com.github.stenzek.duckstation/.EmulationActivity`.
- `dem3ux.target.action` is optional and sets the action on the target emulator intent.
- All extras whose keys do not start with `dem3ux.` are target-emulator extras and should be forwarded unchanged by key.
- If a forwarded string extra or string-array item equals the bridge input, replace it with the selected playlist entry before launching the target.
- If no forwarded emulator extra consumes the bridge input, set the target intent data to the selected entry.
- Forward only the documented activity flags used by frontends so far: clear task, clear top, and no history.
- For `content://` selected entries, include a read grant and `ClipData` on the target intent.

For direct non-playlist inputs, skip parsing and proxy the input path or URI unchanged. Direct non-playlist launches should not create playlist database records.

## Bridge Activity Manifest

`BridgeActivity` is a transient launch handoff activity, not a destination screen.

Keep these manifest attributes unless replacing them with a tested equivalent:

```xml
android:taskAffinity=""
android:excludeFromRecents="true"
```

The empty task affinity is important. It prevents emulator launches from being associated with dem3ux's normal app task when `MainActivity` is already running.

Do not set `android:noHistory="true"` on `BridgeActivity`. The bridge may launch `ACTION_OPEN_DOCUMENT_TREE` to request a persisted SAF folder grant; `noHistory` can remove the bridge while the picker is in front and prevent the activity-result callback from retrying the launch. `BridgeActivity` should finish itself explicitly after the handoff or failure path completes.

## Persistence

Use Room for persisted playlist state.

The model should support:

- Known `.m3u` playlists.
- Parsed playlist entries.
- One selected/default entry per playlist.
- Timestamps for future sorting and cleanup.

Do not use DataStore for playlist/entry state. DataStore is acceptable later for simple app preferences.

Clearing app data removes Room playlist state and persisted SAF grants.

## Parsing

Initial `.m3u` behavior:

- Plain text input.
- Ignore blank lines.
- Ignore lines beginning with `#`.
- Resolve relative paths against the `.m3u` parent directory.
- Preserve entry order.
- Use the persisted selected entry if present, otherwise the first valid entry.

For `.m3u8`, use the same initial behavior as `.m3u` unless a concrete encoding issue requires different handling.

## Android Path And URI Handling

Treat path and URI conversion as core domain logic.

- Preserve the input path family where practical: filesystem path in, filesystem path out; `file://` in, `file://` out; SAF/content URI in, SAF/content URI out.
- For Android external-storage SAF URIs, dem3ux may need to request and persist a ROM-folder tree grant before reading playlists or forwarding selected entries.
- Map external-storage document URIs through matching persisted tree grants before reading or forwarding them.
- Do not assume a temporary grant from a frontend is enough for dem3ux to read an `.m3u` or grant the selected entry onward.

## Testing

Prioritize unit tests for:

- `.m3u` parsing.
- Relative path resolution.
- Selected-entry fallback rules.
- Launch intent construction.
- Direct non-playlist proxy behavior.
- SAF external-storage URI mapping and persisted tree grant selection.
- Generic target extra forwarding and input replacement.

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

ES-DE has been validated launching DuckStation through dem3ux using SAF URIs for both `.m3u` playlists and direct image paths.

Native ES-DE `%ACTION%` applies to ES-DE launching dem3ux. If the target emulator needs an action, pass it as `%EXTRA_dem3ux.target.action%=...`.

## Daijishou Notes

Daijishou player definitions use an Android `am start`-style argument string.

Useful mappings:

- `-n` is the component to start.
- `-a` is the intent action.
- `-d` is the intent data.
- `-e` and `--es` are string extras.
- `--ez` is a boolean extra.
- `{file.uri}` appears to correspond to a SAF/content URI, similar to ES-DE `%ROMSAF%`.
- `{file.path}` appears to correspond to a filesystem path, similar to ES-DE `%ROM%`.

A Daijishou DuckStation-through-dem3ux player should be able to launch dem3ux with `-d {file.uri}`, pass `-e dem3ux.target.activity com.github.stenzek.duckstation/.EmulationActivity`, and keep DuckStation's native `-e bootPath {file.uri}` extra for dem3ux to replace when needed.

Validate Daijishou on device before documenting it as tested compatibility.
