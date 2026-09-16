# How Discord libraries cache state

Research date: 2026-09-15. Source links are pinned to commits.

The supplied index contains 115 repositories across 30 language headings at commit `10d39b6` ([generated library data](https://github.com/apacheli/discord-api-libs/blob/10d39b6632086ab0b3e18dfd12e1f9226202d4ba/data/libraries.json), [generated language map](https://github.com/apacheli/discord-api-libs/blob/10d39b6632086ab0b3e18dfd12e1f9226202d4ba/data/languages.json)). Many entries are webhook-only, interaction-only, voice-only, archived, or too small to establish a cache design. This report concentrates on current gateway libraries that represent the main architectural families, with all full Java gateway libraries prioritized. It is a representative architectural survey rather than a claim that every indexed repository has a gateway cache.

## Executive answer

Most mature Discord libraries use one of four designs:

1. **Mutable identity maps.** JDA, Javacord, discord.js, Eris, Discord.Net, DSharpPlus, DiscordGo, and Serenity retain one object per ID and patch it in place. This minimizes allocation and preserves object identity, but consumers can observe later mutations. Libraries must clone or copy the object when an event needs a stable `old` value.
2. **Immutable or copy-on-read snapshots in normalized stores.** Discord4J, Kord, Remora.Discord, disgo, Hikari, and Nostrum replace values. The important detail is normalization. Guild members, channels, roles, presences, and messages generally live in separate keyed stores instead of one enormous guild value.
3. **Mutable internal state with snapshot-like public values.** Hikari keeps compact internal records and returns shallow copies. Twilight exposes reduced cached models behind map guards. These designs separate storage concerns from the public entity API.
4. **No mandatory cache.** Discordeno makes caching an application or adapter concern. This works well for interactions and stateless workloads, but a gateway client still needs some session and relationship state.

Your Java-record idea is sound. The expensive design is not "records". It is a `GuildSnapshot` that owns copied maps of every member, channel, role, thread, voice state, and presence. A scalar guild update should allocate one shallow guild record, not rebuild a guild-sized object graph.

For Dukecord, I would use immutable public snapshots backed by normalized, per-kind stores. Keep `GuildSnapshot` to guild fields and perhaps lightweight ID views. Store members under `(guildId, userId)`, roles under `(guildId, roleId)`, and channels by ID with a guild index. Replace one record atomically on an update. Bound messages and presences, or disable them by default. Make cache misses and incomplete entities explicit in the API.

## Discord sets the constraints

Discord itself says clients should update cached resource state from gateway create, update, and delete events, but large applications should keep only state they need. It calls out member state as something an application may skip because some events carry member data inline ([gateway caching guidance](https://github.com/discord/discord-api-docs/blob/52ecc8a1908cc3a8dadb55671aef696bb1ed270e/developers/events/gateway.mdx#L567-L603)).

The initial guild member set is deliberately incomplete in common cases. Large guilds omit many offline members, clients without presence intent receive much less, guilds over 75,000 members receive only the bot and users in voice, and clients must request additional chunks ([Request Guild Members behavior](https://github.com/discord/discord-api-docs/blob/52ecc8a1908cc3a8dadb55671aef696bb1ed270e/developers/events/gateway-events.mdx#L190-L220), [Guild Create fields and limits](https://github.com/discord/discord-api-docs/blob/52ecc8a1908cc3a8dadb55671aef696bb1ed270e/developers/events/gateway-events.mdx#L741-L778)). `GUILD_MEMBERS` and `GUILD_PRESENCES` events are off by default, and some member updates arrive even without the member intent ([intent caveats](https://github.com/discord/discord-api-docs/blob/52ecc8a1908cc3a8dadb55671aef696bb1ed270e/developers/events/gateway.mdx#L433-L483)). A cache cannot honestly promise that a guild's member collection is complete merely because the guild is cached.

This argues for two independent concepts in Dukecord:

- whether a resource kind is enabled in the cache;
- whether the cached data for a particular guild is complete, partial, or unavailable.

## JVM libraries

### JDA and Javacord

JDA and Javacord sit at the mutable-identity end of the spectrum. JDA's `GuildImpl` is a long-lived mutable aggregate with child caches and mutable scalar fields ([guild implementation](https://github.com/discord-jda/JDA/blob/456434e160f607b25851ba460e18bf57e25bc470/src/main/java/net/dv8tion/jda/internal/entities/GuildImpl.java#L105-L165)); the guild update handler changes it in place and captures old scalar values for the event ([update handler](https://github.com/discord-jda/JDA/blob/456434e160f607b25851ba460e18bf57e25bc470/src/main/java/net/dv8tion/jda/internal/handle/GuildUpdateHandler.java#L46-L65)). Its cache supplies snapshot collection views behind read/write locking ([cache implementation](https://github.com/discord-jda/JDA/blob/456434e160f607b25851ba460e18bf57e25bc470/src/main/java/net/dv8tion/jda/internal/utils/cache/ReadWriteLockCache.java#L29-L84)). Feature flags can disable categories such as activity, voice state, emoji, sticker, and scheduled-event data, while member policy separately controls which members remain cached ([cache flags](https://github.com/discord-jda/JDA/blob/456434e160f607b25851ba460e18bf57e25bc470/src/main/java/net/dv8tion/jda/api/utils/cache/CacheFlag.java#L31-L101), [member policy](https://github.com/discord-jda/JDA/blob/456434e160f607b25851ba460e18bf57e25bc470/src/main/java/net/dv8tion/jda/api/utils/MemberCachePolicy.java#L29-L120)).

Javacord likewise keeps mutable maps and fields inside `ServerImpl` ([server implementation](https://github.com/Javacord/Javacord/blob/4d6c82218670dfebdbe50d647d9231216a5cd55c/javacord-core/src/main/java/org/javacord/core/entity/server/ServerImpl.java#L118-L208)). Its guild update handler compares payload values to the cached server, applies setters, and emits property-specific events ([update handler](https://github.com/Javacord/Javacord/blob/4d6c82218670dfebdbe50d647d9231216a5cd55c/javacord-core/src/main/java/org/javacord/core/util/handler/guild/GuildUpdateHandler.java#L90-L127)). The generic cache can use strong or weak references and supports a cleanup delay; the top-level entity cache owns separate caches for users, servers, channels, messages, and other kinds ([generic cache](https://github.com/Javacord/Javacord/blob/4d6c82218670dfebdbe50d647d9231216a5cd55c/javacord-core/src/main/java/org/javacord/core/util/cache/Cache.java#L23-L132), [entity cache](https://github.com/Javacord/Javacord/blob/4d6c82218670dfebdbe50d647d9231216a5cd55c/javacord-core/src/main/java/org/javacord/core/util/cache/JavacordEntityCache.java#L26-L59)). These APIs preserve entity identity and avoid replacement allocation, but references remain live rather than historical snapshots.

This model reduces allocations, but it spreads synchronization, partial-payload merge rules, and lifecycle mutation throughout entity classes.

### Discord4J

Discord4J is the most relevant precedent for Java records. Its local store has separate concurrent maps for immutable channels, guilds, members, messages, presences, roles, voice states, and other entities ([local store layout](https://github.com/Discord4J/Discord4J/blob/fefad18fa77fcbf7e210a05aad3af04e4d091c87/common/src/main/java/discord4j/common/store/impl/LocalStoreLayout.java#L45-L93)). A guild update builds and replaces one immutable guild value ([guild update](https://github.com/Discord4J/Discord4J/blob/fefad18fa77fcbf7e210a05aad3af04e4d091c87/common/src/main/java/discord4j/common/store/impl/LocalStoreLayout.java#L656-L666)). Public guild objects wrap immutable guild data plus a client reference, rather than owning the whole live child graph ([guild wrapper](https://github.com/Discord4J/Discord4J/blob/fefad18fa77fcbf7e210a05aad3af04e4d091c87/core/src/main/java/discord4j/core/object/entity/Guild.java#L60-L110)). Storage is configurable per entity type ([storage configuration](https://github.com/Discord4J/Discord4J/blob/fefad18fa77fcbf7e210a05aad3af04e4d091c87/common/src/main/java/discord4j/common/store/impl/StorageConfig.java#L30-L105)). Shared users use stable `AtomicReference` cells with compare-and-set replacement ([user update](https://github.com/Discord4J/Discord4J/blob/fefad18fa77fcbf7e210a05aad3af04e4d091c87/common/src/main/java/discord4j/common/store/impl/LocalStoreLayout.java#L1104-L1129)). Guild children do not need to be copied into every new guild value.

That means a guild-name or icon update has constant-size allocation even though the public entity represents a large guild. This is the shape Dukecord should copy, not necessarily Discord4J's exact API.

### Kord

Kord defines immutable guild data separately from its public entities ([guild data](https://github.com/kordlib/kord/blob/5e8e8cc4527e76fdd0ab86afb7cf11454adf3bd6/core/src/commonMain/kotlin/cache/data/GuildData.kt#L18-L74)). Its guild event handler updates guild, role, channel, thread, presence, voice-state, member, and emoji stores separately ([guild handler](https://github.com/kordlib/kord/blob/5e8e8cc4527e76fdd0ab86afb7cf11454adf3bd6/core/src/commonMain/kotlin/gateway/handler/GuildEventHandler.kt#L109-L147)). `KordCache` exposes separate stores for these resource types ([cache interface](https://github.com/kordlib/kord/blob/5e8e8cc4527e76fdd0ab86afb7cf11454adf3bd6/core/src/commonMain/kotlin/cache/KordCache.kt#L16-L139)), while entity supply strategies choose cache, REST, or fallback behavior without changing entity types ([supply strategy](https://github.com/kordlib/kord/blob/5e8e8cc4527e76fdd0ab86afb7cf11454adf3bd6/core/src/commonMain/kotlin/supplier/EntitySupplyStrategy.kt#L28-L80)).

The JVM split is therefore useful: JDA and Javacord prove that stable mutable identity works; Discord4J and Kord prove that immutable cached data works on the JVM when the cache is normalized.

## JavaScript and TypeScript

### discord.js

Every `CachedManager` obtains its store through the configurable `makeCache` factory. `_add` normally finds an existing entity by ID and patches that instance, preserving identity; a non-caching add instead patches a shallow clone ([manager implementation](https://github.com/discordjs/discord.js/blob/c626815bff3c985b78c9932e0e8bfb34b85343f9/packages/discord.js/src/managers/CachedManager.js#L12-L64)). `Base._update` first shallow-clones the old object and then mutates the live object, which lets guild update events expose old and new values while the canonical identity remains stable ([base update](https://github.com/discordjs/discord.js/blob/c626815bff3c985b78c9932e0e8bfb34b85343f9/packages/discord.js/src/structures/Base.js#L22-L34), [guild update action](https://github.com/discordjs/discord.js/blob/c626815bff3c985b78c9932e0e8bfb34b85343f9/packages/discord.js/src/client/actions/GuildUpdate.js#L6-L24)).

The cache factory supports per-manager limits and retention predicates. Most entity caches are unbounded by default; messages default to 200 per channel ([defaults and factory](https://github.com/discordjs/discord.js/blob/c626815bff3c985b78c9932e0e8bfb34b85343f9/packages/discord.js/src/util/Options.js#L84-L175)). Configured partials let events expose minimally populated objects when full cached state is unavailable ([partial construction](https://github.com/discordjs/discord.js/blob/c626815bff3c985b78c9932e0e8bfb34b85343f9/packages/discord.js/src/client/actions/Action.js#L57-L85)).

### Eris and Oceanic

Eris collections patch existing objects by ID. Their optional limit evicts the oldest insertion-order key, and zero disables retention ([Eris `Collection`](https://github.com/abalabahaha/eris/blob/ea2de118aef03e991fcf48adaf131c7d4f87e651/lib/util/Collection.js#L9-L72)). A guild update changes only fields present in the partial gateway payload ([guild update](https://github.com/abalabahaha/eris/blob/ea2de118aef03e991fcf48adaf131c7d4f87e651/lib/structures/Guild.js#L228-L275)). Global guild and user caches are unbounded; the main default bound is 100 messages per channel ([client caches](https://github.com/abalabahaha/eris/blob/ea2de118aef03e991fcf48adaf131c7d4f87e651/lib/Client.js#L135-L154), [message limit](https://github.com/abalabahaha/eris/blob/ea2de118aef03e991fcf48adaf131c7d4f87e651/lib/Client.js#L215-L224)).

Oceanic follows the same mutable identity-map model. Its typed collection patches the existing object and uses insertion-order eviction for bounded collections ([typed collection](https://github.com/OceanicJS/Oceanic/blob/936a539c8e7998a7b7f651a2471391cce2bdc489/lib/util/TypedCollection.ts#L15-L88)). Cache limits are configurable by entity kind; defaults are mostly unlimited, with bounded messages, audit logs, and direct-message channels, while `disableCache` sets all limits to zero ([client cache options](https://github.com/OceanicJS/Oceanic/blob/936a539c8e7998a7b7f651a2471391cce2bdc489/lib/Client.ts#L56-L140)). It serializes the old guild to a plain JSON value before mutating the cached guild for an update event ([gateway update](https://github.com/OceanicJS/Oceanic/blob/936a539c8e7998a7b7f651a2471391cce2bdc489/lib/gateway/events.ts#L423-L424)).

### Discordeno

Discordeno has no cache by default and explicitly permits external stores such as Redis or PostgreSQL ([README](https://github.com/discordeno/discordeno/blob/91f8d123ec9fd6a832851ea10c49d39f98948cee/README.md#L35-L47)). Its transformers construct fresh plain objects and only materialize configured desired properties ([guild transformer](https://github.com/discordeno/discordeno/blob/91f8d123ec9fd6a832851ea10c49d39f98948cee/packages/bot/src/transformers/guild.ts#L27-L98)); transformer customizers provide hooks for application-specific storage ([transformer configuration](https://github.com/discordeno/discordeno/blob/91f8d123ec9fd6a832851ea10c49d39f98948cee/packages/bot/src/transformers.ts#L380-L446)).

## Python

### Hikari

Hikari is a useful hybrid. Public models are mutable Python objects, but its cache stores compact internal data or defensive shallow copies. Guild reads and writes copy values, and updates return distinct old and new values ([guild cache operations](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/impl/cache.py#L492-L596)). It normalizes top-level resources; the internal guild record holds IDs and per-guild maps rather than one recursively materialized public guild graph ([cache maps](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/impl/cache.py#L67-L105), [guild record](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/internal/cache.py#L187-L267)).

Resource flags default to all enabled. Messages default to 300 and direct-message IDs to 50; most other enabled stores are unbounded ([cache configuration API](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/api/config.py#L36-L96), [defaults](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/impl/config.py#L310-L345)). Old event values are nullable because a cache can be disabled, miss, or evict ([event manager](https://github.com/hikari-py/hikari/blob/8057b5c9b8eb3d448a15ab0d216e8f1c3e60f0f9/hikari/impl/event_manager.py#L434-L486)).

### discord.py family

Pycord, representative of the discord.py family, uses ordinary dictionaries for core entities, an LRU capped at 128 direct-message channels, and a configurable message deque that defaults to 1,000 ([state initialization](https://github.com/Pycord-Development/pycord/blob/f5764d46b045dbe86e9b22dc9aad92bbf3a91356/discord/state.py#L174-L176), [cache construction](https://github.com/Pycord-Development/pycord/blob/f5764d46b045dbe86e9b22dc9aad92bbf3a91356/discord/state.py#L285-L311)). Message updates replace the cached `Message` and dispatch distinct old/new objects ([message parser](https://github.com/Pycord-Development/pycord/blob/f5764d46b045dbe86e9b22dc9aad92bbf3a91356/discord/state.py#L763-L820)). Guild updates shallow-copy the old guild, mutate the canonical guild, and dispatch both ([guild parser](https://github.com/Pycord-Development/pycord/blob/f5764d46b045dbe86e9b22dc9aad92bbf3a91356/discord/state.py#L1499-L1508)). Member cache flags support all/joined, voice-only, and interaction-related policies, with intent-dependent cleanup semantics ([member flags](https://github.com/Pycord-Development/pycord/blob/f5764d46b045dbe86e9b22dc9aad92bbf3a91356/discord/flags.py#L1261-L1368)).

## .NET and Elixir

### Discord.Net and DSharpPlus

Discord.Net keeps mutable live entities in concurrent identity maps. The client state owns global indexes and explicitly purges dependent channel and user entries when a guild is removed ([client state](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/ClientState.cs#L11-L138)). Each guild owns concurrent child maps ([guild storage](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L37-L68)). The message cache defaults to disabled and, when enabled, uses a per-channel FIFO cap ([socket config](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketConfig.cs#L68-L83), [message cache](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/MessageCache.cs#L9-L43)). `Cacheable<T>` represents an optional cached value plus an explicit REST download path ([cacheable wrapper](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Core/Entities/Cacheable.cs)).

DSharpPlus also keeps mutable concurrent maps for clients and guild children ([client maps](https://github.com/DSharpPlus/DSharpPlus/blob/26e8033595c690e868b2459fab2ebdc89bdd8f6e/DSharpPlus/Clients/DiscordClient.cs#L68-L84), [guild child maps](https://github.com/DSharpPlus/DSharpPlus/blob/26e8033595c690e868b2459fab2ebdc89bdd8f6e/DSharpPlus/Entities/Guild/DiscordGuild.cs#L312-L453)). Guild updates copy an old value and patch the live guild ([dispatcher](https://github.com/DSharpPlus/DSharpPlus/blob/26e8033595c690e868b2459fab2ebdc89bdd8f6e/DSharpPlus/Clients/DiscordClient.Dispatch.cs#L1417-L1550)). Its message cache uses weighted memory entries with sliding and absolute expiry, defaulting to 30 minutes and one day respectively ([message cache](https://github.com/DSharpPlus/DSharpPlus/blob/26e8033595c690e868b2459fab2ebdc89bdd8f6e/DSharpPlus/MessageCache.cs#L7-L32), [defaults](https://github.com/DSharpPlus/DSharpPlus/blob/26e8033595c690e868b2459fab2ebdc89bdd8f6e/DSharpPlus/DiscordConfiguration.cs#L18-L31)).

### Remora.Discord

Remora uses immutable API objects behind replaceable cache providers, including memory and distributed stores. Per-type absolute and sliding TTLs default to 30 and 10 seconds; zero disables that type ([settings](https://github.com/Remora/Remora.Discord/blob/b53661e2f5f099513d26cb98d9392bac2bf7403a/Backend/Remora.Discord.Caching/Services/CacheSettings.cs#L36-L83), [disable check](https://github.com/Remora/Remora.Discord/blob/b53661e2f5f099513d26cb98d9392bac2bf7403a/Backend/Remora.Discord.Caching/Services/CacheService.cs#L63-L91)). Caching a guild decomposes roles and emojis into separate keys ([normalization](https://github.com/Remora/Remora.Discord/blob/b53661e2f5f099513d26cb98d9392bac2bf7403a/Backend/Remora.Discord.Caching/Services/CacheService.cs#L197-L235)). Partial member updates reconstruct a complete immutable member from the event and old cached object ([early responder](https://github.com/Remora/Remora.Discord/blob/b53661e2f5f099513d26cb98d9392bac2bf7403a/Backend/Remora.Discord.Caching/Responders/EarlyCacheResponder.cs#L39-L159)). Delete handling temporarily keeps the evicted value so later event responders can obtain the previous state ([late responder](https://github.com/Remora/Remora.Discord/blob/b53661e2f5f099513d26cb98d9392bac2bf7403a/Backend/Remora.Discord.Caching/Responders/LateCacheResponder.cs#L35-L138)).

### Nostrum

Nostrum makes each cache category configurable as ETS, Mnesia, or no-op. Guild, member, user, and presence caches default to ETS; messages default to no-op ([guild cache behavior](https://github.com/Kraigie/nostrum/blob/03b06ba1c5094b83991097b1ce76b5fe2740324c/lib/nostrum/cache/guild_cache.ex#L1-L12), [message cache behavior](https://github.com/Kraigie/nostrum/blob/03b06ba1c5094b83991097b1ce76b5fe2740324c/lib/nostrum/cache/message_cache.ex#L1-L12)). ETS stores immutable guild structs. Scalar and nested channel updates build a new guild value and replace its tuple, returning old and new values ([ETS guild cache](https://github.com/Kraigie/nostrum/blob/03b06ba1c5094b83991097b1ce76b5fe2740324c/lib/nostrum/cache/guild_cache/ets.ex#L83-L166)). Elixir's persistent maps structurally share unchanged data, so this is closer to path-copying than rebuilding Java `Map.copyOf` containers.

## Rust and Go

### Twilight

Twilight uses separate concurrent maps for guilds, channels, members, users, roles, messages, and other resources. A cached guild does not own all child collections ([cache layout](https://github.com/twilight-rs/twilight/blob/132304966d767f5eef83606fda451670ee24d8cb/twilight-cache-inmemory/src/lib.rs#L193-L231)). Guild creation drains nested resources into those stores and builds a reduced cached guild ([guild event](https://github.com/twilight-rs/twilight/blob/132304966d767f5eef83606fda451670ee24d8cb/twilight-cache-inmemory/src/event/guild.rs#L10-L80), [cached guild model](https://github.com/twilight-rs/twilight/blob/132304966d767f5eef83606fda451670ee24d8cb/twilight-cache-inmemory/src/model/guild.rs#L20-L65)). Cache resource flags default to all enabled and messages default to 100 per channel ([configuration](https://github.com/twilight-rs/twilight/blob/132304966d767f5eef83606fda451670ee24d8cb/twilight-cache-inmemory/src/config.rs#L3-L85)). Its model trait lets large bots substitute thinner cached models ([cache model trait](https://github.com/twilight-rs/twilight/blob/132304966d767f5eef83606fda451670ee24d8cb/twilight-cache-inmemory/src/traits.rs#L1-L83)).

### Serenity and DiscordGo

Serenity keeps mutable guild values with nested maps in a concurrent cache and returns borrowed guards rather than snapshots ([cache layout and references](https://github.com/serenity-rs/serenity/blob/db6c7e188717c957c7051570dda60d314b6afe2d/src/cache/mod.rs#L59-L119), [stores](https://github.com/serenity-rs/serenity/blob/db6c7e188717c957c7051570dda60d314b6afe2d/src/cache/mod.rs#L179-L243)). Its message limit defaults to zero, and guild, channel, and user caching have toggles ([settings](https://github.com/serenity-rs/serenity/blob/db6c7e188717c957c7051570dda60d314b6afe2d/src/cache/settings.rs#L18-L49)).

DiscordGo uses a single state object with locks, pointer graphs, ID maps, and per-kind toggles; message caching defaults off ([state](https://github.com/bwmarrin/discordgo/blob/f43dd94faaacd5b163e9e783f14b5bd8be639fc9/state.go#L33-L76)). Updates overwrite the pointed-to guild value, preserving selected fields missing from partial updates ([guild replace](https://github.com/bwmarrin/discordgo/blob/f43dd94faaacd5b163e9e783f14b5bd8be639fc9/state.go#L87-L149)). Getters return raw pointers after releasing the lock, so callers can observe future mutation ([guild getter](https://github.com/bwmarrin/discordgo/blob/f43dd94faaacd5b163e9e783f14b5bd8be639fc9/state.go#L182-L194)). This is low-allocation but offers much weaker isolation than records.

### disgo

disgo's generic cache stores values under a read-write mutex and documents returned entities as immutable copies ([cache interface and implementation](https://github.com/disgoorg/disgo/blob/b61a46a3b8aa7f21e6bff103e9f7abf597fe94dd/cache/cache.go#L12-L65)). It has per-resource flags, per-entity policies, and custom cache injection ([flags](https://github.com/disgoorg/disgo/blob/b61a46a3b8aa7f21e6bff103e9f7abf597fe94dd/cache/cache_flags.go#L5-L36), [configuration](https://github.com/disgoorg/disgo/blob/b61a46a3b8aa7f21e6bff103e9f7abf597fe94dd/cache/cache_config.go#L8-L137)). Guild updates replace a cache value while preserving fields omitted by the event, and update events receive the old value ([guild handler](https://github.com/disgoorg/disgo/blob/b61a46a3b8aa7f21e6bff103e9f7abf597fe94dd/bot/handlers/guild_handlers.go#L111-L128)).

## A caching model for Dukecord

### Recommended storage shape

```java
public record GuildSnapshot(
    GuildId id,
    String name,
    UserId ownerId,
    Optional<String> iconHash,
    boolean available,
    Completeness members
) {}

public record MemberKey(GuildId guildId, UserId userId) {}

public interface EntityStore<K, V> {
    Optional<V> get(K key);
    Optional<V> put(K key, V value);
    Optional<V> remove(K key);
}
```

`GuildSnapshot` should not contain a copied `Map<UserId, MemberSnapshot>`. A higher-level `GuildView` can query `MemberStore` through a guild index when callers want members. Keep raw gateway payload types separate from complete snapshots because Discord update payloads omit fields. Merge a partial update with the current snapshot in one cache reducer, not in public record constructors.

### Update and event semantics

Process gateway events in shard sequence order. For each key, atomically compute a replacement:

```java
Change<GuildSnapshot> change = guilds.compute(id, previous -> {
    GuildSnapshot next = reducer.apply(previous, update);
    return new Change<>(previous, next);
});
```

The event can carry `Optional<GuildSnapshot> oldValue` and the new snapshot. Unlike a shallow clone of a mutable entity, the old record remains stable. Cache misses are ordinary and should not trigger hidden REST calls. Offer an explicit fetch operation if the caller wants network I/O, similar to Discord.Net's `Cacheable<T>` separation.

### Policy knobs worth shipping early

- Enable or disable each resource type. Dependencies should be explicit, such as presence caching requiring member/user identity information.
- Bound messages per channel. A FIFO ring is predictable and sufficient. TTL is useful for messages and REST response caches, but guild lifecycle state should normally be removed by gateway delete events rather than time expiry.
- Configure member retention: none, self-only, voice-only, event-seen, or chunked/all-known. Call the result "known members," not "all members," unless the cache has proof of completeness.
- Allow reduced cached models or projection hooks for large bots. Twilight's replaceable cached models and Discordeno's desired properties show why this matters.
- Keep availability distinct from absence. A temporary guild outage is not the same as leaving the guild.
- Expose cache provenance and completeness in diagnostics. A value can come from gateway state, REST, or application insertion and can be stale or partial.

### Churn estimate

A record replacement allocates one small object and references existing immutable field values. That is cheap compared with parsing gateway JSON and dispatching an event. Churn becomes material when each update copies an `O(n)` Java collection or recursively rebuilds children. `List.copyOf` and `Map.copyOf` do not provide persistent structural sharing. If you need immutable collection snapshots, use normalized stores first; consider a persistent collection only for small indexes that must themselves have snapshot semantics.

Records are ordinary heap classes with `private final` component fields, not allocation-free value types ([JLS 8.10.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.10.3)). HotSpot may scalar-replace an allocation only when it does not globally escape; a snapshot installed in a cache intentionally escapes, so assume it is a real allocation ([HotSpot escape analysis](https://docs.oracle.com/javase/10/vm/java-hotspot-virtual-machine-performance-enhancements.htm#JSGCT-GUID-2C078FE2-9DC8-4E59-9810-0C3EAF0C2A42)). `ConcurrentHashMap.compute` can atomically replace one key ([JDK API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html#compute(K,java.util.function.BiFunction))), but one ordered mutation lane per shard is still useful because a Discord event can update several stores.

Do not optimize this from intuition alone. Add a JMH benchmark with representative small, medium, and large guilds. Measure scalar guild updates, member presence updates, member joins/leaves, and bursty role changes. Record allocation rate and pause time under the collector you expect users to run. Compare normalized record replacement against a mutable entity baseline. My expectation, supported by Discord4J, Remora, Twilight, disgo, Hikari, and Nostrum's layouts, is that normalized record replacement will not be your bottleneck.

## Bottom line

The industry default is still mutable identity objects, largely for historical API ergonomics. The newer and more data-oriented designs normalize the cache and replace immutable or reduced values. Those designs fit Dukecord better.

Use records. Keep them shallow. Normalize high-cardinality children. Treat partiality as data. Put eviction policy behind per-resource configuration. That gives callers stable snapshots and makes concurrency much easier to reason about without paying to copy a guild on every presence event.
