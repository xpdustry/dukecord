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

The branch is based on commit `76c63ea` (`z events`). The event annotation refactor, including the channel decode
replacement, is uncommitted. Run `git status --short` before editing. Preserve all current modifications and untracked
event files.

## What is good

[`DiscordGatewayEvents.java`](src/main/java/dev/phinner/dukecord/gateway/events/DiscordGatewayEvents.java) now only maps Discord event names to Jackson target types. The old NanoJSON helpers, ID loops, and event constructors are gone.

These event families now use Immutables interfaces and Jackson annotations:

- Guild create, update, delete, and unavailable
- Message create, delete, and bulk delete
- Channel create, update, and delete
- User update

`GUILD_CREATE` and `GUILD_DELETE` use separate package-private Jackson roots because their default variants differ. The `unavailable: true` subtype maps to `GuildUnavailableEvent`. Missing or `false` maps to the normal create/delete event.

Full guild, message, and user payloads use `@JsonUnwrapped`. Delete payloads use `@JsonProperty` for Discord's `id`, `ids`,
`channel_id`, and `guild_id` names. `Snowflake` handles scalar and list conversion.

Channel create, update, and delete share [`ChannelEventDeserializer.java`](src/main/java/dev/phinner/dukecord/gateway/events/ChannelEventDeserializer.java):
a contextual deserializer that reads the flat payload as the polymorphic `Channel` through `context.readValue`, then wraps
it in the target event. Polymorphism lives only on `Channel`; the three event interfaces carry no subtype tables.

## Channel decode: what failed and why the deserializer won

Two Jackson 3.1.4 mechanisms were rejected empirically before settling on the shared deserializer:

- `@JsonUnwrapped Channel channel()` decodes as `null`: unwrapping buffers tokens and routes them by property name, and the
  polymorphic wrapper contributes no property-name table.
- `@JsonDeserialize(converter = StdConverter<Channel, Event>)` on the event interface builds its delegate deserializer for
  `Channel` without the root-level `TypeWrappedDeserializer`, so the abstract type resolves to `AbstractDeserializer` and
  every decode fails with "no Creators". Converter delegates never receive polymorphic type resolution.

The shared deserializer works because `context.readValue(parser, Channel.class)` re-enters root resolution, which applies
`Channel`'s `@JsonTypeInfo`. `DeserializationContext.getContextualType()` supplies the concrete event target at root, so one
class serves all three operations. The Dukecord reader attribute needed by `DiscordEntityRefDeserializer` flows through the
same context; a guild channel payload with `guild_id` fails to decode without it, which the probe below relies on.

`ChannelPayloads.java` and its fifteen adapters are deleted.

## Verification already performed

An ephemeral probe (outside the repository; no probe files remain) covered:

- `GUILD_CREATE` with missing, `false`, and `true` `unavailable`
- `GUILD_DELETE` with missing and `true` `unavailable`
- `GUILD_UPDATE`, `USER_UPDATE`, unknown dispatch names
- Channel create, update, and delete for guild text (0 and 5), DM (1 and 3), category (4), thread (10, 11, and 12), and
  unknown (99) channel types, asserting the canonical `Channel` subtype and, for guild channels, the store-bound
  `guild()` reference
- Message create with author, channel, and guild references; message delete; bulk delete
- Concrete and family listener routing: a create listener received exactly one event, a `ChannelEvent` family listener
  received create, update, and delete once each, and a message listener received no channel events

The latest command passed:

```shell
./gradlew spotlessApply build --no-daemon
```

Run the same command after further changes. Also run `git diff --check`.

## Constraints and preferences

- Do not stash or discard the current worktree.
- Do not add persistent tests at this stage.
- Do not duplicate entity fields onto event interfaces.
- Keep the public event API based on canonical entity objects.
- Avoid new data-object layers.
- The user values a small amount of direct adapter code over large annotation-driven type explosions.

[`docs/gateway-events.md`](docs/gateway-events.md) documents the shared channel deserializer and the rejected alternatives.
