# Dukecord entity and cache model

Design date: 2026-09-15, revised the same day after the Imperium translation exercise ([`api-shape-imperium.md`](api-shape-imperium.md)). This is the contract for the `model`, `cache`, `gateway` (typed events), `rest` and `interaction` packages. Implementations must follow it; when it is silent, follow the reducer rules in [`discord-gateway-and-rest-payloads.md`](discord-gateway-and-rest-payloads.md) and the lessons in [`catnip-and-discordnet-entity-model.md`](catnip-and-discordnet-entity-model.md).

## 1. Why the current implementation is wrong

The interfaces in `model/` never said where data comes from, so the generated `*Impl` classes guessed: every lookup is a REST call.

- `GuildImpl.findRoleById` downloads every role of the guild for each call, and `MemberImpl.getRoles()` builds one such reference per role.
- `DiscordEntityReferenceImpl.get()` performs network I/O from a `Supplier` and throws `NoSuchElementException` on a 404. Hidden I/O in a getter is the exact Discord.Net trap we want to avoid.
- `MessageImpl.getMember()` prefers a REST round trip and only falls back to the `member` object that was already in the payload.
- Entities wrap a mutable `JsonObject` and re-parse on every access. Nothing is ever cached, so `GUILD_CREATE`, which hands us the whole guild for free, is thrown away.

The fix is not better implementations of these interfaces. It is a model that makes the data source part of the type: snapshots for state, references for links, and two clearly named verbs for cache reads and REST fetches.

## 2. The rules

