# Catnip and Discord.Net: entity models, caching, and their failure modes

Research date: 2026-09-15. Source links are pinned to commits.

Everything below was read from the repositories themselves. Catnip is pinned to `650ea16`, the final commit of the archived `mewna/catnip` repository ([README status notice](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L1-L3)), which is Maven version 3.3.5 on RxJava 3.1.5 and Java 17 ([pom](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/pom.xml#L8-L17), [compiler release 17](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/pom.xml#L176-L182)). Discord.Net is pinned to `d34a50e`, the tip of `dev` at the time of reading.

One premise in the brief is wrong and it matters for the design: **Catnip never used the Immutables annotation processor.** There is no `org.immutables` dependency in the POM, no `@Value.Immutable` anywhere in the tree, and `git log --all -S "Value.Immutable"` over the whole history returns nothing. Catnip entities are Lombok-generated **mutable** POJOs with `@Getter @Setter @Builder`. The immutability-flavoured feel of the library comes from its *cache* discipline (whole-object replacement) and from the fact that nothing in the public API hands you a setter, not from the entity classes themselves.

## Part 1: Catnip

### 1.1 Base entity abstractions

The root is a two-method interface. Every entity carries a back-reference to the client, and serialization is delegated to a pluggable serializer rather than a `toJson()` on the entity:

```java
public interface Entity {
    Catnip catnip();

    @Nonnull @CheckReturnValue @SuppressWarnings("unchecked")
    default <T> T serialize() {
        return (T) catnip().entitySerializer().serialize(this);
    }
}
```

([`Entity.java#L45-L87`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/Entity.java#L45-L87))

Above `Entity` sits a set of tiny "partial" mixins in `entity/partials`: `Snowflake`, `Timestamped`, `GuildEntity`, `HasGuild`, `HasChannel`, `HasName`, `HasJoinedAt`, `Mentionable`, `Permissable` and about a dozen more ([directory](https://github.com/mewna/catnip/tree/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials)). `Snowflake` is an interface, not a value type — the ID is a primitive `long` with a `String` convenience overload, and `creationTime()` is derived:

```java
public interface Snowflake extends Entity, Timestamped {
    @Nonnull @CheckReturnValue
    default String id() { return Long.toUnsignedString(idAsLong()); }

    @CheckReturnValue
    long idAsLong();

    @Nonnull @CheckReturnValue
    default OffsetDateTime creationTime() { return Utils.creationTimeOf(idAsLong()); }
}
```

([`Snowflake.java#L43-L77`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials/Snowflake.java#L43-L77))

`Timestamped` is a pure behaviour mixin: it parses ISO timestamps and renders Discord `<t:...:R>` markup ([`Timestamped.java#L42-L89`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials/Timestamped.java#L42-L89)). `GuildEntity` composes `HasGuild` (which supplies `guildIdAsLong()`) with a cache lookup ([`GuildEntity.java#L42-L53`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials/GuildEntity.java#L42-L53), [`HasGuild.java#L36-L54`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials/HasGuild.java#L36-L54)).

The `catnip()` back-reference is injected after construction. Entities that need it implement a separate marker interface with a setter, and the builder sets it:

```java
public interface RequiresCatnip extends Entity {
    void catnip(@Nonnull Catnip catnip);
}
```

([`RequiresCatnip.java#L34-L36`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/RequiresCatnip.java#L34-L36))

Construction from JSON is one 1824-line god class, `EntityBuilder`, with ~90 `createX(JsonObject)` methods ([`EntityBuilder.java`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/EntityBuilder.java)). Every one of them ends in a `delegate(...)` call so that users can substitute their own subclasses:

```java
private <T, R extends T> R delegate(@Nonnull final Class<T> type, @Nonnull final T data) {
    return catnip.options().entityDelegator().delegate(type, data);
}
```

([`EntityBuilder.java#L175-L177`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/EntityBuilder.java#L175-L177), [`EntityDelegator.java#L49-L61`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/delegate/EntityDelegator.java#L49-L61))

There is **no `toJson()` on entities** in the final version. `Entity#serialize()` and `Entity#deserialize(...)` route to an `EntitySerializer<T>` supplied via options, and the default implementation throws:

```java
public final class DefaultEntitySerializer implements EntitySerializer<Void> {
    public Void serialize(@Nonnull final Entity entity) {
        throw new IllegalStateException("Entity serialization is not configured!");
    }
```

([`DefaultEntitySerializer.java#L39-L53`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/serialization/DefaultEntitySerializer.java#L39-L53), [`EntitySerializer.java#L46-L84`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/serialization/EntitySerializer.java#L46-L84))

`toJson()` in the current tree belongs only to *request-body* types (`GuildData`, `ChannelData`, `MemberData`, `MessageComponent`), via a `JsonConvertible` interface ([`JsonConvertible.java#L39`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/util/JsonConvertible.java#L39)). Entity-level JSON round-tripping was added in commit `ce02050` ("Support converting entities to/from JSON") and pulled back out with `3df8b67` ("[BREAKING] Fully remove Jackson"), leaving the pluggable-serializer hole.

### 1.2 Immutability: Lombok POJOs, not Immutables

A representative implementation class, in full:

```java
@Getter @Setter @Builder @Accessors(fluent = true) @NoArgsConstructor @AllArgsConstructor
public class MemberImpl implements Member, RequiresCatnip, Timestamped {
    private transient Catnip catnip;

    private long idAsLong;
    private long guildIdAsLong;
    private String nick;
    private Set<String> roleIds;
    private String joinedAt;
    private String premiumSince;
    private String avatarHash;
```

([`MemberImpl.java#L46-L62`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/guild/MemberImpl.java#L46-L62))

`GuildImpl` ([`#L52-L92`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/guild/GuildImpl.java#L52-L92)), `RoleImpl` ([`#L43-L63`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/guild/RoleImpl.java#L43-L63)) and `TextChannelImpl` ([`#L44-L63`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/channel/TextChannelImpl.java#L44-L63)) follow the identical shape. The public interfaces (`Member`, `Guild`, `Role`, `TextChannel`, `User`, `Message`) expose only getters plus `default` navigation/behaviour methods, so from the consumer's side the entities *read* as immutable snapshots, but nothing enforces it: the `Impl` classes are public, have public setters, and are handed straight to callers.

Absence is encoded three ways, never with `Optional`:

- **Nullable reference fields** with `@Nullable` JSR-305 annotations: `Member#nick()`, `Member#premiumSince()`, `Member#avatarHash()`, `Message#editedTimestamp()`, `Message#referencedMessage()` ([`Member.java#L78-L206`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Member.java#L78-L206)).
- **Sentinel `0` for absent snowflakes.** `Message#guildId()` converts a `0` long into `null`, and `guildIdAsLong() == 0` is the "this is a DM" test throughout the library ([`Message.java#L339-L372`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/message/Message.java#L339-L372)).
- **Empty `Maybe`** for anything resolved through the cache (below).

Note also that identity is ID-only and guild-blind:

```java
@Override
public boolean equals(final Object obj) {
    return obj instanceof Member && ((Member) obj).idAsLong() == idAsLong;
}
```

([`MemberImpl.java#L80-L88`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/guild/MemberImpl.java#L80-L88))

Two members of *different guilds* with the same user ID compare equal, and a stale member snapshot equals a fresh one. The `hashCode` is `Long.hashCode(idAsLong)`, so putting members from several guilds in one `Set` silently collapses them.

### 1.3 Relationship navigation

Everything guild-scoped resolves **through the cache**, returning RxJava `Maybe`/`Observable`, never `CompletionStage` and never `null`:

| Accessor | Resolution | Return |
| --- | --- | --- |
| `GuildEntity#guild()` | `cache().guild(guildIdAsLong())` | `Maybe<Guild>` |
| `Member#user()` | `cache().user(idAsLong())` | `Maybe<User>` |
| `Member#roles()` | `cache().roles(guildId())` + id lookup | `Set<Role>` (eager, nullable elements) |
| `Member#voiceState()` | `guild().voiceStates().getById(id())` | `Maybe<VoiceState>` |
| `Message#guild()` | `cache().guild(id)`, `Maybe.empty()` if id == 0 | `Maybe<Guild>` |
| `Message#channel()` | cache if guild, **REST** if DM | `Maybe<MessageChannel>` |
| `Message#member()` / `#author()` / `#mentionedUsers()` | **inline from the payload** | `Member` (nullable) / `User` / `List<User>` |
| `Message#mentionedRoles()` | `cache().role(guildId, id)` per id | `Observable<Role>` |
| `Guild#members()/roles()/channels()/emojis()` | live cache views | `NamedCacheView<T>` |

([`GuildEntity.java#L50-L52`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/partials/GuildEntity.java#L50-L52), [`Member.java#L69-L71`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Member.java#L69-L71), [`Message.java#L163-L194`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/message/Message.java#L163-L194), [`Guild.java#L391-L431`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Guild.java#L391-L431))

The one place Catnip silently falls back to the network is `Message#channel()`:

```java
default Maybe<MessageChannel> channel() {
    final long guild = guildIdAsLong();
    if(guild != 0) {
        return catnip().cache().channel(guild, channelIdAsLong()).map(Channel::asMessageChannel);
    } else {
        return catnip().rest().channel().getChannelById(channelId()).map(Channel::asMessageChannel).toMaybe();
    }
}
```

([`Message.java#L230-L239`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/message/Message.java#L230-L239))

Two navigation methods are outright buggy. `Member#roles()` maps ids through `CacheView#getById`, which is documented to return `null` on a miss, and then collects into `Collectors.toUnmodifiableSet()`, which rejects nulls:

```java
default Set<Role> roles() {
    final CacheView<Role> roles = catnip().cache().roles(guildId());
    return roleIds().stream()
            .map(roles::getById)
            .collect(Collectors.toUnmodifiableSet());
}
```

([`Member.java#L108-L115`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Member.java#L108-L115); `getById` nullability at [`CacheView.java#L80-L94`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/view/CacheView.java#L80-L94))

A single uncached role turns a "just read a field" call into an NPE. The method twelve lines below, `orderedRoles(Comparator)`, does the null check correctly ([`Member.java#L128-L141`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Member.java#L128-L141)). That inconsistency inside one file is the clearest possible argument for making cache misses a *type*, not a convention.

### 1.4 The cache

`EntityCache` is the read side and `EntityCacheWorker extends EntityCache` is the write side ([`EntityCache.java`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/EntityCache.java), [`EntityCacheWorker.java`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/EntityCacheWorker.java)). The read API is uniformly `Maybe<T>` for single lookups and `CacheView<T>` / `NamedCacheView<T>` for collections, with `String` and `long` overloads of everything.

**What is cached:** guilds, users, presences, members, roles, guild channels, DM channels, thread members, custom emoji, voice states, and the self user. **Messages are not cached at all** — `EntityCache` has no message method and `grep -i message src/main/java/com/mewna/catnip/cache/*.java` returns nothing. This is a deliberate, load-bearing simplification: it is why `MESSAGE_UPDATE` cannot carry an old value (§1.5).

The store is **normalized per guild**, not a guild object graph:

```java
protected final MutableNamedCacheView<Guild> guildCache = createGuildCacheView();
protected final Map<Long, MutableNamedCacheView<Member>> memberCache = new ConcurrentHashMap<>();
protected final Map<Long, MutableNamedCacheView<Role>> roleCache = new ConcurrentHashMap<>();
protected final Map<Long, MutableNamedCacheView<GuildChannel>> guildChannelCache = new ConcurrentHashMap<>();
protected final Map<Long, MutableNamedCacheView<CustomEmoji>> emojiCache = new ConcurrentHashMap<>();
protected final Map<Long, MutableCacheView<VoiceState>> voiceStateCache = new ConcurrentHashMap<>();
```

([`MemoryEntityCache.java#L77-L91`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L77-L91))

Correspondingly `GuildImpl` holds **only scalars** — no member map, no channel list ([`GuildImpl.java#L58-L92`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/guild/GuildImpl.java#L58-L92)) — and `Guild#members()` is a `default` method that just reads the cache ([`Guild.java#L409-L413`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Guild.java#L409-L413)). Replacing a guild on `GUILD_UPDATE` is therefore a constant-size operation.

Two concrete caches ship: `SplitMemoryEntityCache` (default) keys users/presences/DM channels by shard so a shard reconnect can invalidate just its slice, and `UnifiedMemoryEntityCache` shares one map across shards ([`SplitMemoryEntityCache.java#L40-L105`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/SplitMemoryEntityCache.java#L40-L105), [`UnifiedMemoryEntityCache.java#L42-L88`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/UnifiedMemoryEntityCache.java#L42-L88), selected at [`CatnipOptions.java#L95`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/CatnipOptions.java#L95)). `NoopEntityCache` and `CustomizableEntityCache` are the opt-out paths.

**There is no `CacheFlag` in the final version.** It existed — `DROP_EMOJI`, `DROP_VOICE_STATES`, `DROP_GAME_STATUSES` — and was deleted in [`2f33e57` "Delete cache flags (intents solve this problem)"](https://github.com/mewna/catnip/commit/2f33e57f6a47c7c9e28947b08aa85d4c362375f3) (2020-11-10). The README still links to the dead file ([`README.md#L43-L46`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L43-L46)). Catnip's post-2020 position is: intents decide what arrives, and if you want finer control you swap the whole `EntityCacheWorker`. The all-or-nothing granularity is real — you cannot say "cache members but not presences" without writing a cache class.

Gateway updates land in one `switch` over the raw event name ([`MemoryEntityCache.java#L454-L628`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L454-L628)). The important cases:

- `GUILD_CREATE` calls `createAndCacheGuild`, which drains `roles`, `channels`, `members`, `emojis`, `presences`, `voice_states` and `threads` into their own stores before building the scalar guild ([`EntityBuilder.java#L1065-L1096`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/EntityBuilder.java#L1065-L1096)).
- `GUILD_UPDATE` is **whole-object replacement, no merge** — safe only because `GuildImpl` is scalars-only ([`#L494-L497`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L494-L497)).
- `GUILD_DELETE` explicitly tears down the member, role, channel, emoji, voice-state and thread sub-caches ([`#L498-L507`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L498-L507)).
- `GUILD_MEMBER_UPDATE` is the only true merge, and it is done by **synthesizing a fake gateway payload** from the old cached member and re-running the parser:

```java
final JsonObject data = JsonObject.builder()
        .value("user", user)
        .value("roles", payload.getArray("roles"))
        .value("nick", payload.getString("nick"))
        .value("deaf", old.deaf())
        .value("mute", old.mute())
        .value("joined_at", old.joinedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .done();
final Member member = catnip.entityBuilder().createMember(guild, data);
cacheMember(member);
```

([`MemoryEntityCache.java#L536-L562`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L536-L562))

This is worth dwelling on, because it is exactly the trap a snapshot-based library must avoid. The synthesized object omits `premium_since` and `avatar`, both of which `createMember` reads ([`EntityBuilder.java#L726-L735`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/EntityBuilder.java#L726-L735)), so **every member update silently wipes the member's boost date and per-guild avatar**. The `deaf`/`mute` keys it *does* write are `Single<Boolean>`-returning default methods on `Member` ([`Member.java#L149-L163`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Member.java#L149-L163)) stuffed into a JSON object, and `createMember` never reads them back — dead code that tracks [issue #489, "Member#mute()/deaf() don't work"](https://github.com/mewna/catnip/issues/489). And if the member is not already cached the update is dropped with a warning, which is [issue #255](https://github.com/mewna/catnip/issues/255). A hand-written "merge old snapshot + partial payload → new snapshot" reducer with an exhaustive field list would have made all three bugs impossible.

`PRESENCE_UPDATE` merges the same way but by hand, field by field, defaulting each to the old user's value ([`#L589-L619`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L589-L619)).

**REST responses do not populate the cache**, with one leaky exception. `RestGuild#getGuild` uses `createGuild`, not `createAndCacheGuild` ([`RestGuild.java#L303`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/rest/handler/RestGuild.java#L303)). But `EntityBuilder#createMember` writes to the user cache as a side effect of *building an object*, wherever it is called from — including REST paths like `listGuildMembers`:

```java
public Member createMember(@Nonnull final String guildId, @Nonnull final String id, @Nonnull final JsonObject data) {
    final JsonObject userData = data.getObject("user");
    final long guild = Long.parseUnsignedLong(guildId);
    if(userData != null) {
        final int shards = catnip.shardManager().shardCount();
        final int shardMod = shards == 0 ? 1 : shards;
        catnip.cacheWorker().bulkCacheUsers((int) ((guild >> 22) % shardMod),
                Collections.singletonList(createUser(userData)));
    }
```

([`EntityBuilder.java#L709-L718`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/impl/EntityBuilder.java#L709-L718))

Parsing is not supposed to mutate global state. This also means a `NoopEntityCache` user still pays for the call, and the shard is *guessed* from the guild id rather than known.

`CacheView` is a read-only, live, non-copying window over a store — `size()`, `getById(long)` (nullable), `findAny`, `find(predicate, supplier)`, `Iterable<T>` — with `NamedCacheView` adding name-indexed lookup ([`CacheView.java#L49-L138`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/view/CacheView.java#L49-L138)). "View, not copy" is good, but it is also the reason `Guild#members()` can change under a handler mid-iteration, and the member name index needs a synchronous cache read that the author flagged himself:

```java
// FIXME: THIS IS UNSAFE -- REWRITE CACHE VIEWS AS ASYNC
final User u = user(m.idAsLong()).blockingGet();
```

([`MemoryEntityCache.java#L113-L121`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L113-L121))

### 1.5 Events: typing, dispatch, and old/new values

Events are typed by a static table of `EventType<T>` / `DoubleEventType<T, E>` tokens, each pairing a raw gateway name with the payload class:

```java
EventType<Channel>                     CHANNEL_CREATE      = event(Raw.CHANNEL_CREATE, Channel.class);
DoubleEventType<Guild, Guild>          GUILD_UPDATE        = doubleEvent(Raw.GUILD_UPDATE, Guild.class, Guild.class);
EventType<Member>                      GUILD_MEMBER_ADD    = event(Raw.GUILD_MEMBER_ADD, Member.class);
DoubleEventType<Member, PartialMember> GUILD_MEMBER_UPDATE = doubleEvent(Raw.GUILD_MEMBER_UPDATE, Member.class, PartialMember.class);
EventType<Message>                     MESSAGE_CREATE      = event(Raw.MESSAGE_CREATE, Message.class);
EventType<Message>                     MESSAGE_UPDATE      = event(Raw.MESSAGE_UPDATE, Message.class);
```

([`DiscordEvent.java#L56-L98`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/DiscordEvent.java#L56-L98); the raw-name constants are in the nested `Raw` interface at [`#L106+`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/DiscordEvent.java#L106))

`DoubleEventType<T, E>` is *the* old/new mechanism: subscribing yields `Observable<Pair<T, E>>` where left is the previous value and right is the new one ([`DoubleEventType.java#L39-L73`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/event/DoubleEventType.java#L39-L73)). `GUILD_UPDATE`, `GUILD_ROLE_UPDATE`, `CHANNEL_UPDATE`, `THREAD_UPDATE` and `USER_UPDATE` are symmetric; `GUILD_MEMBER_UPDATE` and `PRESENCE_UPDATE` are *asymmetric* — the new side is a `PartialMember` / `PresenceUpdate`, honestly typed as "less than a full entity". `MESSAGE_UPDATE` is a plain `EventType<Message>` with no old value, because messages are not cached.

The old value is fetched from the cache at dispatch time:

```java
case Raw.GUILD_UPDATE -> {
    final Guild guild = catnip.entityBuilder().createGuild(data);
    if(catnip.cacheWorker().canProvidePreviousState(GUILD)) {
        catnip.cache().guild(guild.idAsLong())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .subscribe(old -> catnip.dispatchManager().dispatchEvent(type, ImmutablePair.of(old.orElse(null), guild)),
                        e -> cacheErrorLog(type, e));
    } else {
        catnip.dispatchManager().dispatchEvent(type, ImmutablePair.of(null, guild));
    }
}
```

([`DispatchEmitter.java#L169-L180`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/DispatchEmitter.java#L169-L180))

`EntityCacheWorker#canProvidePreviousState(CachedEntityState)` lets a custom cache declare per-resource-type whether it can supply old values, in which case the left side is `null` ([`EntityCacheWorker.java#L90-L102`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/EntityCacheWorker.java#L90-L102)). That is a genuinely good idea: capability is explicit rather than assumed.

For that read to see the *old* value, the buffer has to emit the event **before** updating the cache, and it does — for a hard-coded set of event types:

```java
private static final Set<String> FIRE_BEFORE_CACHING = Set.of(
        Raw.CHANNEL_UPDATE, Raw.CHANNEL_DELETE,
        Raw.GUILD_UPDATE, Raw.GUILD_DELETE,
        Raw.GUILD_ROLE_UPDATE, Raw.GUILD_ROLE_DELETE,
        Raw.GUILD_MEMBER_UPDATE, Raw.GUILD_MEMBER_REMOVE);
...
private void cacheAndDispatch(final String type, final int id, final JsonObject event) {
    final JsonObject d = event.getObject("d");
    if(FIRE_BEFORE_CACHING.contains(type)) {
        // TODO: Will this work always?
        emitter().emit(event);
        // TODO: Make this not be blocking
        maybeCache(type, id, d).blockingSubscribe();
    } else {
        maybeCache(type, id, d).subscribe(() -> emitter().emit(event));
    }
}
```

([`CachingBuffer.java#L84-L93`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/buffer/CachingBuffer.java#L84-L93), [`#L247-L258`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/buffer/CachingBuffer.java#L247-L258))

This is the single worst thing in Catnip's design. Inside a `GUILD_UPDATE` handler, `catnip.cache().guild(id)` returns the **pre-update** guild, while the right side of your pair is the new one — the cache and the event disagree about reality for the duration of the handler, and which way round depends on a private static `Set`. The two `TODO`s and the `blockingSubscribe()` are the author's own verdict.

Subscription is `catnip.observable(EventType)` / `flowable(...)`; `on(...)` is private in 3.x ([`Catnip.java#L654-L750`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/Catnip.java#L654-L750)). The README's canonical example:

```java
final Catnip catnip = Catnip.catnip("your token goes here");
catnip.observable(DiscordEvent.MESSAGE_CREATE)
    .filter(msg -> msg.content().equals("!ping"))
    .subscribe(msg -> { msg.respond("pong!"); }, error -> error.printStackTrace());
catnip.connect();
```

([`README.md#L61-L67`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L61-L67))

The README also spends a paragraph warning users off `Observable#forEach` because it swallows the stream on the first uncaught exception ([`README.md#L104-L111`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L104-L111)) — a tax paid purely for choosing Rx as the public surface.

One small thing that a stricter model would have caught: `INVITE_DELETE` is registered against `Raw.INVITE_CREATE` ([`DiscordEvent.java#L75-L76`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/DiscordEvent.java#L75-L76)).

### 1.6 REST

Every REST method returns an RxJava type — `Single<T>` for one entity, `Observable<T>` for collections and paginated endpoints, `Completable` for void — plus a parallel `...Raw` method returning the untouched `JsonObject`:

```java
public Single<Message> createMessage(@Nonnull final String channelId, @Nonnull final MessageOptions options) {
    return Single.fromObservable(createMessageRaw(channelId, options).map(entityBuilder()::createMessage));
}
```

([`RestChannel.java#L81-L213`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/rest/handler/RestChannel.java#L81-L213), [`RestGuild.java#L303-L568`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/rest/handler/RestGuild.java#L303-L568))

Request bodies are separate mutable builder classes, never entities: `MessageOptions` (content, embeds, files, allowed-mentions parse flags, flags, message reference, components) with copy constructors from both `MessageOptions` and `Message` ([`MessageOptions.java#L58-L203`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/message/MessageOptions.java#L58-L203)), and `GuildData` / `ChannelData` / `MemberData` / `PermissionOverrideData` implementing `JsonConvertible#toJson()` ([`GuildData.java#L200-L213`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/rest/guild/GuildData.java#L200-L213)).

**There was never a sync/blocking API.** The only blocking escape hatch is RxJava's own `blockingGet()`/`blockingSubscribe()`, which Catnip itself resorts to internally in the two places noted above. For Dukecord — explicitly a blocking library — this is the cleanest point of departure: Catnip's entity and cache design survives the removal of Rx, because the Rx types are only ever used as "zero or one" (`Maybe`), "exactly one" (`Single`) and "many" (`Observable`) wrappers, which map onto `Optional<T>`, `T`, and `List<T>` in a blocking API almost mechanically.

### 1.7 What worked, what hurt

From the README's own "why write a fourth Java lib" section, the design goals were: map ~1:1 to the Discord API rather than inventing high-level events, maximize customizability (raw websocket interception, custom distributed caches, pluggable session/ratelimit storage), keep modular (REST-only or gateway-only) usage identical to full usage, and return Rx types everywhere ([`README.md#L139-L150`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L139-L150), features list at [`#L36-L57`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/README.md#L36-L57)).

Pain points visible in the tracker and the code:

- **Identity vs snapshot was never resolved.** Entities are mutable POJOs handed out directly, `equals` is ID-only and guild-blind, and the cache replaces objects wholesale — so a reference you hold is a snapshot, but nothing in the type system says so, and `==`/`equals` behave as if it were an identity.
- **Partial-payload merging via synthesized JSON** silently drops fields ([#489](https://github.com/mewna/catnip/issues/489), [#255](https://github.com/mewna/catnip/issues/255), [#615 "Update user cache from GUILD_MEMBER_UPDATE"](https://github.com/mewna/catnip/issues/615)).
- **Counts that lie.** [#192 "GuildImpl#memberCount is a lie"](https://github.com/mewna/catnip/issues/192) — a field set once at `GUILD_CREATE` and never updated. The fix was to delete the field, so `memberCount()` is now `members().size()` ([`Guild.java#L378-L385`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/entity/guild/Guild.java#L378-L385)) — i.e. it now reports *cached* members and undercounts whenever chunking is off. Neither version tells the caller which question it is answering.
- **Presence memory.** [#99 "Figure out how to deal with all the garbage that gets created"](https://github.com/mewna/catnip/issues/99), [#92 "Switch to Trove or something else for default caching"](https://github.com/mewna/catnip/issues/92) (the outcome is the Koloboke primitive-keyed map at [`KolobokeLongEntityMap.java`](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/view/KolobokeLongEntityMap.java)), and [#91 "ConcurrentHashMap in MemoryEntityCache may block"](https://github.com/mewna/catnip/issues/91). Catnip's answer to presence memory was ultimately "don't enable the intent", after deleting `DROP_GAME_STATUSES`.
- **Sync cache reads forced async.** [#154 "API for asynchronous cache reads"](https://github.com/mewna/catnip/issues/154) and [#188 "Add asynchronous cache view APIs"](https://github.com/mewna/catnip/issues/188) drove the whole cache surface to `Maybe`, which is what forces `blockingGet()` in the member-name path and makes `Member#roles()` awkward enough that it was written wrong.
- **Cache/event ordering.** [#104 "Make DispatchEmitter properly cache-aware"](https://github.com/mewna/catnip/issues/104) produced the `FIRE_BEFORE_CACHING` hack.

## Part 2: Discord.Net caching traps

### 2.1 `Cacheable<TEntity, TId>`

```csharp
public struct Cacheable<TEntity, TId>
    where TEntity : IEntity<TId>
    where TId : IEquatable<TId>
{
    public bool HasValue { get; }
    public TId Id { get; }
    public TEntity Value { get; }
    private Func<Task<TEntity>> DownloadFunc { get; }

    public Task<TEntity> DownloadAsync() => DownloadFunc();
    public async Task<TEntity> GetOrDownloadAsync() => HasValue ? Value : await DownloadAsync().ConfigureAwait(false);
}
```

([`Cacheable.cs#L11-L63`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Core/Utils/Cacheable.cs#L11-L63))

The idea is right: a cache miss is a value, `Id` is always present, and the network call is explicit. Three things go wrong in practice.

**First**, there is a second, four-parameter overload in the same file:

```csharp
public struct Cacheable<TCachedEntity, TDownloadableEntity, TRelationship, TId>
    ...
    public async Task<TRelationship> GetOrDownloadAsync() => HasValue ? Value : await DownloadAsync().ConfigureAwait(false);
```

([`Cacheable.cs#L64-L119`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Core/Utils/Cacheable.cs#L64-L119))

`GetOrDownloadAsync()` returns `TRelationship`, a common *interface* of two unrelated concrete types. The static type deliberately tells you nothing about what you actually got.

**Second**, `GetOrDownloadAsync` is documented to throw `NullReferenceException` when the entity is deleted and uncached ([`Cacheable.cs#L53-L62`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Core/Utils/Cacheable.cs#L53-L62)) — and `MessageDeleted` hands you a `Cacheable<IMessage, ulong>` whose download can *never* succeed, as its own docs say ([`BaseSocketClient.Events.cs#L129-L139`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/BaseSocketClient.Events.cs#L129-L139)). The type is the same in both the recoverable and the unrecoverable case.

**Third**, the event signatures mix `Cacheable`, `Optional`, and plain nullable references for the *same* notion of "might not be there":

```csharp
public event Func<Cacheable<IMessage, ulong>, Cacheable<IMessageChannel, ulong>, Task> MessageDeleted
public event Func<Cacheable<IMessage, ulong>, SocketMessage, ISocketMessageChannel, Task> MessageUpdated
public event Func<Cacheable<IUserMessage, ulong>, Cacheable<IMessageChannel, ulong>, SocketReaction, Task> ReactionAdded
```

([`BaseSocketClient.Events.cs#L150`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/BaseSocketClient.Events.cs#L150), [`#L209`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/BaseSocketClient.Events.cs#L209), [`#L245`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/BaseSocketClient.Events.cs#L245))

`MessageUpdated`'s third parameter is a raw `ISocketMessageChannel` that the handler sets to `null` when the channel is not in the socket cache; `SocketReaction` uses `Optional<IUser> User`, `Optional<SocketUserMessage> Message`, *and* a nullable `ISocketMessageChannel Channel` in one class ([`SocketReaction.cs#L23-L69`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketReaction.cs#L23-L69)). Three encodings of "absent" in one callback.

### 2.2 The Socket/Rest split

The official framing is that socket entities come from the gateway and live in the cache, REST entities come from HTTP and "will be disposed after use", and navigating between variants requires casting ([entities guide](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/docs/guides/entities/introduction.md#L18-L71)). In practice the same call returns different concrete types depending on cache state:

```csharp
async Task<IGuildUser> IGuild.GetUserAsync(ulong id, CacheMode mode, RequestOptions options)
{
    var user = GetUser(id);
    if (user is not null || mode == CacheMode.CacheOnly)
        return user;

    return await GuildHelper.GetUserAsync(this, Discord, id, options).ConfigureAwait(false);
}
```

([`SocketGuild.cs#L2262-L2269`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L2262-L2269))

Cache hit → `SocketGuildUser`; cache miss → `RestGuildUser`; `CacheMode.CacheOnly` miss → `null`. Same signature, three outcomes. Users have been filing this for years: [#1816 "ReactionAdded Cacheable msg returns RestUserMessage rather than SocketUserMessage"](https://github.com/discord-net/Discord.Net/issues/1816) and [#890 "Channel entities produce message entities of inconsistent types"](https://github.com/discord-net/Discord.Net/issues/890), which concludes "there's little point to me ever using `SocketMessage` instead of `IMessage`".

The author-typing problem is worse than the brief suggests. `SocketMessage.Author` is declared `SocketUser`, not `SocketGuildUser` ([`SocketMessage.cs#L29`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketMessage.cs#L29)), and the `MESSAGE_CREATE` handler picks the concrete type at runtime:

```csharp
SocketUser author;
if (guild != null)
{
    if (data.WebhookId.IsSpecified)
        author = SocketWebhookUser.Create(guild, State, data.Author.Value, data.WebhookId.Value);
    else
        author = guild.GetUser(data.Author.Value.Id);
}
else
    author = (channel as SocketChannel).GetUser(data.Author.Value.Id);

if (author == null)
{
    if (guild != null)
    {
        if (data.Member.IsSpecified) // member isn't always included, but use it when we can
        {
            data.Member.Value.User = data.Author.Value;
            author = guild.AddOrUpdateUser(data.Member.Value);
        }
        else
            author = guild.AddOrUpdateUser(data.Author.Value); // user has no guild-specific data
    }
```

([`DiscordSocketClient.EventHandling.cs#L867-L897`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketClient.EventHandling.cs#L867-L897))

So `message.Author as SocketGuildUser` returns `null` for webhook messages (it is a `SocketWebhookUser`) and for DMs, which is well known. The subtler trap is the last branch. `AddOrUpdateUser(UserModel)` inserts a **fully synthetic member** into the guild's member map:

```csharp
internal SocketGuildUser AddOrUpdateUser(UserModel model)
{
    if (_members.TryGetValue(model.Id, out SocketGuildUser member))
        member.GlobalUser?.Update(Discord.State, model);
    else
    {
        member = SocketGuildUser.Create(this, Discord.State, model);
        member.GlobalUser.AddRef();
        _members[member.Id] = member;
        DownloadedMemberCount++;
    }
    return member;
}
```

([`SocketGuild.cs#L1229-L1241`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L1229-L1241))

and the `UserModel` overload of `Create` explicitly blanks the roles:

```csharp
internal static SocketGuildUser Create(SocketGuild guild, ClientState state, UserModel model)
{
    var entity = new SocketGuildUser(guild, guild.Discord.GetOrCreateUser(state, model));
    entity.Update(state, model);
    entity.UpdateRoles([]);
    return entity;
}
```

([`SocketGuildUser.cs#L163-L169`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Users/SocketGuildUser.cs#L163-L169))

The result is a `SocketGuildUser` whose cast *succeeds*, whose `Roles` is empty, whose `Nickname` and `JoinedAt` are `null`, and which is now cached and indistinguishable from a real member. `Roles` even filters out unresolvable role ids while reporting the raw length:

```csharp
public IReadOnlyCollection<SocketRole> Roles
    => _roleIds.Select(id => Guild.GetRole(id)).Where(x => x != null).ToReadOnlyCollection(() => _roleIds.Length);
```

([`SocketGuildUser.cs#L101-L102`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Users/SocketGuildUser.cs#L101-L102))

Permission checks written against `user.Roles` therefore fail *open or closed at random* depending on how the member first entered the cache. A cast that succeeds while the object is a lie is strictly worse than a cast that fails.

### 2.3 Intents, member downloads, and cache sizes

```csharp
public int MessageCacheSize { get; set; } = 0;          // message cache OFF by default
public int AuditLogCacheSize { get; set; } = 0;
public int LargeThreshold { get; set; } = 250;
public bool AlwaysDownloadUsers { get; set; } = false;  // member chunking OFF by default
public GatewayIntents GatewayIntents { get; set; } = GatewayIntents.AllUnprivileged;
```

([`DiscordSocketConfig.cs#L81-L199`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketConfig.cs#L81-L199))

`AllUnprivileged` excludes `GuildMembers`, `GuildPresences` **and** `MessageContent` ([`GatewayIntents.cs#L72-L83`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Core/GatewayIntents.cs#L72-L83)). Nothing throws when they are missing. `SocketGuild.Users` simply returns whatever happens to be in `_members`, with the warning buried in XML docs:

```csharp
public IReadOnlyCollection<SocketGuildUser> Users => _members.ToReadOnlyCollection();
```

([`SocketGuild.cs#L378-L397`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L378-L397))

`GetUser` returns `null` on a miss, again documented only in prose ([`SocketGuild.cs#L1210-L1224`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L1210-L1224)). The completeness flag is a heuristic comparison, with the more honest implementation commented out on the same line:

```csharp
public bool HasAllMembers => MemberCount <= DownloadedMemberCount;// _downloaderPromise.Task.IsCompleted;
```

([`SocketGuild.cs#L162-L167`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L162-L167))

Because `AddOrUpdateUser` increments `DownloadedMemberCount` for every synthetic member created from a message author or a reaction payload, `HasAllMembers` can flip to `true` on a guild whose member cache is mostly fakes — and `IGuild.GetUsersAsync` uses exactly that flag to decide whether to chunk:

```csharp
if (mode == CacheMode.AllowDownload && !HasAllMembers)
    return (await GetUsersAsync(options).FlattenAsync().ConfigureAwait(false)).ToImmutableArray();
else
    return Users;
```

([`SocketGuild.cs#L2250-L2256`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L2250-L2256))

`DownloadUsersAsync()` is the manual fix ([`SocketGuild.cs#L1323-L1324`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Guilds/SocketGuild.cs#L1323-L1324)), and `AlwaysDownloadUsers`'s own docs concede that "it can be difficult to fill the cache completely on large guilds" and suggest overriding the command framework's user `TypeReader` to fall back to REST ([`DiscordSocketConfig.cs#L105-L135`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketConfig.cs#L105-L135)). The library's own FAQ warns that `GatewayIntents.All` "_can_ cause a memory leak from presence" ([client-basics FAQ](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/docs/faq/basics/client-basics.md#L33-L40)).

`MessageCacheSize = 0` means that, out of the box, every `Cacheable<IMessage, ulong>` in every message event arrives with `HasValue == false`. When enabled it is a per-channel FIFO ring ([`MessageCache.cs#L9-L43`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/MessageCache.cs#L9-L43)). This is the root of recurring reports like [#2661 "The MessageDeleted event does not return a message"](https://github.com/discord-net/Discord.Net/issues/2661).

### 2.4 Other well-known confusions

**`ModifyAsync` does not update the socket entity.** The socket overload discards the returned model; the REST one applies it:

```csharp
// SocketUserMessage
public Task ModifyAsync(Action<MessageProperties> func, RequestOptions options = null)
    => MessageHelper.ModifyAsync(this, Discord, func, options);

// RestUserMessage
public async Task ModifyAsync(Action<MessageProperties> func, RequestOptions options = null)
{
    var model = await MessageHelper.ModifyAsync(this, Discord, func, options).ConfigureAwait(false);
    Update(model);
}
```

([`SocketUserMessage.cs#L242-L243`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketUserMessage.cs#L242-L243), [`RestUserMessage.cs#L192-L196`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.Rest/Entities/Messages/RestUserMessage.cs#L192-L196))

After awaiting the socket version your object still shows the old content until `MESSAGE_UPDATE` arrives and the gateway handler mutates it — which only happens if the message is in the message cache.

**`IMessage.Channel` narrows to `ISocketMessageChannel` on socket entities**, so `IMessage.Channel` and `SocketMessage.Channel` are different static types on the same object, and neither is guaranteed to be a guild channel:

```csharp
public ISocketMessageChannel Channel { get; }
...
IUser IMessage.Author => Author;
IMessageChannel IMessage.Channel => Channel;
```

([`SocketMessage.cs#L29-L36`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketMessage.cs#L29-L36), [`#L305-L307`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketMessage.cs#L305-L307)). The official guides have an entire page on casting as a required skill ([casting guide](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/docs/guides/entities/casting.md), [basic-operations FAQ](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/docs/faq/basics/basic-operations.md#L40-L63)).

**"before" values are shallow clones of the live object.** `MESSAGE_UPDATE` does `before = cachedMsg.Clone(); cachedMsg.Update(State, data); after = cachedMsg;` ([`DiscordSocketClient.EventHandling.cs#L918-L926`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketClient.EventHandling.cs#L918-L926)) where `Clone()` is `MemberwiseClone()` ([`SocketMessage.cs#L300`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/Entities/Messages/SocketMessage.cs#L300)). `after` is the same object the cache holds, so it keeps mutating under the handler; `before` shares every reference field with it.

**Events are silently dropped for unsynced guilds.** `MESSAGE_CREATE` and `MESSAGE_UPDATE` both `return` early if `guild != null && !guild.IsSynced` ([`#L847-L852`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketClient.EventHandling.cs#L847-L852), [`#L911-L916`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketClient.EventHandling.cs#L911-L916)).

**Author id 0.** An edit whose payload omits the author produces `new SocketUnknownUser(this, id: 0)` ([`#L959-L961`](https://github.com/discord-net/Discord.Net/blob/d34a50eeabb39699a2329fbdac735b7d2b069824/src/Discord.Net.WebSocket/DiscordSocketClient.EventHandling.cs#L959-L961)) — a valid-looking `IUser` with an impossible snowflake.

## Lessons for Dukecord

**Copy from Catnip.**

1. **Normalized, per-guild stores; scalar-only guild snapshots.** Catnip's `GuildImpl` holds no children, and `Guild#members()` is a `default` method reading `cache().members(id)`. This is the single best decision in the library and it composes perfectly with Java records: a `GUILD_UPDATE` allocates one small record instead of rebuilding a member map. Confirms the direction already set in `docs/discord-api-caching.md`.
2. **Tiny composable "partial" interfaces.** `Snowflake`, `HasGuild`, `HasChannel`, `HasName`, `Mentionable` gave Catnip uniform navigation for free and gave users precise generic bounds. Java 25 sealed interfaces over records make this strictly better than Catnip's version.
3. **Separate read and write cache interfaces.** `EntityCache` for consumers, `EntityCacheWorker` for the gateway. Non-negotiable; do it on day one, not after the fact.
4. **`canProvidePreviousState(CachedEntityState)`.** Let the cache declare per-resource-kind whether it can produce old values, instead of pretending it always can. Generalize this into a small capability/completeness descriptor the caller can query.
5. **`CacheView` as a live, non-copying, read-only window.** Keep the idea, but return a genuine immutable snapshot list for iteration so a handler cannot observe mid-iteration mutation.
6. **Request bodies are their own types.** `MessageOptions`, `GuildData`, `ChannelData` are never entities. Dukecord already has `MessageCreateBuilder`/`MessageEditBuilder`; keep entities out of the request path entirely.
7. **Raw escape hatches.** Every REST method has a `...Raw` sibling returning the untouched JSON. Cheap to provide, and it means a missing Discord field never blocks a user.

**Do not copy from Catnip.**

8. **Do not merge partial payloads by synthesizing a fake gateway object.** `GUILD_MEMBER_UPDATE` in `MemoryEntityCache` drops `premium_since` and `avatar` on every call precisely because the merge is "build a JSON blob and re-parse". Write `MemberSnapshot merge(MemberSnapshot old, MemberUpdate patch)` as a total function over an exhaustive field list; a record's canonical constructor and `with`-style copy make omissions a compile error, not a silent data loss.
9. **Never dispatch an event before the cache is consistent.** `FIRE_BEFORE_CACHING` makes the cache disagree with the event for the duration of a handler, selected by a private static `Set`, with two `TODO`s and a `blockingSubscribe()` attached. Update the store first, then dispatch an event carrying `Optional<T> previous` and `T current` explicitly. Nothing in a handler should ever need to race the cache to read an old value.
10. **Never let entity construction mutate global state.** `EntityBuilder#createMember` writes to the user cache and guesses the shard from `guildId >> 22`. Parsing produces values; the cache reducer decides what to store.
11. **Do not define `equals` by ID alone on snapshots.** Catnip's `MemberImpl.equals` ignores the guild and ignores every other field, so stale and fresh snapshots — and members of different guilds — compare equal. Records give you full structural equality for free; keep it, and put identity in a separate `MemberId(GuildId, UserId)` key type (Dukecord already has `DiscordEntityReference`, which is the right shape).
12. **Do not make cache misses NPEs.** `Member#roles()` NPEs on an uncached role while `orderedRoles()` twelve lines below handles it. `Optional`/`Result` at the boundary, uniformly.
13. **Do not ship a count that does not say what it counts.** `memberCount()` has been wrong in both directions across Catnip's history. Name it `knownMemberCount()` and expose Discord's own `approximate_member_count` separately, plus an explicit completeness state.

**Avoid from Discord.Net.**

14. **Do not split entities by provenance.** The `Socket*` / `Rest*` split means `GetUserAsync` returns `SocketGuildUser`, `RestGuildUser`, or `null` from one signature, and it has generated a decade of issues ([#890](https://github.com/discord-net/Discord.Net/issues/890), [#1816](https://github.com/discord-net/Discord.Net/issues/1816)). Dukecord should have **one** `Member` record regardless of whether it came from the gateway, the cache, or REST, with provenance as a field if anyone needs it — never as a type.
15. **Do not fabricate entities to satisfy a type.** `AddOrUpdateUser(UserModel)` inserts a `SocketGuildUser` with empty roles and a null `JoinedAt`, *and* increments `DownloadedMemberCount`, poisoning `HasAllMembers`. If you only have a `User` and a guild id, the value is a `User`, not a `Member`. Model partial data as a distinct type (`PartialMember`, or better, just `User` + `GuildId`) and never insert it into the member store.
16. **Pick one representation of absence.** Discord.Net uses `Cacheable<T,TId>`, `Optional<T>`, `null`, and sentinel ids in a single event callback. Dukecord should use `Optional<T>` for "not in cache" plus one explicit `fetch()` method, and nothing else.
17. **Keep the good half of `Cacheable`: `HasValue` + `Id` + an explicit download, never an implicit one.** But type the result concretely and never return a type whose only guarantee is a shared interface. A `CachedRef<Message>` with `Optional<Message> cached()`, `MessageId id()`, and `Message fetch()` (blocking, throwing) is the whole idea with none of the four-type-parameter ceremony — and for deleted entities, offer no `fetch()` at all rather than one documented to throw `NullReferenceException`.
18. **Make completeness a first-class value, not a derived heuristic.** `HasAllMembers => MemberCount <= DownloadedMemberCount` with the honest implementation commented out beside it is the cautionary tale. Track a per-guild, per-kind `Completeness { UNAVAILABLE, PARTIAL, COMPLETE }` set by the code that actually knows (chunking finished, intent missing, guild unavailable).
19. **Make `edit` return the new value.** `SocketUserMessage.ModifyAsync` throws away the REST response and leaves the caller waiting for a gateway event. In a blocking library, `message.edit(...)` should return the updated `Message` snapshot from the HTTP response — trivially better, and the reason the Socket/Rest split existed in the first place does not apply to us.
20. **Do not default a cache to "off" while events still advertise it.** `MessageCacheSize = 0` means every `Cacheable` message arrives empty out of the box, which is a perennial bug report. Either enable a small bounded message cache by default or make the event signature state plainly that no message is available.

**Blocking API translation.** Catnip's entire cache and navigation surface maps onto a blocking API with no design loss: `Maybe<T>` → `Optional<T>`, `Single<T>` → `T` (throwing `DiscordHttpException`), `Observable<T>` → `List<T>` or `Stream<T>`, `Completable` → `void`. Catnip reached for `blockingGet()` internally in the two places where the async surface fought the domain ([member name indexing](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/cache/MemoryEntityCache.java#L113-L121), [ordered cache/emit](https://github.com/mewna/catnip/blob/650ea162ac0c51ae055c7ead9b765ab138467ffc/src/main/java/com/mewna/catnip/shard/buffer/CachingBuffer.java#L247-L258)). For a single-guild bot on Java 25 virtual threads, a synchronous cache read returning `Optional<Member>` is the right primitive and removes both hacks by construction.
