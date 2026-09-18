# Gateway event handoff

## Goal

Finish the gateway event refactor with this shape:

- Public events are interfaces.
- Immutables generates implementations and builders.
- Jackson 3 decodes event payloads.
- `DiscordGatewayEvents` keeps the event-name switch but contains no field-by-field parsing.
- Full payload events expose canonical entities through `guild()`, `channel()`, `message()`, or `user()`.
- Unknown channel kinds still become the single `UnknownChannel` model type.
- Keep the repository free of persistent tests for now.

## Current worktree

The branch is based on commit `76c63ea` (`z events`). The event annotation refactor is uncommitted. Run `git status --short` before editing. Preserve all current modifications and untracked event files.

The current implementation compiles and the full Gradle build passes, but the channel solution should be replaced before committing.

## What is good

[`DiscordGatewayEvents.java`](src/main/java/dev/phinner/dukecord/gateway/events/DiscordGatewayEvents.java) now only maps Discord event names to Jackson target types. The old NanoJSON helpers, ID loops, and event constructors are gone.

These event families now use Immutables interfaces and Jackson annotations:

- Guild create, update, delete, and unavailable
- Message create, delete, and bulk delete
- Channel create, update, and delete
- User update

`GUILD_CREATE` and `GUILD_DELETE` use separate package-private Jackson roots because their default variants differ. The `unavailable: true` subtype maps to `GuildUnavailableEvent`. Missing or `false` maps to the normal create/delete event.

Full guild, message, and user payloads use `@JsonUnwrapped`. Delete payloads use `@JsonProperty` for Discord's `id`, `ids`, `channel_id`, and `guild_id` names. `Snowflake` handles scalar and list conversion.

## The bad part

[`ChannelPayloads.java`](src/main/java/dev/phinner/dukecord/gateway/events/ChannelPayloads.java) is a technically correct but unacceptable workaround. Remove it.

Discord sends a flat channel object, while the public API wants `ChannelEvent.channel()`. That suggests `@JsonUnwrapped Channel channel()`, but Jackson 3.1.4 cannot unwrap a polymorphic value. `Channel` uses `@JsonTypeInfo` on its `type` property. A direct unwrapped `Channel` silently decoded as `null` in a local probe.

The current workaround moves channel polymorphism onto every operation. Five channel variants multiplied by create, update, and delete produces 15 private Immutables adapters. This is the cursed cross-product the user rejected.

## Recommended replacement

Use a small Jackson converter or deserializer per channel operation:

```java
@Value.Immutable
@JsonDeserialize(converter = ChannelCreateEventConverter.class)
public interface ChannelCreateEvent extends ChannelEvent {
    Channel channel();
}
```

Jackson should deserialize the flat payload as `Channel`, preserving the existing `type` polymorphism and Dukecord-bound reader context. The converter should only wrap that `Channel` in `ImmutableChannelCreateEvent`. Repeat for update and delete, sharing a generic base if it stays clearer than three direct implementations.

Prefer `StdConverter<Channel, ChannelCreateEvent>` over a parser-level deserializer. It lets Jackson own all channel field and subtype decoding. Verify that `@JsonDeserialize(converter = ...)` on the event interface supplies `Channel` as the converter input type and preserves the reader attribute used by `DiscordEntityRefDeserializer`.

Completion criteria:

1. Delete `ChannelPayloads.java`.
2. Restore the three public channel event interfaces to one `Channel channel()` property each.
3. Remove the repeated `@JsonTypeInfo` and `@JsonSubTypes` tables from the three event interfaces.
4. Keep `DiscordGatewayEvents` switch-only.
5. Decode every supported channel type and an unknown type into the correct canonical `Channel` subtype.
6. Confirm create listeners never receive update/delete events and family listeners receive each event once.
7. Run formatting and the full build.

If `StdConverter` loses the Dukecord reader context, use one shared contextual deserializer. It should call `context.readValue(parser, Channel.class)` and only construct the appropriate generated event wrapper. Keep channel field parsing inside Jackson.

## Verification already performed

The current, cursed implementation passed an ephemeral JShell probe for:

- `GUILD_CREATE` with missing, `false`, and `true` `unavailable`
- `GUILD_DELETE` with missing and `true` `unavailable`
- Channel create, update, and delete for guild text, DM, category, thread, and unknown channel types
- Message create, delete, and bulk delete
- User update
- Store-bound entity references
- Concrete and family listener routing

No probe files remain in the repository.

The latest command passed:

```shell
./gradlew spotlessApply build --no-daemon
```

Run the same command after replacing the channel adapters. Also run `git diff --check`.

## Constraints and preferences

- Do not stash or discard the current worktree.
- Do not add persistent tests at this stage.
- Do not duplicate entity fields onto event interfaces.
- Keep the public event API based on canonical entity objects.
- Avoid new data-object layers.
- The user values a small amount of direct adapter code over large annotation-driven type explosions.

[`docs/gateway-events.md`](docs/gateway-events.md) currently documents the 15-adapter workaround. Update that section when replacing it.