1. **Entities are immutable snapshots.** An entity never changes after construction. The cache replaces whole values; it never patches them. Two snapshots of the same Discord object are equal if and only if their data is equal. There is exactly one Java type per Discord object regardless of whether it came from the gateway, the cache or REST. No `Socket*`/`Rest*` split, no synthetic entities.
2. **Object if inline, reference if id.** If Discord sent the object in the payload, the entity exposes it directly (`message.author()` is a `User`, `message.member()` is an `Optional<Member>`). If Discord sent only an id, the entity exposes a `DiscordEntityRef<T>` (`message.channel()`, `member.guild()`) or a `DiscordEntityRefList<T>` (`member.roles()`). A reference is `id()` plus a cache read `get()` plus a REST `fetch()`. There are no bare `xxxId()` accessors next to a reference.
3. **Two verbs, never mixed.** A cache read is a noun: `guild(id)`, `member(guildId, userId)`, `ref.get()`. It returns `Optional<T>` and never does I/O. A REST call is a verb: `fetchGuild(id)`, `sendMessage(...)`, `addRole(...)`, `ref.fetch()`. It blocks, returns `T` or `void`, and throws `DiscordRestException`. A method name that does not start with a verb never touches the network.
4. **Only the gateway writes the cache.** REST responses are returned to the caller and are not inserted into the cache. This avoids the stale-REST-overwrites-fresh-gateway race by construction. The cache converges when the corresponding gateway event arrives, usually milliseconds later. Consequence: a bot that creates something over REST and immediately scans the cache will not see it; keep the returned value, or wait for the event with `duke.events().await(...)` (section 6).
5. **Commit before dispatch.** The gateway thread applies an event to the cache, then dispatches the typed event. When a handler runs, the cache reflects at least that event. Events carry the exact snapshots (`previous`, `current`) so handlers never need to race the cache for an old value.
6. **Completeness and availability are data, not heuristics.** A guild knows whether it is available and whether its known member set is `NONE`, `PARTIAL` or `COMPLETE`. The code that knows sets it (ready, chunking finished, intent missing). Counts say what they count: `guild.memberCount()` is Discord's number from `GUILD_CREATE`; `guild.members().size()` is how many we know.
7. **Absence has one representation.** `Optional<T>` means "not present" (not in cache, field omitted, or nullable field null). Tri-state fields where absent and null mean different things (`referenced_message`, `nick` on member update, every field of an edit spec) use the sealed `Possible<T>` type (`Absent`, `Null`, `Present(T)`, named after Discord4J's `Possible`, but folding the nullable case in rather than nesting `Possible<Optional<T>>`) or are handled inside the reducer; they never leak as `Optional<Optional<T>>`.
8. **Unknown is not an error.** Unknown channel types become `UnknownChannel`, unknown events become `UnknownEvent`, unknown components become `UnknownComponent`, unknown JSON fields are preserved in the raw object. A Discord change must not crash a bot.

## 3. Entity representation

### 3.1 Shape

Every entity is a `final` class implementing its public interface, holding:

- the `Dukecord` back-reference (`duke()`), excluded from `equals`/`hashCode`/`toString`;
- the frozen raw JSON it was parsed from (`raw()`), an immutable tree of `Map`/`List`/scalars;
- eagerly parsed fields for everything the interface exposes.

Parsing is eager so that a malformed payload fails inside the reducer (logged, event dropped) rather than inside a user handler. `equals` compares the raw tree: this is structural equality for free and is stable across library versions.

```java
public interface DiscordEntity {
    Dukecord duke();
    Snowflake id();
    Json raw();      // immutable view of the source object, the escape hatch for fields we do not model
}
```

`Json` is a small immutable wrapper produced by freezing nanojson output recursively (`Map.copyOf`/`List.copyOf`, ~50 lines). It replaces the mutable `JsonObject` in every public signature, including `UnknownEvent`. The `Impl` classes are package-private; only interfaces are public.

### 3.2 Naming

One rule set, applied everywhere so that hand-written interfaces, records and builders read the same:

- **Read**: `name()`, `channel()`, `joinedAt()`. Booleans read as `isBot()`, `isJoined()`, `hasRole(id)`.
- **Build**: `MessageCreateSpec.builder().content("x").embed(e).build()`. Builder setters carry no prefix, like `HttpRequest.Builder`.
- **Copy**: `withX(value)` on immutable values only (`button.withDisabled(true)`), marking "returns a modified copy".
- **Act**: verbs for anything that blocks (`send`, `edit`, `fetch`, `addRole`, `join`).

Request bodies end in `Spec` (`MessageCreateSpec`), so they never collide with events (`MessageCreateEvent`). Query objects (`MessageQuery`, `MemberQuery`) are not bodies and carry no suffix.

### 3.3 References

```java
public interface DiscordEntityRef<T extends DiscordEntity> {
    Snowflake id();
    Optional<T> get();     // cache only, never I/O
    T fetch();             // REST, blocking, throws DiscordNotFoundException or DiscordRestException
}

public interface DiscordEntityRefList<T extends DiscordEntity> {
    SequencedSet<Snowflake> ids();
    boolean contains(Snowflake id);
    List<DiscordEntityRef<T>> refs();
    List<T> get();         // cached ones in id order, misses omitted
    List<T> fetch();       // REST; one request where Discord allows it (roles), one per id otherwise
}
```

A reference captures its route context at creation: a `DiscordEntityRef<Member>` remembers its guild id, a `DiscordEntityRef<Message>` its channel id, so `fetch()` always knows where to go. References are equal by target type and id. `get()` reads the live cache at call time, exactly like `duke.cache()`. Deleted things never come as references: a `MessageDeleteEvent` carries plain ids and the cached `previous` snapshot, because a `fetch()` on it is guaranteed to fail.

### 3.4 Why not Immutables or records for entities

Immutables would give `withX()` copies, but the reducer never needs to copy-with; it merges raw JSON and re-parses (section 5.3). Records would force the client back-reference into equality and offer no benefit over a final class here. Request specs (section 7.2) are JSON-backed values too, so the Immutables dependency can be dropped from the build. Records are used for events (section 6) and for plain values (`Snowflake`, `Embed`, `Attachment`, components).

### 3.5 Catalogue

Field lists are the minimum for the three target bots (`discord-api-feature-set.md`) plus what the Imperium translation needed. Everything else stays reachable through `raw()` until it is needed. "cache" marks a default method that reads the cache; "REST" marks a blocking call.

**`User`**: `username()`, `Optional<String> globalName()`, `Optional<String> avatarHash()`, `isBot()` (absent means false), `isSystem()`, `displayName()`, `mention()`. REST: `DmChannel openDm()`.

**`Guild`**: `name()`, `DiscordEntityRef<Member> owner()`, `Optional<String> description()`, `Optional<String> iconHash()`, `Set<String> features()`, `isAvailable()`, `Optional<Integer> memberCount()`, `Optional<Integer> boostCount()`, `MemberCompleteness memberCompleteness()`.
cache: `Optional<Member> member(userId)`, `List<Member> members()`, `Member selfMember()`, `Optional<Role> role(id)`, `Role everyoneRole()`, `List<Role> roles()` (by position, highest first), `Optional<GuildChannel> channel(id)`, `<C extends GuildChannel> Optional<C> channel(id, Class<C>)`, `List<GuildChannel> channels()`, `List<ThreadChannel> threads()`.
computed: `PermissionSet permissionsOf(Member)`, `PermissionSet permissionsOf(Member, GuildChannel)` (the eight-step algorithm; threads resolve through their parent).
REST: `Member fetchMember(userId)`, `List<Member> fetchMembers(MemberQuery)`, `GuildTextChannel createTextChannel(ChannelCreateSpec)`, `CategoryChannel createCategory(ChannelCreateSpec)`, `List<ThreadChannel> fetchActiveThreads()`.

**`Member`**: `DiscordEntityRef<Guild> guild()`, `User user()`, `Optional<String> nickname()`, `DiscordEntityRefList<Role> roles()`, `hasRole(id)`, `Optional<Instant> joinedAt()`, `Optional<Instant> boostingSince()`, `Optional<Instant> timeoutUntil()`, `isPending()`, `EnumSet<MemberFlag> flags()`, `Optional<PermissionSet> permissions()` (present only when the member came from an interaction), `displayName()`, `mention()`. `id()` is the user id; the cache key is `(guildId, userId)`.
cache: `Optional<Role> highestRole()`.
REST: `void addRole(roleId)`, `void addRole(roleId, String reason)`, `void removeRole(...)` (same two forms), `Member edit(MemberEditSpec)`, `void kick(String reason)`.

**`Role`**: `DiscordEntityRef<Guild> guild()`, `name()`, `int color()`, `int position()`, `PermissionSet permissions()`, `isManaged()`, `isMentionable()`, `isEveryone()` (id equals guild id), `mention()`.

**`Channel`** is sealed:

```
Channel
├── GuildChannel            guild(), name(), int position(), Optional<DiscordEntityRef<CategoryChannel>> category(),
│   │                       isInCategory(id), List<PermissionOverwrite> overwrites(), isObfuscated()
│   ├── CategoryChannel     cache: List<GuildChannel> channels(), Optional<GuildChannel> channel(String name)
│   ├── GuildTextChannel    implements MessageChannel; topic(), isNsfw(), rateLimitPerUser()
│   ├── GuildVoiceChannel   (later)
│   ├── ForumChannel        REST: ThreadChannel startThread(ThreadCreateSpec) with a starter message   (later)
│   ├── ThreadChannel       implements MessageChannel; DiscordEntityRef<GuildChannel> parent(), DiscordEntityRef<Member> owner(),
│   │                       ThreadMetadata metadata(), isJoined(); REST: void join(), void leave()
│   └── UnknownGuildChannel
├── DmChannel               implements MessageChannel; List<User> recipients()
└── UnknownChannel
```

`MessageChannel` is a mixin interface: `Optional<DiscordEntityRef<Message>> lastMessage()`; REST `Message send(MessageCreateSpec)`, `Message send(String)`, `Message fetchMessage(id)`, `List<Message> fetchMessages(MessageQuery)`, `void bulkDelete(Collection<Snowflake>)`, `ThreadChannel createThread(ThreadCreateSpec)`, `void typing()`.
`GuildChannel` computed: `PermissionSet permissionsOf(Member)`.

`isJoined()` on a thread is tracked by the cache: `member` present on a `GUILD_CREATE` or REST thread object, the `members` array of `THREAD_LIST_SYNC`, `THREAD_MEMBER_UPDATE`, and the bot's id in `THREAD_MEMBERS_UPDATE.added_members` or `removed_member_ids`. It is a real boolean, not an `Optional`.

The `Channels.fromJson` factory switches on `type` and produces the `Unknown*` variants for unmapped values; it never throws on an unknown type. `isObfuscated()` reads `flags & (1 << 17)`; from 2026-11-16 obfuscated channels carry fake names.

**`Message`**: `DiscordEntityRef<MessageChannel> channel()`, `Optional<DiscordEntityRef<Guild>> guild()`, `User author()`, `isFromWebhook()`, `Optional<Member> member()` (inline payload member merged with `author`, never a cache lookup), `content()`, `strippedContent()` (mentions resolved through the cache, markdown escaped; no I/O), `Instant timestamp()`, `Optional<Instant> editedAt()`, `MessageType type()`, `EnumSet<MessageFlag> flags()`, `List<User> mentionedUsers()`, `DiscordEntityRefList<Role> mentionedRoles()`, `mentionsEveryone()`, `List<Attachment> attachments()`, `List<Embed> embeds()`, `List<Component> components()`, `Optional<MessageReference> reference()`, `ReferencedMessage referencedMessage()` (sealed: `Unknown`, `Deleted`, `Present(Message)`), `isPinned()`, `jumpUrl()`.
REST: `Message reply(MessageCreateSpec)`, `Message reply(String)`, `Message edit(MessageEditSpec)`, `void delete()`, `void delete(String reason)`, `void disableComponents()`, `ThreadChannel createThread(ThreadCreateSpec)` (message-anchored route), `void addReaction(Emoji)`, `void pin()`.

Content-gated fields (`content`, `embeds`, `attachments`, `components`) arrive empty without `MESSAGE_CONTENT`. `Dukecord.intents()` exposes the configured set so a bot can tell "empty" from "hidden".

**Values** are records with no client back-reference: `Attachment` (`id`, `filename`, `size`, `url`, `contentType`; download through `duke.rest().download(attachment)`), `Embed` (with `Embed.builder()`, `Embed.builder(Embed)` copy, `Optional<String> field(String name)`; builder: `color`, `title`, `url`, `description`, `field(name, value[, inline])`, `blankField()`, `author`, `footer`, `image`, `thumbnail`, `timestamp`), `Component` (sealed: `ActionRow`, `Button`, `StringSelect`, `TextDisplay` and the other v2 layouts, `UnknownComponent`; every component has `asDisabled()`), `AllowedMentions` (`none()`, `all()`, `users(ids)`, `roles(ids)`, `withEveryone()`, `withRepliedUser(boolean)`), `FileUpload` (`of(Path)`, `of(Path, filename)`, `of(byte[], filename)`, `ofStream(filename, Supplier<InputStream>)`, `withDescription`), `PermissionOverwrite`, `ThreadMetadata`, `MessageReference`, `Emoji`.

**`Snowflake`**: keep the record. Add `static Snowflake of(String)`, `String asString()` (unsigned), `static Snowflake fromInstant(Instant)` for pagination cursors, `Comparable`. Use `asString()` everywhere the current code has `Long.toUnsignedString(x.value())`.

**`PermissionSet`** (rename from `DiscordPermissionSet`): keep `long` for now (Discord is below bit 60), parse from the string form, provide `contains`, `containsAll`, `with`, `without`, `ALL`, `NONE`, `EnumSet<Permission> asSet()`. `Permission` (rename from `DiscordPermission`) gets the full bit table from the permissions doc.

## 4. The cache

### 4.1 Stores

The cache is normalized. A guild snapshot holds only guild scalars; children live in their own stores keyed by id and carrying their `guildId`.

| Store | Key | Value | Populated from | Bound |
|---|---|---|---|---|
| `guilds` | guild id | `GuildState { Guild snapshot, boolean available, MemberCompleteness }` | READY (unavailable stubs), GUILD_CREATE/UPDATE/DELETE | none |
| `channels` | channel id | `Channel` (guild channels, threads with joined flag, DMs) | GUILD_CREATE, CHANNEL_*, THREAD_*, THREAD_LIST_SYNC, THREAD_MEMBER(S)_UPDATE | none |
| `roles` | role id | `Role` | GUILD_CREATE, GUILD_UPDATE (wholesale), GUILD_ROLE_* | none |
| `members` | `(guildId, userId)` | `Member` | GUILD_CREATE, GUILD_MEMBER_*, GUILD_MEMBERS_CHUNK, plus every event that carries an inline member | none |
| `users` | user id | `User` | every full user object that is not a webhook author | evicted with the last member that references it |
| `messages` | `(channelId, messageId)` | `Message` | MESSAGE_CREATE, MESSAGE_UPDATE (when reconstructable) | global FIFO, default 1000, `0` disables |
| `self` | – | `User` | READY, USER_UPDATE | – |

Per-guild lists (`guild.channels()`, `guild.members()`, `category.channels()`) filter the store by `guildId` or `parentId`. For a few guilds with a few thousand members a scan is microseconds and needs no index; add a `guildId -> ids` index only if profiling shows it.

Presences and voice states are not cached in v1. The intent is still configurable, and the raw events are still dispatched as `UnknownEvent`.

### 4.2 Read API

```java
public interface DiscordCache {
    Optional<Guild> guild(Snowflake id);
    List<Guild> guilds();
    Optional<Channel> channel(Snowflake id);
    <C extends Channel> Optional<C> channel(Snowflake id, Class<C> type);
    Optional<Role> role(Snowflake id);
    Optional<Member> member(Snowflake guildId, Snowflake userId);
    Optional<User> user(Snowflake id);
    Optional<Message> message(Snowflake channelId, Snowflake messageId);
    List<GuildChannel> channels(Snowflake guildId);
    List<Role> roles(Snowflake guildId);
    List<Member> members(Snowflake guildId);
}
```

Every return value is an immutable snapshot or an immutable list copy. There is no live view. `Dukecord` exposes `cache()` and forwards the common ones: `duke.guild(id)`, `duke.requireGuild(id)` (throws `IllegalStateException` saying whether the guild is unknown or unavailable), and `duke.requireSingleGuild()` for the single-guild case, which throws when the bot is in zero or more than one guild.

### 4.3 Configuration

```java
public record DukecordOptions(
    GatewayIntentSet intents,             // no default; the user's call
    int messageCacheSize,                 // default 1000; 0 disables the store, MessageUpdateEvent.previous is then always empty
    boolean chunkMembersOnStartup,        // default true; ignored without GUILD_MEMBERS; login() waits for the chunks
    boolean autoJoinThreads,              // default false; see below
    Duration readyTimeout,                // default 60 s
    Duration shutdownTimeout,             // default 15 s, used by close()
    List<ListenerRegistration> listeners  // registered before the gateway connects, so no event is missed
)
```

`autoJoinThreads` means: join every thread that enters the cache un-joined, whatever the source (`GUILD_CREATE` active threads, `THREAD_CREATE` including the not-newly-created "you gained access" form, `THREAD_LIST_SYNC`, `THREAD_UPDATE` un-archive). Threads created by the bot are already joined and are skipped. This replaces the three workaround listeners Imperium carries.

Member and user caching has no policy knob. A single-guild bot wants every member it can see; the store is bounded by the guild size and by what Discord sends.

## 5. Write path

### 5.1 Threading

The gateway reader thread parses the frame, calls `CacheWorker.apply(type, json)`, and hands the resulting typed event to `DiscordEventDispatcher`. `apply` is the only writer and is never called concurrently; stores are `ConcurrentHashMap`s so readers never block. Multi-store updates are not transactional; the worker installs children before parents (channels, roles, members, then the guild snapshot) and removes parents before children, so a reader that can see a guild can see its children.

Listeners run on virtual threads, one per event. Between the commit and the moment a handler reads the cache, later events may have committed. Guarantee: the cache is at or after the event; the event's own snapshots are exact. Two events for the same message (two fast button clicks) run their handlers concurrently; a bot that keeps per-message state must protect it (`ConcurrentHashMap.compute`) or serialise itself. Dukecord does not order dispatch by channel or message in v1; if that proves necessary, a sequential dispatch option is a small addition.

### 5.2 Event reducers

Each row is the whole contract for that event. Rule numbers refer to the "Implications for a normalized cache" list in [`discord-gateway-and-rest-payloads.md`](discord-gateway-and-rest-payloads.md).

| Gateway event | Cache effect | Dispatched event |
|---|---|---|
| READY | set `self`; insert each `guilds[i]` as unavailable `GuildState` (rule 7) | `ReadyEvent(User self, List<Snowflake> guildIds)` |
| RESUMED | nothing (rule 29) | `ResumedEvent` |
| GUILD_CREATE | freeze; upsert roles, channels (backfill `guild_id`, rule 10), threads (joined when `member` present), members (and their users); build scalar `Guild` keeping `member_count`, `large`, `joined_at`; completeness = `COMPLETE` if `!large` and GUILD_MEMBERS intent, else `PARTIAL`; mark available; auto-join if configured | `GuildAvailableEvent(Guild)` if it was an unavailable stub, else `GuildJoinEvent(Guild)` |
| GUILD_UPDATE | merge raw over cached guild raw; replace the guild's role set wholesale from `roles` (delete missing ones, rule 6); do not touch channels, members, threads | `GuildUpdateEvent(Guild previous, Guild current)` |
| GUILD_DELETE | `unavailable: true`: keep data, `available = false`; absent: remove guild and cascade to channels, roles, members, messages of its channels, orphaned users (rule 7) | `GuildUnavailableEvent(Guild)` or `GuildLeaveEvent(Guild)` |
| CHANNEL_CREATE / UPDATE / DELETE | upsert / replace / remove (also drop that channel's messages) | `ChannelCreateEvent(Channel)`, `ChannelUpdateEvent(Optional<Channel> previous, Channel current)`, `ChannelDeleteEvent(Channel)` |
| CHANNEL_PINS_UPDATE | update `last_pin_timestamp` on the cached channel raw (rule 17) | `ChannelPinsUpdateEvent(channelId, Optional<Instant>)` |
| THREAD_CREATE | upsert, preserving a known joined flag; auto-join if configured | `ThreadCreateEvent(ThreadChannel, boolean newlyCreated)` |
| THREAD_UPDATE / DELETE | replace (preserving joined) / remove by id (delete payload has ids only, rule 13); auto-join on un-archive if configured | `ThreadUpdateEvent(Optional<ThreadChannel> previous, ThreadChannel current)`, `ThreadDeleteEvent(Snowflake id, Snowflake parentId, Optional<ThreadChannel> previous)` |
| THREAD_LIST_SYNC | remove active threads whose `parentId` is in `channel_ids` (or all if omitted), insert `threads`, mark joined from `members` (rule 12); auto-join if configured | `ThreadListSyncEvent(List<ThreadChannel>)` |
| THREAD_MEMBER_UPDATE / THREAD_MEMBERS_UPDATE | set joined on the thread from the bot's own id; harvest `added_members[].member` into `members` (rule 15) | `ThreadMembersUpdateEvent(...)` |
| GUILD_MEMBER_ADD | upsert member and user (idempotent, rule 28) | `MemberJoinEvent(Member)` |
| GUILD_MEMBER_UPDATE | merge raw over cached member raw (absent = unchanged, null = cleared, rules 3 and 5); upsert user; if not cached, store the payload as a member only if it parses completely, otherwise store nothing | `MemberUpdateEvent(Optional<Member> previous, Member current)` |
| GUILD_MEMBER_REMOVE | remove member; evict user if no member references it | `MemberLeaveEvent(Snowflake guildId, User user, Optional<Member> previous)` |
| GUILD_MEMBERS_CHUNK | upsert members and users; when `chunk_index == chunk_count - 1` for the startup nonce, set completeness `COMPLETE` and release `login()` | `MembersChunkEvent(guildId, List<Member>, int index, int count)` |
| GUILD_ROLE_CREATE / UPDATE / DELETE | upsert / replace / remove; on delete strip the id from every member of the guild (rule 26) | `RoleCreateEvent`, `RoleUpdateEvent(Optional<Role> previous, Role current)`, `RoleDeleteEvent(Snowflake guildId, Snowflake id, Optional<Role> previous)` |
| MESSAGE_CREATE | parse; `member` merged with `author` (rule 1); skip user/member upsert when `webhook_id` is set (rule 2); upsert author user and inline member; insert into `messages` (FIFO); set `last_message_id` on the cached channel raw (rule 16) | `MessageCreateEvent(Message)` |
| MESSAGE_UPDATE | if cached: merge raw over cached raw, replace; else parse the payload alone; if that fails, do not store (rule 20) | `MessageUpdateEvent(Snowflake channelId, Snowflake id, Optional<Message> previous, Optional<Message> current, Json patch)` |
| MESSAGE_DELETE / DELETE_BULK | remove | `MessageDeleteEvent(channelId, id, Optional<Message> previous)`, `MessageBulkDeleteEvent(channelId, List<Snowflake> ids, List<Message> previous)` |
| MESSAGE_REACTION_ADD / REMOVE / REMOVE_ALL / REMOVE_EMOJI | on add, upsert inline member (rule 21); no message mutation | `ReactionAddEvent(channelId, messageId, userId, Optional<Member>, Emoji)`, `ReactionRemoveEvent(...)`, ... |
| INTERACTION_CREATE | upsert `member` merged with `user` (the only source of `member.permissions`, rule 25; do not store resolved partials) | one of the `InteractionEvent` records (section 7.4) |
| USER_UPDATE | replace `self` | `SelfUpdateEvent(User previous, User current)` |
| PRESENCE_UPDATE, VOICE_STATE_UPDATE, TYPING_START | v1: only harvest `member` when present and complete (rule 24) | `UnknownEvent` |
| anything else | nothing | `UnknownEvent(String type, Json payload)` |

A reducer that throws logs the payload at `DEBUG` and the exception at `ERROR`, and the event is dropped. It must not take the gateway down.

### 5.3 Merge semantics

Discord partial updates are the same object with keys omitted. The merge is therefore a shallow key overlay of the update's raw JSON on the cached raw JSON, followed by a fresh parse:

```java
static Json merge(Json cached, Json update) {
    var map = new LinkedHashMap<>(cached.asMap());
    map.putAll(update.asMap());      // present-and-null overwrites with null (cleared); absent keeps cached
    return Json.of(map);
}
```

This is total over the field list by construction (nothing to forget, unlike Catnip's re-synthesized member), preserves `GUILD_CREATE`-only fields across `GUILD_UPDATE`, replaces arrays wholesale (which is what `roles` on `GUILD_UPDATE` requires), and keeps unknown fields. Documented exceptions handled before the overlay: `MESSAGE_CREATE.member` gets `user` filled from `author`; `GUILD_MEMBER_UPDATE` restores `flags` and `permissions` from the cached copy if missing; thread updates carry the joined flag over since it is not part of the payload.

### 5.4 Startup and shutdown

`Dukecord.login(token, options)` blocks until: READY received, every guild listed in READY has delivered its GUILD_CREATE (or `readyTimeout` elapsed, in which case the remaining guilds stay unavailable and login still succeeds with a warning), and, when `chunkMembersOnStartup` applies, every chunk has arrived. Listeners passed through `DukecordOptions.listeners` are active from the first frame, so events dispatched during login are not lost. After `login()` returns, a single-guild bot can call `duke.requireSingleGuild()` and trust `channels()`, `roles()` and `members()`.

`close()` sends a clean websocket close, waits up to `shutdownTimeout` for in-flight handlers, then interrupts them. `close(Duration)` overrides the timeout for one call.

## 6. Events

`DiscordEvent` is a sealed interface over the records listed in 5.2 and 7.4. Every record is public, immutable, and carries snapshots rather than ids where a snapshot exists. `previous` is `Optional` wherever the cache may legitimately not have it (message cache disabled, entity created before the bot connected). Deleted entities are carried as ids plus the cached `previous` snapshot, never as references.

`DiscordEventSource` keeps `register(Class<E>, listener)` and adds `<E extends DiscordEvent> E await(Class<E>, Predicate<E>, Duration)`, the blessed idiom for "wait until my REST write has landed in the cache":

```java
var channel = category.guild().fetch().createTextChannel(spec);
duke.events().await(ChannelCreateEvent.class, e -> e.channel().id().equals(channel.id()), Duration.ofSeconds(5));
```

## 7. REST

### 7.1 Facade

`Dukecord.rest()` returns `DiscordRest`, a blocking, typed facade over `DiscordHttpClient`. Every method maps to one route, returns entities built with the same factories as the gateway path, and never writes to the cache. Entity convenience verbs (`message.reply`, `member.addRole`, `ref.fetch()`) delegate here. Audit-log reasons are plain `String` overloads; no method takes an `Optional` parameter.

```java
public interface DiscordRest {
    User fetchSelf();
    User fetchUser(Snowflake id);
    Guild fetchGuild(Snowflake id);
    Channel fetchChannel(Snowflake id);
    Member fetchMember(Snowflake guildId, Snowflake userId);
    List<Member> fetchMembers(Snowflake guildId, MemberQuery query);
    List<Role> fetchRoles(Snowflake guildId);
    Message fetchMessage(Snowflake channelId, Snowflake id);
    List<Message> fetchMessages(Snowflake channelId, MessageQuery query);
    Message sendMessage(Snowflake channelId, MessageCreateSpec spec);
    Message editMessage(Snowflake channelId, Snowflake id, MessageEditSpec spec);
    void deleteMessage(Snowflake channelId, Snowflake id);
    void deleteMessage(Snowflake channelId, Snowflake id, String reason);
    void bulkDeleteMessages(Snowflake channelId, Collection<Snowflake> ids);
    ThreadChannel createThread(Snowflake channelId, ThreadCreateSpec spec);
    ThreadChannel createThread(Snowflake channelId, Snowflake messageId, ThreadCreateSpec spec);
    void joinThread(Snowflake threadId);
    void leaveThread(Snowflake threadId);
    GuildChannel createChannel(Snowflake guildId, ChannelCreateSpec spec);
    GuildTextChannel createTextChannel(Snowflake guildId, ChannelCreateSpec spec);
    CategoryChannel createCategory(Snowflake guildId, ChannelCreateSpec spec);
    Member editMember(Snowflake guildId, Snowflake userId, MemberEditSpec spec);
    void addMemberRole(Snowflake guildId, Snowflake userId, Snowflake roleId);
    void addMemberRole(Snowflake guildId, Snowflake userId, Snowflake roleId, String reason);
    void removeMemberRole(...);                   // same two forms
    DmChannel openDm(Snowflake userId);
    InputStream download(Attachment attachment);  // CDN, not rate limited like the API
    DiscordHttpClient http();                     // raw escape hatch, same as Catnip's ...Raw methods
}
```

A 404 throws `DiscordNotFoundException`, a 403 throws `DiscordForbiddenException`; both extend `DiscordRestException`. There is no third `find*` verb on the REST side; catch the exception when "does it exist" is the question.

### 7.2 Specs

Request bodies are immutable values, never entities and never bound to a target. `MessageCreateSpec` and `MessageEditSpec` hold a frozen `Json` payload plus a list of `FileUpload`s. A builder produces them and writes a key only when its method is called, so the payload itself carries Discord's tri-state: key absent means "not set" (Discord keeps the current value on PATCH), key present with `null` means "clear", key present with a value means "set". Reading a spec back (copying, tests) goes through `Possible<T>`.

```java
var body = MessageCreateSpec.builder()
        .content("Server started")
        .embed(embed)
        .file(FileUpload.of(path, "map.msav"))
        .build();
channel.send(body);
message.reply(body);            // consumer adds message_reference
event.reply(body);              // consumer wraps in {type: 4, data: body}
```

Builders:

- `MessageCreateSpec.builder()`: `content`, `embed`/`embeds`, `component`/`components`, `file`, `allowedMentions`, `flags`, `ephemeral()`, `suppressNotifications()`, `componentsV2()`, `tts`, `nonce`/`enforceNonce`, `reference(messageId, channelId, failIfNotExists)`, `stickers`. `MessageCreateSpec.builder(MessageCreateSpec)` copies.
- `MessageEditSpec.builder()`: `content`/`clearContent()`, `embeds`/`clearEmbeds()`, `components`/`clearComponents()`, `file`, `keepAttachments(ids)`, `removeAttachments()`, `allowedMentions`, `suppressEmbeds(boolean)`. The `attachments` key is written only by `keepAttachments`, `removeAttachments` or `file`: absent keeps every existing attachment, an empty array removes all, a list keeps exactly those ids plus the new uploads' placeholders. `MessageEditSpec.from(MessageCreateSpec)` is a key-level copy through the edit allow-list, for paginators that render one body and use it as a reply first and as an edit afterwards. `MessageEditSpec.disablingComponentsOf(Message)` builds the teardown edit.
- `ThreadCreateSpec.builder()`: `name`, `autoArchiveDuration`, `appliedTags`, `message(MessageCreateSpec)` for forum and media channels (rejected on the message-anchored route).
- `ChannelCreateSpec`, `MemberEditSpec` (`nick`/`clearNick()`, `roles(Collection<Snowflake>)`, `timeoutUntil`/`clearTimeout()`, `mute`, `deaf`, `voiceChannel`), `MemberQuery`, `MessageQuery` (`before`, `after`, `around`, `limit`): same pattern. Every spec builder has `reason(String)` for the audit log.

`MessageCreateSpec` writes `allowed_mentions` as "none" at construction; `MessageEditSpec` writes it at build time whenever `content` is set, because Discord re-parses mentions with default allowances on any content edit that omits the key, while an edit that leaves content alone must stay an empty payload. `allowedMentions(...)` overrides it in both. Builders validate nothing that Discord validates better; a bad combination surfaces as a `DiscordRestException` with Discord's error body.

**Consumers own the envelope, an allow-list and the response parser.** The spec stays dumb; the consumer decides what the route accepts, so a caller never sees a 400 for a key that was valid in another context. Keys outside the allow-list are dropped, logged at `DEBUG` with their names; `flags` is masked to the bits the route accepts.

| Consumer | Accepts | Envelope | Allow-list | Returns |
|---|---|---|---|---|
| `channel.send` | `MessageCreateSpec` | body as is | all create keys | `Message` |
| `message.reply` | `MessageCreateSpec` | body with `message_reference` set by the consumer | all create keys | `Message` |
| `message.edit`, `DeferredReply.edit`, `DeferredEdit.edit` | `MessageEditSpec` | body as is (edit-original route for the handles) | all edit keys | `Message` |
| `event.reply` (before acknowledgement) | `MessageCreateSpec` | `{type: 4, data: body}` | drops `nonce`, `enforce_nonce`, `tts`, `message_reference`, `sticker_ids` | `void` |
| `DeferredReply.send` | `MessageCreateSpec` | PATCH edit-original with the core keys | same drop list | `Message` |
| `event.followUp`, `DeferredReply.followUp` | `MessageCreateSpec` | body as is, webhook route | same drop list as reply | `Message` |
| `ComponentEvent.updateMessage` (before acknowledgement) | `MessageEditSpec` | `{type: 7, data: body}` | all edit keys | `void` |
| `event.replyModal` (before acknowledgement, never from a modal submit) | `ModalSpec` | `{type: 9, data: modal}` | modal keys | `void` |
| `forum.startThread` | `ThreadCreateSpec` with `message` | `{name, auto_archive_duration, applied_tags, message: body}` | create keys inside `message` | `ThreadChannel` |
| `message.createThread` | `ThreadCreateSpec` without `message` | `{name, auto_archive_duration}` on the message-anchored route | thread keys | `ThreadChannel` |
| webhook execute (later) | `MessageCreateSpec` | body plus `username`, `avatar_url`, `?thread_id` | create keys | `Message` or `void` |

Ephemerality is decided at acknowledgement: `deferReply(Ephemeral.YES)` fixes it for the whole interaction, and the `ephemeral()` builder flag is honoured only on `event.reply` and on follow-ups. The acknowledgement state lives in the handle types (section 7.4), not in a runtime flag, which is why `reply` and `DeferredReply.send` are different methods with different return types.

**Files.** Uploads live outside the JSON. The consumer builds the multipart request with the wrapped envelope as the `payload_json` part and the files as numbered `files[n]` parts; the JSON `attachments` array holds `{id: n, filename, description}` placeholders. One request carries one spec, so placeholder indexes are stable even when the body is nested under `data` or `message`.

### 7.3 Errors

`DiscordRestException extends RuntimeException` with `status`, Discord `code`, `message`, `Optional<Duration> retryAfter`, and the raw error body. `DiscordNotFoundException` (404) and `DiscordForbiddenException` (403) are subtypes. `DiscordErrorCode` holds the constants bots test against (`UNKNOWN_MESSAGE = 10008`, `CANNOT_SEND_TO_USER = 50007`, `MISSING_PERMISSIONS = 50013`, ...). The current checked `DiscordHttpException` stays at the `http` layer and is translated at the `rest` boundary. Blocking bot code must not be forced to catch `IOException` on every send.

### 7.4 Interactions

The interaction module is imperative only; annotation scanning can be a separate module later. Interaction events are ordinary `DiscordEvent` records, so `duke.events().register(SlashCommandEvent.class, ...)` works, and the registrar is the convenience layer for routing by command name and custom id.

```java
interface Dukecord { CommandRegistrar commands(); }

interface CommandRegistrar {
    CommandRegistrar slash(SlashCommandSpec spec, Consumer<SlashCommandEvent> handler);
    CommandRegistrar component(String customIdPrefix, Consumer<ComponentEvent> handler);
    CommandRegistrar modal(String customIdPrefix, Consumer<ModalSubmitEvent> handler);
    void publishGuild(Snowflake guildId);      // PUT guild commands, blocking; the development mode
    void publishGlobal();                      // PUT global commands, blocking
}

record SlashCommandSpec(String name, String description, List<OptionSpec> options, List<SlashCommandSpec> subcommands,
                        Optional<PermissionSet> defaultMemberPermissions) { static Builder builder(String name); }
record OptionSpec(String name, String description, OptionType type, boolean required, List<Choice> choices,
                  OptionalLong min, OptionalLong max) {}
enum OptionType { STRING, INTEGER, NUMBER, BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT }
enum Ephemeral { YES, NO }

sealed interface InteractionEvent extends DiscordEvent {
    Snowflake id();
    String token();
    User user();
    Optional<Member> member();                  // guild interactions; carries permissions()
    Optional<DiscordEntityRef<Guild>> guild();
    DiscordEntityRef<MessageChannel> channel();
    PermissionSet appPermissions();
    Instant expiresAt();                        // 15 minutes after the id's timestamp

    DeferredReply deferReply(Ephemeral ephemeral);   // callback type 5
    void reply(MessageCreateSpec spec);              // callback type 4, only before acknowledgement
    void reply(String content);
    void replyModal(ModalSpec modal);                // callback type 9, only before acknowledgement
    Message followUp(MessageCreateSpec spec);        // webhook route, valid after any acknowledgement
    Message fetchOriginal();                         // GET @original
}

sealed interface ComponentEvent extends InteractionEvent {
    String customId();
    Message message();                               // the message the component lives on
    DeferredEdit deferEdit();                        // callback type 6
    void updateMessage(MessageEditSpec spec);        // callback type 7, only before acknowledgement
}

record SlashCommandEvent(...) implements InteractionEvent {
    String commandPath();                            // "map search"
    <T> Optional<T> option(String name, Class<T> type);   // String, Long, Double, Boolean, User, Channel, Role, Attachment
}
record ButtonEvent(...) implements ComponentEvent {}
record StringSelectEvent(...) implements ComponentEvent { List<String> values(); }
record ModalSubmitEvent(...) implements InteractionEvent {
    String customId();
    Optional<String> value(String inputId);
    Optional<Message> message();                     // present when the modal came from a component
}

interface DeferredReply {                            // after callback type 5
    Message send(MessageCreateSpec spec);            // PATCH @original
    Message send(String content);
    Message edit(MessageEditSpec spec);              // PATCH @original again
    Message followUp(MessageCreateSpec spec);
    void delete();
}
interface DeferredEdit {                             // after callback type 6
    Message edit(MessageEditSpec spec);              // PATCH @original
    Message followUp(MessageCreateSpec spec);
}
```

Calling `reply`, `replyModal`, `updateMessage`, `deferReply` or `deferEdit` a second time on the same event throws `IllegalStateException` naming the earlier acknowledgement. Interaction tokens expire after fifteen minutes; the handles throw `DiscordRestException` with Discord's error after that, and `expiresAt()` lets a paginator schedule its own teardown.

Components and modals are values: `ActionRow.of(...)`, `Button.primary(customId, label)` and the other styles, `Button.link(url, label)`, `withDisabled`, `withEmoji`, `StringSelect.builder(customId)`, `SelectOption.of(label, value)`, `ModalSpec.builder(customId, title)`, `TextInput.shortLine(id, label)`, `TextInput.paragraph(id, label)`. Custom ids are free-form strings; the registrar matches on prefix so bots can encode state after a separator.

## 8. Threading and consistency summary

- Reads never block and never do I/O; `ref.get()` is a read.
- Writes come from one thread, in shard sequence order.
- Handlers run on virtual threads; ordering between two events is not guaranteed across handlers, only within the reducer.
- Every entity a handler receives is immutable and safe to keep or pass to another thread.
- REST calls block the calling thread; use virtual threads for anything that fans out.
- REST results are not cached; keep the returned value or `await` the event.

## 9. What bot code looks like

```java
try (var duke = Dukecord.login(token, DukecordOptions.builder()
        .intents(GatewayIntentSet.UN_PRIVILEGED.with(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS))
        .autoJoinThreads(true)
        .listener(MessageCreateEvent.class, ImperiumBridge::onMessage)
        .build())) {

    var guild = duke.requireSingleGuild();
    var bridge = guild.channel(BRIDGE_ID, GuildTextChannel.class).orElseThrow();
    bridge.send(MessageCreateSpec.builder().content("Server started").build());

    duke.commands()
        .slash(SlashCommandSpec.builder("sync-roles").description("Synchronize your roles").build(), event -> {
            var reply = event.deferReply(Ephemeral.YES);
            var member = event.member().orElseThrow();
            var wanted = rolesFor(member.user().id());                       // Set<Snowflake>
            if (!wanted.equals(member.roles().ids())) {
                member.edit(MemberEditSpec.builder().roles(wanted).reason("rank sync").build());
            }
            reply.send("Done.");
        })
        .publishGuild(guild.id());

    duke.events().register(MemberUpdateEvent.class, event -> {
        var before = event.previous().map(m -> m.roles().ids()).orElse(Set.of());
        var after = event.current().roles().ids();
        // diff before/after; the cache already holds `current`
    });
}
```

`guild` above is a snapshot taken at startup. Its scalars (`name()`, `owner().id()`) can go stale, but its navigation methods read the live cache, so `guild.channels()` and `guild.permissionsOf(...)` are always current. Re-read `duke.guild(id)` when current scalars matter, or subscribe to `GuildUpdateEvent`.

## 10. Implementation plan

Each phase is independently mergeable and testable without a Discord token. Feed recorded gateway JSON into the reducer in tests; do not test through HTTP unless the code under test is the HTTP layer.

1. **Foundations (`model`).** *Landed 2026-09-16: everything below except the `rest` implementation behind `ref.fetch()`, which is phase 4.* `Json` immutable tree and reader helpers (replaces `ModelSupport` parsing functions). `Snowflake` helpers. `Possible<T>`. Full `Permission` table and `PermissionSet`. Naming per 3.2. Replace `DiscordEntityReference` with `DiscordEntityRef` and `DiscordEntityRefList` (the `fetch()` side can throw `UnsupportedOperationException` until phase 4). Rewrite every entity as an eager-parsed final class with `duke()`, `raw()`, references for links and default cache navigation. Add the channel hierarchy, the value records (`Embed` with builder, `Attachment`, `AllowedMentions`, components with `asDisabled()`), and the `Unknown*` types. Tests: parse fixtures for each entity, equality, unknown type handling.
2. **Cache (`cache`).** `DiscordCache` (read) and package-private `CacheWorker` (write) with the store table and reducer table above; `GuildState`; thread joined tracking; message FIFO; merge helper. Tests: one fixture sequence per reducer row, asserting store contents and the emitted event including `previous`.
3. **Typed gateway.** `DiscordGatewayImpl` calls the worker before dispatching; sealed `DiscordEvent`; `Dukecord.login` with READY/GUILD_CREATE/chunk barrier and pre-registered listeners; REQUEST_GUILD_MEMBERS with nonce; `autoJoinThreads`; `await`; bounded `close`. Tests: fake websocket feeding frames, assert `login()` returns only after the barrier and that pre-login events reach listeners.
4. **REST facade.** `DiscordRest`, JSON-backed specs and their builders, consumer allow-lists, multipart uploads, downloads, `DiscordRestException` family and error codes, per-route bucket rate limiting with 429 handling in `JdkDiscordHttpClient`. Wire entity verbs and `ref.fetch()` to the facade. Tests: local `HttpServer` as now, plus allow-list tests per consumer row.
5. **Interactions (`interaction`).** Section 7.4: registrar, command publishing, event records, acknowledgement handles, components and modals. Tests: fixture `INTERACTION_CREATE` payloads through the reducer, handle state machine, callback envelopes.

The existing `*Impl` classes and `DukecordImplTest.retrievesEveryLookupInsteadOfCachingEntities` are replaced in phases 1 and 2; that test asserts the behaviour this document forbids.

## 11. Decisions

Settled on 2026-09-15:

- **Accessor style:** record style with `is`/`has` for booleans, `with` for copies, verbs for I/O (section 3.2).
- **References:** kept as `DiscordEntityRef` / `DiscordEntityRefList` with cache `get()` and REST `fetch()`; no bare id accessors next to a reference (section 3.3).
- **Specs:** detached JSON-backed values with the `Spec` suffix and consumer-owned envelopes; the bound `MessageCreateBuilder` with `execute()` is replaced (section 7.2).
- **Message cache:** on by default, global FIFO of 1000.
- **REST never writes the cache:** kept; the read-your-writes gap is documented with the `await` idiom.
- **Acknowledgement lifecycle:** handle types (`DeferredReply`, `DeferredEdit`), not a runtime flag.
- **Dispatch ordering:** parallel on virtual threads, race documented; no ordering key in v1.
- **Commands:** imperative registrar only; no annotation scanning in the core.

Still open:

- **Permissions as `long`:** fine today; switch `PermissionSet` to `BigInteger` only when Discord passes bit 62.
- **Presence and voice caching:** out of scope for v1; revisit if a target bot needs it.
