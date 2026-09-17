# Entity references in Discord libraries

Research date: 2026-09-17. The library set came from
[`apacheli/discord-api-libs`](https://github.com/apacheli/discord-api-libs). Claims below link to the owning projects'
source or documentation.

## Finding

There is no common equivalent of a serializable, generic `EntityRef<T>` that also owns a store and resolves itself.
Libraries converge on one of two designs:

1. **Pure models with typed IDs.** Payload models retain IDs. Cache and HTTP clients resolve those IDs explicitly.
2. **Client-bound domain entities.** Every domain entity retains a client, state object, or entity supplier. Navigation methods
   use that shared context.

The Dukecord design under evaluation is between the two: its surrounding entities look like pure immutable payload values, but each ref
is a live handle retaining a store. That mixture causes the Jackson/Immutables friction, makes model construction depend on
an ambient client, and leaves scoped resources such as messages underspecified.

## Java and Kotlin

### Discord4J

Discord4J does not expose a universal ref type. An `EntityRetriever` has distinct methods for globally addressed resources
and scoped resources: users, guilds and channels take one ID, while messages take `(channelId, messageId)` and roles take
`(guildId, roleId)`. Retrieval policy belongs to an `EntityRetrievalStrategy`, not to the ID or payload.
([retriever](https://github.com/Discord4J/Discord4J/blob/3.x/core/src/main/java/discord4j/core/retriever/EntityRetriever.java),
[strategies](https://github.com/Discord4J/Discord4J/blob/3.x/core/src/main/java/discord4j/core/retriever/EntityRetrievalStrategy.java))

Its higher-level entities are consistently stateful: a `Message` retains immutable `MessageData` and a
`GatewayDiscordClient`. `getChannel()` resolves `channelId` through the client and has a strategy-aware overload. Thus the
wire DTO remains separate from the behaviorful entity.
([Message](https://github.com/Discord4J/Discord4J/blob/master/core/src/main/java/discord4j/core/object/entity/Message.java))

### JDA

JDA uses client-owned managers/caches and entities that expose their `JDA` context. `UserSnowflake` is a useful small
analogue for a pure reference: it represents user identity, while `JDA#getUserById` and `JDA#retrieveUserById` perform
cache and REST resolution.
([UserSnowflake](https://github.com/discord-jda/JDA/blob/master/src/main/java/net/dv8tion/jda/api/entities/UserSnowflake.java),
[Member context](https://github.com/discord-jda/JDA/blob/master/src/main/java/net/dv8tion/jda/api/entities/Member.java))

### Kord

Kord domain entities carry raw data, the `Kord` client, and an `EntitySupplier`. The supplier owns scoped lookup methods;
strategies compose cache-only, REST-only, cache-then-REST, and cache-writing behavior. This is internally coherent because
all domain entities are client-bound, while transport data lives in separate common-module DTOs.
([EntitySupplier](https://github.com/kordlib/kord/blob/main/core/src/commonMain/kotlin/supplier/EntitySupplier.kt),
[strategies](https://github.com/kordlib/kord/blob/main/core/src/commonMain/kotlin/supplier/EntitySupplyStrategy.kt),
[Message](https://github.com/kordlib/kord/blob/main/core/src/commonMain/kotlin/entity/Message.kt),
[wire message](https://github.com/kordlib/kord/blob/main/common/src/commonMain/kotlin/entity/DiscordMessage.kt))

## Explicit typed-ID models

### Twilight and Serenity

Twilight's `Id<T>` is a scalar snowflake with a phantom marker such as `UserMarker` or `ChannelMarker`. Models, HTTP and
cache are separate crates. Its cache returns borrowed `Reference` guards, but those are cache-resource guards rather than
serialized entity relationships.
([typed ID](https://github.com/twilight-rs/twilight/blob/main/twilight-model/src/id/mod.rs),
[cache reference](https://github.com/twilight-rs/twilight/blob/main/twilight-cache-inmemory/src/lib.rs),
[crate split](https://github.com/twilight-rs/twilight#core-crates))

Serenity similarly has distinct `UserId`, `GuildId`, `ChannelId` and `MessageId` newtypes. Its cache lookup for a message is
explicitly `(channelId, messageId)`. `CacheRef`/`MessageRef` are borrowed cache guards whose lifetimes prevent them from
pretending to be durable model values.
([ID types](https://github.com/serenity-rs/serenity/blob/current/src/model/id.rs),
[cache and guards](https://github.com/serenity-rs/serenity/blob/current/src/cache/mod.rs))

### Arikawa, disgo and DiscordGo

Arikawa models relationships with entity-specific ID types over a snowflake. Its store is cache-only; a higher-level state
layer may perform REST fallback. Models remain in a package separate from API and state behavior.
([snowflakes](https://github.com/diamondburned/arikawa/blob/master/discord/snowflake.go),
[store](https://github.com/diamondburned/arikawa/blob/master/state/store.go))

Disgo also keeps IDs in models and places immutable values in replaceable generic caches. DiscordGo is less strongly typed,
using string IDs and mutable pointer caches, but still keeps cache lookup on `State` rather than introducing self-resolving
wire references.
([disgo guild models](https://github.com/disgoorg/disgo/blob/master/discord/guild.go),
[disgo cache](https://github.com/disgoorg/disgo/blob/master/cache/cache.go),
[DiscordGo state](https://github.com/bwmarrin/discordgo/blob/master/state.go))

### Remora.Discord

Remora is the clearest data-oriented .NET example. `IMessageReference` and its immutable implementation contain optional
wire fields. Cache integration is a separate backend package. A message reference and an optionally embedded referenced
message remain distinct values.
([reference interface](https://github.com/Remora/Remora.Discord/blob/master/Backend/Remora.Discord.API.Abstractions/API/Objects/Messages/IMessageReference.cs),
[immutable reference](https://github.com/Remora/Remora.Discord/blob/master/Backend/Remora.Discord.API/API/Objects/Messages/MessageReference.cs),
[cache backend](https://github.com/Remora/Remora.Discord/tree/master/Backend/Remora.Discord.Caching))

## Manager and partial-object designs

### discord.js

discord.js accepts either an object or string ID as a `Resolvable`. `DataManager.resolve` is cache-only; manager-specific
`fetch` methods perform REST. Managers own both caches and client context. Scoped entities are constructed with parent
context—for example, guild members are owned by a guild's member manager.
([resolution](https://github.com/discordjs/discord.js/blob/main/packages/discord.js/src/managers/DataManager.js#L39-L65),
[cached manager](https://github.com/discordjs/discord.js/blob/main/packages/discord.js/src/managers/CachedManager.js#L8-L67),
[guild members](https://github.com/discordjs/discord.js/blob/main/packages/discord.js/src/managers/GuildMemberManager.js#L39-L105))

### discord.py and Hikari

discord.py's `PartialMessage` carries both a channel and message ID. Its `fetch()` can therefore call the real
channel-scoped endpoint. `MessageReference` separately represents Discord's reference metadata and optionally exposes a
cached resolution.
([PartialMessage](https://github.com/Rapptz/discord.py/blob/master/discord/message.py#L1053-L1166),
[MessageReference](https://github.com/Rapptz/discord.py/blob/master/discord/message.py#L551-L680))

Hikari's `PartialMessage` likewise requires `id` and `channel_id`; REST has `fetch_message(channel, message)`. Cache helpers
are explicitly optional and do not silently turn every model relationship into a live handle.
([message model](https://docs.hikari-py.dev/en/stable/reference/hikari/messages/#hikari.messages.PartialMessage),
[REST lookup](https://docs.hikari-py.dev/en/stable/reference/hikari/impl/rest/#hikari.impl.rest.RESTClientImpl.fetch_message))

## The closest general-purpose reference: Discord.Net

Discord.Net's `Cacheable<T, TId>` carries an ID, an optional already-cached value, and an explicit asynchronous download
delegate. It does not disguise downloading as ordinary property access. It is a runtime event/API wrapper, not the scalar
wire representation used for every relationship.
([Cacheable](https://github.com/discord-net/Discord.Net/blob/dev/src/Discord.Net.Core/Entities/Cacheable.cs))

Discord.Net's message-reference DTO separately carries message, channel and guild IDs, and public messages separately expose
reference metadata and a resolved referenced message.
([wire reference](https://github.com/discord-net/Discord.Net/blob/dev/src/Discord.Net.Rest/API/Common/MessageReference.cs),
[public message](https://github.com/discord-net/Discord.Net/blob/dev/src/Discord.Net.Core/Entities/Messages/IMessage.cs))

## Implications for Dukecord

The generated `DiscordEntityRef<T>` prototype combined four roles:

- scalar JSON representation;
- compile-time target typing;
- identity and equality;
- live cache/REST resolution through a retained store.

That produces concrete costs:

- Immutables sees `@JsonValue id()` and correctly concludes that it cannot reconstruct mandatory `type` and `store`
  attributes from that scalar.
- Deserialization requires a Dukecord-bound reader even though the payload itself is ordinary data.
- Every retained entity graph can retain its store/client.
- Refs from different clients compare equal because `store` is auxiliary, despite resolving against different worlds.
- A one-ID ref cannot honor `FETCH_ONLY` for a message, member, role, or similar scoped Discord route.
- Hand-built entities must supply runtime infrastructure to satisfy what looks like a value-model builder.

### Recommended shape

Treat the relationship as a pure typed identity and let stores resolve it:

```java
@Value.Immutable
@JsonDeserialize(as = ImmutableDiscordEntityRef.class)
public interface DiscordEntityRef<T extends DiscordEntity> {

    @JsonValue
    Snowflake id();
}
```

`T` is a compile-time marker; it does not need to be stored as `Class<T>`. Because `id` is now the only mandatory attribute,
Immutables can generate the scalar Jackson creator that it rejected for the current three-attribute ref.

The call site is explicit about the resolver:

```java
DiscordEntityRef<User> owner = guild.owner();
Optional<User> cached = duke.users().getById(owner.id());
Optional<User> fetched = duke.users().getById(owner.id(), FETCH_ONLY);
```

The store may later accept `DiscordEntityRef<T>` directly if that improves ergonomics, but the ref should not retain the
store. This aligns with Twilight's typed ID, JDA's snowflake interfaces, and manager-based libraries' `resolveId` contracts.

### Scoped identities

Do not force every route into the global ref. Introduce scoped keys where a standalone operation needs the parent:

```java
@Value.Immutable
public interface MessageRef {
    Snowflake channelId();
    Snowflake id();
}
```

Likewise, future member and role operations naturally use `(guildId, entityId)`. A `last_message_id` inside a channel can
remain `DiscordEntityRef<Message>` as payload identity, but resolving it uses the containing channel ID. A standalone,
fetchable message handle must be a `MessageRef` carrying both IDs.

### Alternative if method navigation is the priority

If `guild.owner().get()` is a non-negotiable API goal, follow Discord4J/Kord fully: make every public domain entity
client/supplier-bound and keep separate transport data for Jackson. A runtime `Cacheable`-style reference can then retain a
resolver and expose distinct `getCached()` and `fetch()` operations. That is coherent, but it gives up the current goal of
using the same Immutables interface as both wire model and domain entity.

### Chosen self-resolving variant

Dukecord chose to keep `ref.get()` and accept the client-lifetime coupling. A concrete, package-private implementation is
cleaner for that choice than an Immutables value. The interface can put `@JsonValue` directly on `id()`, while the
implementation privately retains `type`, `id`, and the correctly typed store. This avoids exposing `store()` and prevents
Immutables from trying to construct runtime context from the scalar JSON value. Equality remains based on target type and ID.

Scoped references are still necessary when a fetch route needs more than one ID.
