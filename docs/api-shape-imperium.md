# API shape check: Imperium on Dukecord

Purpose: re-implement the Discord module of a real bot against the API in [`entity-model.md`](entity-model.md) before any of it is built, and record where the design is pleasant, awkward or missing.
Derived from [xpdustry/imperium at `6f4d195`](https://github.com/xpdustry/imperium/tree/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb): [`DiscordService.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt), [`RoleSyncListener.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/account/RoleSyncListener.kt), [`MapSearchCommand.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSearchCommand.kt), [`MapSubmitCommand.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSubmitCommand.kt), [`MindustryBridgeListener.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/bridge/MindustryBridgeListener.kt), [`ReportListener.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/security/ReportListener.kt), [`DiscordCommandDispatcher.kt`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/command/DiscordCommandDispatcher.kt).

Everything marked `// PROPOSED` does not exist in the design doc and is invented here; section "Proposed interaction API" collects them. Imperium's non-Discord halves are stubs.

## 0. Stubs for Imperium's non-Discord world

```java
// Accounts, maps, storage, Mindustry: one-liners, the point is the Discord surface.
enum Rank { EVERYONE, VERIFIED, OVERSEER, MODERATOR, ADMIN, OWNER;
    Set<Rank> andBelow() { return EnumSet.range(EVERYONE, this); } }
enum Achievement { SUPPORTER, ACTIVE, HYPER, RESPECTED }
enum Gamemode { SURVIVAL, SANDBOX, ATTACK, PVP, HEXED }

record Account(int id, Rank rank, Optional<Snowflake> discord) {}
interface AccountService      { Optional<Account> byId(int id); Optional<Account> byDiscord(Snowflake id); }
interface AchievementService  { Set<Achievement> completed(int accountId); void grant(int accountId, Achievement a); }

record MindustryMap(int id, String name, String author, String description, int width, int height, Set<Gamemode> gamemodes) {}
record MapMeta(String name, String author, String description, int width, int height) {}
interface MapService     { List<MindustryMap> all(); List<MindustryMap> searchByName(String q); Optional<MindustryMap> byId(int id);
                           int create(MapMeta meta, Path file); void update(int id, MapMeta meta, Path file); }
interface ContentHandler { MapMeta parseMap(Path file) throws IOException;      // throws on a corrupt .msav
                           BufferedImage renderMap(Path file) throws IOException; }
interface IdCodec        { String encode(int id); OptionalInt tryDecode(String s); }
interface Messenger      { <M> void subscribe(Class<M> type, Consumer<M> handler); void broadcast(Object message); }

record DiscordConfig(Snowflake guild, Snowflake mapsChannel, Snowflake reportsChannel, Snowflake liveChatCategory,
                     Optional<Snowflake> alertsRole, Map<Rank, Snowflake> ranks, Map<Achievement, Snowflake> achievements,
                     Map<Permission, Snowflake> permissions, boolean globalCommands) {}

// Bridge payloads flowing in from the Mindustry servers over the message bus.
record BridgeChatMessage(String server, String author, long discordId, String text) {}
sealed interface PlayerAction { record Join() implements PlayerAction; record Quit() implements PlayerAction;
                                record Chat(String text) implements PlayerAction; }
record MindustryPlayerMessage(String server, String player, PlayerAction action) {}
record MindustryServerMessage(String server, String message, boolean chat) {}
record ReportMessage(String server, String senderName, int senderId, String targetName, int targetId, String reason) {}
```

## 1. Role synchronization

Imperium maps an account's rank ladder and its unlocked achievements onto Discord roles ([`DiscordService.kt#L118-L177`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt#L118-L177)). It walks every `Achievement` and every `Rank`, looks the configured role up in the guild, and issues a `PUT` or `DELETE` per role that is missing or stale — up to two dozen REST calls for one member. It is triggered from `GuildMemberJoinEvent`, from rank/achievement bus messages and from a `/sync-roles` slash command ([`RoleSyncListener.kt#L34-L66`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/account/RoleSyncListener.kt#L34-L66)).

```java
final class RoleSyncService {

    private final Dukecord duke;
    private final DiscordConfig config;
    private final AccountService accounts;
    private final AchievementService achievements;

    /** Computes the target role set and applies the diff. */
    void syncRoles(int accountId) {
        var account = accounts.byId(accountId).orElse(null);
        if (account == null || account.discord().isEmpty()) return;

        var guild = duke.requireGuild(config.guild());              // cache, throws if unknown/unavailable
        var member = guild.member(account.discord().orElseThrow()).orElse(null);   // cache read, no I/O
        if (member == null) return;                                 // left the guild

        // FRICTION 1: roleIds() is a List, every diff starts with a defensive Set.copyOf.
        Set<Snowflake> current = Set.copyOf(member.roleIds());
        Set<Snowflake> target  = new HashSet<>(current);

        var unlocked = achievements.completed(accountId);
        for (var achievement : Achievement.values()) {
            var roleId = config.achievements().get(achievement);
            if (roleId == null || guild.role(roleId).isEmpty()) continue;   // role deleted since config was written
            if (unlocked.contains(achievement)) target.add(roleId); else target.remove(roleId);
        }

        var granted = account.rank().andBelow();
        for (var rank : Rank.values()) {
            var roleId = config.ranks().get(rank);
            if (roleId == null || guild.role(roleId).isEmpty()) continue;
            if (granted.contains(rank)) target.add(roleId); else target.remove(roleId);
        }

        if (target.equals(current)) return;                         // the common case: zero REST calls

        // One PATCH /guilds/{g}/members/{u} with the whole role array.
        // MemberEdit.roles(...) is not in the design doc's builder list -- FRICTION 2.
        member.edit(MemberEdit.builder()                            // blocking, throws DiscordRestException
                .roles(target)                                      // PROPOSED: MemberEdit.Builder.roles(Collection<Snowflake>)
                .reason("rank and achievement synchronization")     // PROPOSED: audit-log reason on every spec builder
                .build());
    }

    /** The literal translation of Imperium, kept to show what the doc's API forces today. */
    void syncRolesOneCallPerRole(Guild guild, Member member, Set<Snowflake> current, Set<Snowflake> target) {
        for (var roleId : target)  if (!current.contains(roleId)) member.addRole(roleId);
        for (var roleId : current) if (!target.contains(roleId))  member.removeRole(roleId);
        // FRICTION 3: member.addRole(roleId) has no reason parameter although
        // DiscordRest.addMemberRole(guildId, userId, roleId, Optional<String> reason) does.
        // Also: 2N blocking calls, each its own rate-limit bucket hit, on a member join burst.
    }

    void install() {
        duke.events().register(MemberJoinEvent.class, event ->
                accounts.byDiscord(event.member().id()).ifPresent(a -> syncRoles(a.id())));

        messenger.subscribe(RankChangeEvent.class,  m -> syncRoles(m.accountId()));
        messenger.subscribe(AchievementUpdate.class, m -> { if (m.completed()) syncRoles(m.accountId()); });

        // Imperium's booster sweep: guild.boosters, which has no Dukecord equivalent -- FRICTION 4.
        syncBoosters();
        duke.events().register(GuildUpdateEvent.class, event -> {
            if (!Objects.equals(event.previous().raw().get("premium_subscription_count"),
                                event.current().raw().get("premium_subscription_count"))) syncBoosters();
        });
    }

    private void syncBoosters() {
        for (var member : duke.requireGuild(config.guild()).members()) {
            // member.boostingSince() does not exist; raw() is the only way out today.
            if (member.raw().getString("premium_since").isEmpty()) continue;
            accounts.byDiscord(member.id()).ifPresent(a -> achievements.grant(a.id(), Achievement.SUPPORTER));
        }
    }

    /** /sync-roles, the ephemeral-reply shape used by every Imperium command. */
    @SlashCommand(name = "sync-roles", description = "Synchronize your Discord roles with your CN account")   // PROPOSED
    void onSyncRoles(SlashCommandEvent event) {                     // PROPOSED: SlashCommandEvent
        DeferredReply reply = event.deferReply(Ephemeral.YES);      // PROPOSED: DeferredReply handle, Ephemeral enum
        var account = accounts.byDiscord(event.user().id()).orElse(null);
        if (account == null) { reply.send("You are not linked to a CN account."); return; }
        syncRoles(account.id());
        reply.send("Your Discord roles have been synchronized.");
    }
}
```

## 2. Startup and thread joining

Imperium builds JDA with six intents, `MemberCachePolicy.ALL`, and blocks on `awaitReady()` before the rest of the application starts. It then works around the rule that a bot only receives thread messages once it is a thread member: it joins every active thread on startup and re-joins on `ChannelCreateEvent` (for threads) and `ThreadRevealedEvent` ([`DiscordService.kt#L59-L89`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt#L59-L89)); the source carries the comment *"This is goofy, why do I need these to receive thread messages too"*.

```java
final class ImperiumDiscord implements AutoCloseable {

    private final Dukecord duke;
    private final DiscordConfig config;

    ImperiumDiscord(String token, DiscordConfig config) {
        this.config = config;
        this.duke = Dukecord.login(token, DukecordOptions.builder()
                .intents(GatewayIntentSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_EXPRESSIONS))
                .chunkMembersOnStartup(true)     // replaces MemberCachePolicy.ALL
                .autoJoinThreads(true)           // replaces all three JDA listeners below... in theory
                .messageCacheSize(1000)
                .readyTimeout(Duration.ofSeconds(60))
                .build());
        // login() blocks until READY + every GUILD_CREATE + every member chunk. Exactly awaitReady(), minus the ceremony.
    }

    /** Imperium's getMainServer(): jda.guildCache.first(). Dukecord wants the id. */
    Guild mainServer() {
        return duke.requireGuild(config.guild());
        // FRICTION 5: a single-guild library has no duke.requireSingleGuild(); the id must be configured
        // even though the bot is by construction in exactly one guild.
    }

    /**
     * What autoJoinThreads must actually cover. The doc says "joins on THREAD_CREATE and on GUILD_CREATE
     * active threads" -- that misses Imperium's ThreadRevealedEvent case (the bot gains access to a channel
     * later, and Discord replays the threads through THREAD_LIST_SYNC).  FRICTION 6.
     */
    void joinThreadsManually() {
        var guild = mainServer();

        for (var thread : guild.threads()) {                         // cache
            if (!thread.joined()) thread.join();                     // PROPOSED: ThreadChannel.joined()
            // Today the doc only offers Optional<ThreadMember> selfMember(), and the payloads doc says the
            // self thread-member is absent on gateway channel events -- so empty() means "unknown", not
            // "not joined". Calling join() unconditionally is the only correct translation, and it is a
            // wasted REST call per thread on every boot.
        }

        duke.events().register(ThreadCreateEvent.class, event -> {
            if (event.newlyCreated() || !event.thread().joined()) event.thread().join();
        });

        // The "revealed" case: THREAD_LIST_SYNC after a permission change.
        duke.events().register(ThreadListSyncEvent.class, event -> {
            for (var thread : event.threads()) if (!thread.joined()) thread.join();
        });

        // And un-archiving, which drops thread membership server-side.
        duke.events().register(ThreadUpdateEvent.class, event -> {
            boolean unarchived = event.previous().map(t -> t.metadata().archived()).orElse(false)
                                 && !event.current().metadata().archived();
            if (unarchived) event.current().join();
        });
    }

    @Override public void close() {
        duke.close();
        // FRICTION 7: Imperium does jda.shutdown() then awaitShutdown(15s) then shutdownNow().
        // The doc's close() has no bounded-timeout form.  Proposed: close(Duration) or
        // DukecordOptions.shutdownTimeout.
    }
}
```

Registration of the command tree, which Imperium does after ready ([`DiscordCommandDispatcher.kt#L361-L380`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/command/DiscordCommandDispatcher.kt#L361-L380)):

```java
CommandRegistrar registrar = duke.commands();                       // PROPOSED: Dukecord.commands()
registrar.register(new MapSearchCommand(...))                       // PROPOSED: scans @SlashCommand/@ComponentHandler/@ModalHandler
         .register(new MapSubmitCommand(...))
         .register(new RoleSyncService(...));
if (config.globalCommands()) registrar.publishGlobal();             // PROPOSED: PUT /applications/{id}/commands
else                         registrar.publishGuild(config.guild());// PROPOSED: PUT .../guilds/{id}/commands
```

## 3. Map search: paginated, stateful, expiring

`/map search [name]` defers a public reply, runs a DB query, and posts one embed page plus two action rows (five paging buttons and a gamemode multi-select). The per-message state lives in a Caffeine cache keyed by message id with a 1-minute write / 5-minute access expiry; on eviction a removal listener re-fetches the message and disables its components ([`MapSearchCommand.kt#L45-L183`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSearchCommand.kt#L45-L183)). Component handlers check ownership, `deferEdit()`, then `editOriginal` with a `MessageEditData.fromCreateData(...)` of the same builder.

```java
final class MapSearchCommand {

    private static final String PREV   = "map-search-previous:1";
    private static final String NEXT   = "map-search-next:1";
    private static final String FIRST  = "map-search-first:1";
    private static final String LAST   = "map-search-last:1";
    private static final String MODES  = "map-search-gamemode-select:1";
    private static final String NONE   = "none";
    private static final int PAGE_SIZE = 15;

    record State(Optional<String> query, int page, Set<Optional<Gamemode>> gamemodes,
                 Snowflake owner, Snowflake channel) {
        State withPage(int p)                        { return new State(query, p, gamemodes, owner, channel); }
        State withGamemodes(Set<Optional<Gamemode>> g){ return new State(query, page, g, owner, channel); }
    }

    private final Dukecord duke;
    private final MapService maps;
    private final IdCodec codec;

    // Snowflake is a record, so it is a fine map key. Expiry + eviction hook stay the bot's job.
    private final Cache<Snowflake, State> states = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .expireAfterAccess(Duration.ofMinutes(5))
            .removalListener((Snowflake id, State state, RemovalCause cause) -> {
                if (id == null || state == null || !cause.wasEvicted()) return;
                Thread.ofVirtual().start(() -> disableComponents(state.channel(), id));
            })
            .build();

    @SlashCommand(name = "map search", description = "Search the map pool")
    void onMapSearch(SlashCommandEvent event,
                     @Option(name = "name", description = "Name filter", required = false) Optional<String> name) {
        // Options can also be pulled imperatively; the annotation form is sugar over this.
        Optional<String> query = event.option("name", String.class);          // PROPOSED
        Snowflake channelId = event.channelId();

        DeferredReply reply = event.deferReply(Ephemeral.NO);                 // callback type 5, within 3 s
        var state  = new State(query, 0, Set.of(), event.user().id(), channelId);
        var result = search(state);

        // The handle's send() returns the real Message, so the message id is available for state keying.
        Message message = reply.send(page(result, state));                    // PATCH @original, returns Message
        states.put(message.id(), state);
    }

    @ComponentHandler({PREV, NEXT, FIRST, LAST})                              // PROPOSED: custom-id routing
    void onPaging(ButtonEvent event) {                                        // PROPOSED: ButtonEvent
        update(event, state -> switch (event.customId()) {
            case PREV  -> state.withPage(state.page() - 1);
            case NEXT  -> state.withPage(state.page() + 1);
            case FIRST -> state.withPage(0);
            case LAST  -> state.withPage(Integer.MAX_VALUE);
            default    -> state;
        });
    }

    @ComponentHandler(MODES)
    void onGamemodeSelect(StringSelectEvent event) {                          // PROPOSED: StringSelectEvent
        var selected = event.values().stream()
                .map(v -> v.equals(NONE) ? Optional.<Gamemode>empty() : Optional.of(Gamemode.valueOf(v)))
                .collect(Collectors.toSet());
        update(event, state -> state.withGamemodes(selected));
    }

    private void update(ComponentEvent event, UnaryOperator<State> mutation) {  // PROPOSED: ComponentEvent supertype
        Message message = event.message();                                      // PROPOSED: the message the component is on
        var state = states.getIfPresent(message.id());

        if (state == null) {
            event.deferReply(Ephemeral.YES).send("This button has expired.");
            return;
        }
        if (!state.owner().equals(event.user().id())) {
            event.deferReply(Ephemeral.YES).send("This button is not for you.");
            // Reset the select, which Discord has already visually mutated on the clicker's client.
            message.edit(MessageEdit.from(page(search(state), state)));         // PROPOSED: MessageEdit.from(MessageCreate)
            return;
        }

        DeferredEdit edit = event.deferEdit();                                  // PROPOSED: callback type 6, no loading state
        var next   = mutation.apply(state);
        var result = search(next);
        next = next.withPage(Math.min(next.page(), pages(result)));
        states.put(message.id(), next);
        edit.editOriginal(MessageEdit.from(page(result, next)));

        // FRICTION 8: the page body is naturally a MessageCreate (content + embeds + components), but every
        // consumer after the first acknowledgement takes MessageEdit. Without MessageEdit.from(MessageCreate)
        // -- which the doc does not have -- this function has to build the same body twice.
        // FRICTION 9: two clicks on two virtual threads race on states + editOriginal. The doc guarantees no
        // per-message ordering; a paginator is the canonical case that wants it.
    }

    /** One body, reused for the first reply and for every edit. */
    private MessageCreate page(List<MindustryMap> result, State state) {
        int pages = pages(result);
        var listing = result.stream().skip((long) state.page() * PAGE_SIZE).limit(PAGE_SIZE).toList();

        var embed = Embed.builder()                                             // PROPOSED: Embed.builder()
                .color(MINDUSTRY_ACCENT)
                .description(listing.isEmpty() ? "No maps found"
                        : listing.stream().map(m -> "- " + m.name() + " / " + codec.encode(m.id()))
                                 .collect(Collectors.joining("\n")))
                .build();

        var paging = ActionRow.of(                                              // PROPOSED: ActionRow, Button
                Button.primary(PREV, "Previous").withDisabled(state.page() == 0),
                Button.secondary("unused", (state.page() + 1) + " / " + (pages + 1)).withDisabled(true),
                Button.primary(NEXT, "Next").withDisabled(state.page() == pages),
                Button.success(FIRST, "First").withDisabled(state.page() == 0),
                Button.success(LAST, "Last").withDisabled(state.page() == pages));

        var options = Stream.concat(Arrays.stream(Gamemode.values()).map(Optional::of), Stream.of(Optional.<Gamemode>empty()))
                .map(g -> g.map(Enum::name).orElse(NONE))
                .map(n -> SelectOption.of(n.toLowerCase().replace('_', ' '), n))   // PROPOSED: SelectOption
                .toList();

        var filter = ActionRow.of(StringSelect.builder(MODES)                   // PROPOSED: StringSelect
                .placeholder("Gamemode")
                .minValues(0)
                .maxValues(options.size())
                .options(options)
                .defaultValues(state.gamemodes().stream().map(g -> g.map(Enum::name).orElse(NONE)).toList())
                .build());

        return MessageCreate.builder()
                .embed(embed)
                .components(List.of(paging, filter))
                .build();
        // Pleasant: allowed_mentions is written as "none" at construction, so a listing that happens to
        // contain "@everyone" in a map name cannot ping the guild. Imperium had to remember this by hand.
    }

    /** Expiry: disable the controls without needing the interaction token, which is 15 minutes dead anyway. */
    private void disableComponents(Snowflake channelId, Snowflake messageId) {
        try {
            // The only way today: fetch, map every component to a disabled copy, edit.
            Message message = duke.rest().fetchMessage(channelId, messageId);
            duke.rest().editMessage(channelId, messageId, MessageEdit.builder()
                    .components(message.components().stream().map(Component::asDisabled).toList())   // PROPOSED
                    .build());
            // FRICTION 10: this is a GET + a PATCH for a pure UI teardown that every paginating bot needs.
            // Proposed: MessageEdit.disablingComponentsOf(Message), and better, Message.disableComponents().
        } catch (DiscordNotFoundException ignored) {
            // message deleted; nothing to disable. Unchecked, so no catch is forced on the happy path.
        }
    }

    private List<MindustryMap> search(State state) {
        var base = state.query().map(maps::searchByName).orElseGet(maps::all);
        return base.stream().filter(m -> state.gamemodes().isEmpty()
                || !Collections.disjoint(m.gamemodes(), unwrap(state.gamemodes()))
                || (state.gamemodes().contains(Optional.empty()) && m.gamemodes().isEmpty())).toList();
    }

    private static int pages(List<MindustryMap> r) { return Math.max(0, (int) Math.ceil(r.size() / (float) PAGE_SIZE) - 1); }
}
```

## 4. Map submit: attachments, downloads, uploads, review flow

`/map submit <map> [updating] [notes]` takes an `.msav` attachment, validates the extension and size, downloads it to a temp file, parses and renders a preview, then posts one message to the maps channel carrying two uploads (the `.msav` and `preview.png`), an embed whose image points at `attachment://preview.png`, and Accept/Reject buttons; it starts a comments thread on that message and reports back with a jump link ([`MapSubmitCommand.kt#L53-L159`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSubmitCommand.kt#L53-L159)). The review buttons are rank-gated, re-download the attachment off the posted message, recolor the embed, strip the components and DM the submitter — and the source contains *"Why do I have to fight discord API to simply update an image in an embed ? I hate it"* ([`#L219-L275`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSubmitCommand.kt#L219-L275)).

```java
final class MapSubmitCommand {

    private static final String ACCEPT = "map-submission-accept:2";
    private static final String REJECT = "map-submission-reject:2";
    private static final String REJECT_MODAL = "map-submission-reject-reason:2";

    private final Dukecord duke;
    private final DiscordConfig config;
    private final MapService maps;
    private final ContentHandler content;
    private final IdCodec codec;

    @SlashCommand(name = "map submit", description = "Submit a map for review")
    void onMapSubmit(SlashCommandEvent event) {
        Attachment map = event.option("map", Attachment.class).orElseThrow();   // PROPOSED: attachment option type
        Optional<String> updating = event.option("updating", String.class).map(String::toLowerCase);
        Optional<String> notes    = event.option("notes", String.class);

        DeferredReply reply = event.deferReply(Ephemeral.NO);

        if (!map.filename().endsWith(".msav"))                       { reply.send("Invalid map file!"); return; }
        if (map.size() > MAX_MAP_FILE_SIZE)                          { reply.send("The map file is too big."); return; }

        OptionalInt updatingId = updating.map(codec::tryDecode).orElse(OptionalInt.empty());
        if (updating.isPresent() && (updatingId.isEmpty() || maps.byId(updatingId.getAsInt()).isEmpty())) {
            reply.send("Invalid map identifier!"); return;
        }

        Path temp = Files.createTempFile("submission", ".msav");
        try {
            // FRICTION 11: the doc says Attachment is "a plain record with no client back-reference ...
            // a value, not an entity", so it cannot download itself, yet the feature-set doc requires a
            // blocking download. Today the only in-API route is the raw HTTP escape hatch:
            try (var in = duke.rest().http().get(map.url())) { Files.copy(in, temp, REPLACE_EXISTING); }
            // Proposed: map.downloadTo(temp) / map.open(), or duke.rest().download(String url).

            MapMeta meta;
            try { meta = content.parseMap(temp); }
            catch (IOException e) { reply.send("Invalid map file: " + e.getMessage()); return; }

            if (meta.width() > MAX_SIDE || meta.height() > MAX_SIDE) {
                reply.send("The map is bigger than " + MAX_SIDE + " blocks."); return;
            }

            BufferedImage preview;
            try { preview = content.renderMap(temp); } catch (IOException e) { preview = FALLBACK_PREVIEW; }

            var channel = duke.requireGuild(config.guild())
                    .channel(config.mapsChannel(), GuildTextChannel.class)       // typed cache lookup, pleasant
                    .orElseThrow(() -> new IllegalStateException("Map submission channel not found"));

            var embed = Embed.builder()
                    .color(MINDUSTRY_ACCENT)
                    .title("Map Submission")
                    .field("Submitter", event.member().orElseThrow().mention(), false)
                    .field("Name", strip(meta.name()), false)
                    .field("Author", strip(meta.author()), false)
                    .field("Description", strip(meta.description()), false)
                    .field("Size", meta.width() + " x " + meta.height(), false)
                    .fieldIf(notes.isPresent(), "Notes", notes.orElse(""), false)
                    .fieldIf(updating.isPresent(), "Updating", "`" + updating.orElse("") + "`", false)
                    .image("attachment://preview.png")
                    .build();

            Message submission = channel.send(MessageCreate.builder()
                    .file(FileUpload.of(temp, strip(meta.name()) + ".msav"))     // PROPOSED: filename override
                    .file(FileUpload.ofStream("preview.png", () -> pngStream(preview)))  // PROPOSED: stream supplier
                    .embed(embed)
                    .component(ActionRow.of(
                            Button.primary(ACCEPT, "Accept").withEmoji(Emoji.unicode("✅")),
                            Button.danger(REJECT, "Reject").withEmoji(Emoji.unicode("🗑"))))
                    .build());

            // FRICTION 12: a thread anchored to a message is POST /channels/{c}/messages/{m}/threads, but the
            // doc only puts createThread(ThreadStart) on MessageChannel, which is the standalone route.
            submission.createThread(ThreadStart.builder()                        // PROPOSED: Message.createThread
                    .name("Comments for " + strip(meta.name()))
                    .autoArchiveDuration(Duration.ofDays(3))
                    .build());

            reply.send(MessageCreate.builder()
                    .embed(Embed.builder().color(MINDUSTRY_ACCENT)
                            .description("Your map has been submitted for review. Check the status ["
                                    + "here](" + submission.jumpUrl() + ").")
                            .build())
                    .build());
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @ComponentHandler(value = ACCEPT, rank = Rank.ADMIN, alsoAllow = Permission.MANAGE_MAP)   // PROPOSED gate attrs
    void onAccept(ButtonEvent event) {
        DeferredReply reply = event.deferReply(Ephemeral.YES);
        Message submission = event.message();
        Attachment msav = submission.attachments().getFirst();

        Path temp = Files.createTempFile("accepted", ".msav");
        try (var in = duke.rest().http().get(msav.url())) {
            Files.copy(in, temp, REPLACE_EXISTING);
            MapMeta meta = content.parseMap(temp);

            var embed = submission.embeds().getFirst();
            Optional<String> updating = embed.field("Updating").map(v -> v.replace("`", ""));  // PROPOSED: Embed.field(name)

            int id;
            if (updating.isPresent()) {
                var target = maps.byId(codec.tryDecode(updating.get()).orElse(-1)).orElse(null);
                if (target == null) { reply.send("The map to update no longer exists!"); return; }
                id = target.id();
                maps.update(id, meta, temp);
            } else {
                id = maps.create(meta, temp);
            }

            finishReview(event, GREEN, "accepted", OptionalInt.of(id));
            reply.send("Map submission uploaded! The map id is `" + codec.encode(id) + "`.");
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Reject opens a modal for the reason. A modal IS the acknowledgement: it cannot follow a defer. */
    @ComponentHandler(value = REJECT, rank = Rank.ADMIN, alsoAllow = Permission.MANAGE_MAP)
    void onReject(ButtonEvent event) {
        event.replyModal(Modal.builder(REJECT_MODAL + ":" + event.message().id().asString())  // PROPOSED: Modal
                .title("Reject map submission")
                .row(TextInput.paragraph("reason", "Reason")                                  // PROPOSED: TextInput
                        .placeholder("Why is this map rejected?")
                        .required(true)
                        .maxLength(400)
                        .build())
                .build());
        // FRICTION 13: replyModal is callback type 9 and is only legal as the FIRST response, and is illegal
        // from a ModalSubmit. The doc's acknowledgement table does not mention type 9 at all, and nothing in
        // the type system stops event.deferReply(...) then event.replyModal(...). Make it a compile-time or
        // at least a documented runtime state.
        // FRICTION 14: the modal id is the only channel for carrying state to the submit event, so bots end
        // up string-encoding snowflakes into custom ids. A customIdSuffix/state parameter would be honest.
    }

    @ModalHandler(REJECT_MODAL)                                                               // PROPOSED
    void onRejectReason(ModalSubmitEvent event) {                                             // PROPOSED
        DeferredReply reply = event.deferReply(Ephemeral.YES);
        String reason = event.value("reason").orElse("no reason given");                      // PROPOSED
        Snowflake messageId = Snowflake.of(event.customId().substring(event.customId().lastIndexOf(':') + 1));
        // The modal submit is NOT attached to the button's message, so it must be re-fetched.
        Message submission = duke.rest().fetchMessage(event.channelId(), messageId);
        finishReview(event, RED, "rejected: " + reason, OptionalInt.empty(), submission);
        reply.send("Map submission rejected.");
    }

    /** Recolor the embed, add the reviewer, strip the buttons, keep both uploads, then DM the submitter. */
    private void finishReview(InteractionEvent event, int color, String verb, OptionalInt id, Message submission) {
        var old = submission.embeds().getFirst();
        var updated = Embed.builder(old)                                    // PROPOSED: Embed.builder(Embed)
                .color(color)
                .field("Reviewer", event.member().orElseThrow().mention(), false)
                .fieldIf(id.isPresent() && old.field("Updating").isEmpty(),
                         "Identifier", "`" + codec.encode(id.orElse(0)) + "`", false)
                .build();

        submission.edit(MessageEdit.builder()
                .embeds(List.of(updated))
                .clearComponents()
                .build());
        // PLEASANT: this is the single biggest win over JDA. Because MessageEdit only writes `attachments`
        // when keepAttachments/removeAttachments/file is called, omitting it keeps BOTH uploads, so
        // attachment://preview.png keeps resolving. Imperium had to re-download its own .msav and its own
        // preview.png from Discord's CDN and re-upload them under a new name (preview2.png) to change an
        // embed colour. Dukecord deletes that entire hack.

        // Notify the submitter by DM, plus Imperium's "Contributor" role band-aid.
        var guild = duke.requireGuild(config.guild());
        var submitterId = Snowflake.of(MENTION.matcher(old.field("Submitter").orElseThrow()).group(1));
        var member = guild.member(submitterId).orElse(null);
        if (member == null) return;

        guild.roles().stream().filter(r -> r.name().equals("Contributor")).findFirst()
                .ifPresent(role -> member.addRole(role.id()));

        try {
            member.user().openDm().send(MessageCreate.builder()
                    .embed(Embed.builder().color(color)
                            .description("Your [map submission](" + submission.jumpUrl() + ") has been "
                                    + verb + " by " + event.member().orElseThrow().mention() + ".")
                            .build())
                    .build());
        } catch (DiscordRestException e) {
            if (e.code() != 50007) throw e;     // "Cannot send messages to this user" -- DMs closed
            // FRICTION 15: 50007 is the single most common send failure for any bot that DMs users and it
            // has no named subtype the way 404 gets DiscordNotFoundException. Proposed: DiscordForbiddenException
            // plus a named ErrorCode constant table.
        }
    }
}
```

## 5. Chat bridge

Inbound: every `MessageReceivedEvent` is filtered on webhook/bot/system author, cast to `TextChannel`, and accepted only when its parent category is the configured live-chat category; the stripped content and the member's nickname are broadcast onto the message bus ([`MindustryBridgeListener.kt#L30-L48`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/bridge/MindustryBridgeListener.kt#L30-L48)). Outbound: join/quit/chat lines are sent with `setAllowedMentions(emptySet())` into a per-server channel that is looked up by name under that category and created on demand ([`#L50-L87`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/bridge/MindustryBridgeListener.kt#L50-L87)).

```java
final class MindustryBridgeListener {

    private final Dukecord duke;
    private final DiscordConfig config;
    private final Messenger messenger;

    void install() {
        duke.events().register(MessageCreateEvent.class, event -> {
            Message message = event.message();

            if (message.fromWebhook()) return;                      // exactly Imperium's isWebhookMessage
            if (message.author().bot()) return;
            if (message.type() != MessageType.DEFAULT && message.type() != MessageType.REPLY) return;
            // FRICTION 16: Imperium also filters author.isSystem. The doc's User has bot() but no system(),
            // so the check degrades into a MessageType allow-list which is not the same predicate.

            if (!(message.channel().orElse(null) instanceof GuildTextChannel channel)) return;
            // The cache navigation is fine, but note channel() is Optional<MessageChannel> so every
            // consumer needs an instanceof pattern anyway.

            if (!channel.parentId().filter(config.liveChatCategory()::equals).isPresent()) return;
            // FRICTION 17: "is this channel in that category" is three combinators.
            // Proposed: GuildChannel.inCategory(Snowflake).

            messenger.broadcast(new BridgeChatMessage(
                    channel.name(),
                    message.member().map(Member::displayName).orElseGet(() -> message.author().username()),
                    // PLEASANT: member() is the inline payload member merged with author, no cache round trip
                    // and no REST fallback. Imperium's JDA equivalent needed message.member?.nickname ?: author.name.
                    message.author().id().asLong(),
                    message.content()));
            // FRICTION 18: Imperium sends contentStripped -- mentions rendered as @name, markdown removed --
            // because the destination is a Mindustry chat box. The doc has only content(), so raw
            // "<@123456789>" ends up in the game. Proposed: Message.strippedContent().
        });

        messenger.subscribe(MindustryPlayerMessage.class, message -> {
            var channel = liveChatChannel(message.server());
            if (channel == null) return;
            String text = switch (message.action()) {
                case PlayerAction.Join  j -> ":green_square: **" + message.player() + "** has joined the server.";
                case PlayerAction.Quit  q -> ":red_square: **"   + message.player() + "** has left the server.";
                case PlayerAction.Chat  c -> ":blue_square: **"  + message.player() + "**: " + c.text();
            };
            channel.send(text);
            // PLEASANT: send(String) already carries allowed_mentions: none, so the mention-safety property
            // that Imperium had to repeat at every call site is the default and cannot be forgotten.
            // If it had to be explicit it would read:
            //   channel.send(MessageCreate.builder().content(text).allowedMentions(AllowedMentions.none()).build());
        });

        messenger.subscribe(MindustryServerMessage.class, message -> {
            var channel = liveChatChannel(message.server());
            if (channel == null) return;
            channel.send(":purple_square: " + (message.chat() ? "**" + message.server() + "**: " : "") + message.message());
        });
    }

    /** Find the per-server channel under the live-chat category, or create it. */
    private GuildTextChannel liveChatChannel(String server) {
        var guild = duke.requireGuild(config.guild());
        var category = guild.channel(config.liveChatCategory(), CategoryChannel.class).orElse(null);
        if (category == null) { LOGGER.error("Live chat category is not defined."); return null; }

        // FRICTION 19: the doc gives the child -> parent link (GuildChannel.category()) but not the
        // parent -> children one, so a "find the channel named X in this category" -- which is the whole
        // reason a category exists -- is a full scan with an Optional comparison:
        var existing = guild.channels().stream()
                .filter(c -> c.parentId().filter(category.id()::equals).isPresent())
                .filter(c -> c.name().equals(server))
                .filter(GuildTextChannel.class::isInstance).map(GuildTextChannel.class::cast)
                .findFirst();
        // Proposed: category.channels() and category.channel(String name).
        if (existing.isPresent()) return existing.get();

        return (GuildTextChannel) guild.createTextChannel(ChannelCreate.builder()
                .name(server)
                .parentId(category.id())
                .build());
        // FRICTION 20: createTextChannel is typed GuildTextChannel in the catalogue but
        // DiscordRest.createChannel returns GuildChannel; the entity verb should return GuildTextChannel
        // and the cast should not be in bot code. Also, "REST never writes the cache" means an immediate
        // second broadcast for the same server re-scans a cache that does not have the new channel yet
        // and creates a duplicate. Proposed: document the read-your-writes gap, or give
        // duke.events().await(ChannelCreateEvent.class, e -> ..., timeout) as the blessed idiom.
    }
}
```

## 6. Extra: report alerts (role ping, message thread, one-shot components)

Picked because it exercises three things the four features above do not: a deliberate role mention against the mention-safe default, a thread started on a just-sent message and then written into in chunks, and a "claim this" button that permanently disables its own controls ([`ReportListener.kt#L37-L150`](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/security/ReportListener.kt#L37-L150)).

```java
final class ReportListener {

    private static final String REVIEW = "report-review:1";
    private static final String IGNORE = "report-ignore:1";

    void install() {
        messenger.subscribe(ReportMessage.class, report -> {
            var guild   = duke.requireGuild(config.guild());
            var channel = guild.channel(config.reportsChannel(), GuildTextChannel.class).orElse(null);
            if (channel == null) { LOGGER.error("The report channel is not defined."); return; }

            var alerts = config.alertsRole().flatMap(guild::role);

            Message alert = channel.send(MessageCreate.builder()
                    .content(alerts.map(Role::mention).orElse(""))
                    // The default is "mention nothing", so a deliberate ping must opt in -- and it must opt
                    // in narrowly, not by flipping the whole thing back to Discord's defaults:
                    .allowedMentions(AllowedMentions.roles(alerts.map(Role::id).stream().toList()))  // PROPOSED
                    .embed(Embed.builder().color(YELLOW)
                            .field("Sender", report.senderName()).field("ID", codec.encode(report.senderId())).blankField()
                            .field("Target", report.targetName()).field("ID", codec.encode(report.targetId())).blankField()
                            .field("Server", report.server(), false)
                            .field("Reason", report.reason(), false)
                            .build())
                    .component(ActionRow.of(
                            Button.primary(REVIEW, "Review").withEmoji(Emoji.unicode("🔍")),
                            Button.secondary(IGNORE, "Ignore").withEmoji(Emoji.unicode("❌"))))
                    .build());

            ThreadChannel thread = alert.createThread(ThreadStart.builder()
                    .name("Report on " + report.targetName())
                    .autoArchiveDuration(Duration.ofDays(3))
                    .build());
            // Because autoJoinThreads is on, the bot is already a member by the time this returns -- or is it?
            // FRICTION 21: the doc never says whether a thread the bot itself creates needs joining, nor
            // whether autoJoinThreads covers threads created through REST (whose THREAD_CREATE may arrive
            // after this line). A thread created by the bot is auto-joined by Discord; state it.

            for (var chunk : Lists.partition(history(report), 10))
                thread.send("```\n" + String.join("\n", chunk) + "\n```");
        });
    }

    @ComponentHandler(value = {REVIEW, IGNORE}, rank = Rank.MODERATOR)
    void onReviewOrIgnore(ButtonEvent event) {
        DeferredReply reply = event.deferReply(Ephemeral.YES);
        boolean review = event.customId().equals(REVIEW);
        var old = event.message().embeds().getFirst();

        event.message().edit(MessageEdit.builder()
                .embeds(List.of(Embed.builder(old)
                        .field(review ? "Assigned to" : "Ignored by",
                               event.member().orElseThrow().mention(), false)
                        .build()))
                .clearComponents()      // one PATCH; Imperium needed edit-embeds + disableComponents = two
                .build());

        reply.send(review ? "You have claimed this report, good luck!" : "You have ignored this report.");
    }

    /** Imperium's permission gate, translated. It is role-id driven, not Discord-permission driven. */
    boolean isAllowed(Snowflake userId, Rank required) {
        if (required == Rank.EVERYONE) return true;
        if (accounts.byDiscord(userId).map(Account::rank).orElse(Rank.EVERYONE).compareTo(required) >= 0) return true;
        var guild = duke.requireGuild(config.guild());
        return guild.member(userId).map(Member::roleIds).orElse(List.of()).stream()
                .map(id -> config.rolesToRanks().getOrDefault(id, Rank.EVERYONE))
                .max(Comparator.naturalOrder()).orElse(Rank.EVERYONE)
                .compareTo(required) >= 0;
        // The channel-permission flavour, which MAD needs and Imperium does not:
        //   guild.permissionsOf(member, channel).contains(Permission.MANAGE_MESSAGES)
        // FRICTION 22: for an interaction the authoritative answer is member.permissions / app_permissions
        // out of INTERACTION_CREATE (the payloads doc calls it the only source), but the Member catalogue
        // has no permissions() accessor, so the one precomputed value Discord gives us is reachable only
        // through raw(). Proposed: Member.permissions() -> Optional<PermissionSet> and
        // InteractionEvent.appPermissions() -> PermissionSet.
    }
}
```

## Friction report

Ordered roughly by how much damage each one does.

1. **`Attachment` cannot download itself.** `try (var in = duke.rest().http().get(map.url()))` in section 4. The doc classifies `Attachment` as "a plain record ... a value, not an entity", which forbids the blocking download that the feature-set doc lists as required. Map submission is dead without it and falls through to the raw HTTP escape hatch. **Change:** either give `Attachment` `InputStream open()` / `void downloadTo(Path)` (it needs the client back-reference, so admit it is an entity), or add `InputStream DiscordRest.download(String cdnUrl)` and a `Message.download(Attachment)` convenience.
2. **No `MessageEdit.from(MessageCreate)`.** `edit.editOriginal(MessageEdit.from(page(result, next)))` in section 3. Every paginator builds a body once and then needs it as a create (first reply) and as an edit (every click). Imperium literally calls `MessageEditData.fromCreateData(...)` at two sites. Without a converter, each renderer has to exist twice or the bot has to hand-roll it. **Change:** `static MessageEdit MessageEdit.from(MessageCreate)` and its inverse, both defined as key-level copies through the allow-list.
3. **`reply()` changes its return type based on invisible state.** Section 7.2's table says `interaction.reply` returns `void` before a defer and `Message` after one. A method whose contract depends on a runtime flag is unusable: `/map search` needs the message id to key its state and cannot get it from the non-deferred path at all. **Change:** `deferReply(Ephemeral)` returns a `DeferredReply` handle whose `send(MessageCreate)` returns `Message`; `reply(...)` on the event stays `void` and is only legal before acknowledgement; add `Message fetchOriginal()` for the non-deferred case. This is JDA's `InteractionHook` and Imperium uses it on every single command.
4. **Modal replies are absent from the acknowledgement table.** `event.replyModal(...)` in section 4. Callback type 9 is the one acknowledgement that cannot follow a defer and cannot be issued from a modal submit, and section 7.2's consumer table does not list it. **Change:** add the row (`interaction.replyModal` / accepts `Modal` / envelope `{type: 9, data: modal}` / returns `void`), and state that it is only valid as the first response to a component or command interaction.
5. **`ThreadChannel` has no `joined()`.** `if (!thread.joined()) thread.join();` in section 2. The doc offers `Optional<ThreadMember> selfMember()`, but the payloads doc says the self thread-member is absent on gateway channel events — so `isEmpty()` conflates "not joined" and "not told", which violates rule 7's own "absence has one representation". Imperium's whole reason for touching threads is this flag. **Change:** `boolean joined()` on `ThreadChannel`, tracked from `THREAD_MEMBER_UPDATE` / `THREAD_MEMBERS_UPDATE`, or make it `Possible<ThreadMember>`.
6. **`autoJoinThreads` does not cover the case Imperium actually hit.** Section 2: three extra listeners survive the option. The doc scopes it to `THREAD_CREATE` plus `GUILD_CREATE` active threads; Imperium also needs `ThreadRevealedEvent` (arriving as `THREAD_LIST_SYNC` when the bot gains channel access) and un-archival via `THREAD_UPDATE`. **Change:** define `autoJoinThreads` as "join any thread that enters the cache un-joined, from `THREAD_CREATE`, `THREAD_LIST_SYNC`, `THREAD_UPDATE` un-archive and `GUILD_CREATE`", and say whether self-created threads count.
7. **No `CategoryChannel.channels()`.** The five-line stream in `liveChatChannel`. Child-to-parent navigation exists (`GuildChannel.category()`), parent-to-child does not, so the single most common category operation is a manual scan with `parentId().filter(id::equals).isPresent()`. **Change:** `List<GuildChannel> CategoryChannel.channels()`, `Optional<GuildChannel> channel(String name)`, and `boolean GuildChannel.inCategory(Snowflake)`.
8. **`Message.createThread` is missing.** `submission.createThread(ThreadStart...)` in sections 4 and 6. The doc puts `createThread` only on `MessageChannel`, which is `POST /channels/{c}/threads`; a submission thread and a report thread both need the message-anchored route. Two of Imperium's seven Discord files use it. **Change:** add `ThreadChannel Message.createThread(ThreadStart)`.
9. **`MemberEdit` has no `roles`.** `member.edit(MemberEdit.builder().roles(target)...)` in section 1. The doc lists `MemberEdit` as "same pattern, one builder each" without a key list, so today the only documented path is `addRole`/`removeRole`, i.e. up to 2N REST calls to express one diff. Role sync fires on every member join. **Change:** spell out `MemberEdit`: `nick`, `roles(Collection<Snowflake>)`, `timeoutUntil`, `mute`, `deaf`, `channelId`.
10. **Audit-log reasons are inconsistent and shaped as `Optional<String>` parameters.** `member.addRole(roleId)` (no reason) versus `DiscordRest.addMemberRole(..., Optional<String> reason)` versus `deleteMessage(..., Optional<String> reason)`. Entity verbs silently drop the reason, and `Optional` as a parameter type is a documented anti-pattern. **Change:** `reason(String)` on every spec builder, plus overloads (`addRole(roleId)` / `addRole(roleId, String reason)`) on the entity verbs. Never `Optional<T>` in a parameter position.
11. **Disabling a message's components costs a GET plus a PATCH.** `disableComponents` in section 3. Expiring an interactive message is the canonical component-state teardown and the feature-set doc calls it out explicitly, but `Component` is a "plain record" with no disabled-copy operation. **Change:** `Component asDisabled()` on the sealed component hierarchy, `MessageEdit.disablingComponentsOf(Message)`, and `void Message.disableComponents()`.
12. **`Embed` is declared a value record and then used like a builder target everywhere.** `Embed.builder()`, `Embed.builder(old)`, `old.field("Updating")` across sections 3, 4 and 6. Every one of Imperium's seven files builds embeds, and two of them rebuild an embed from an existing one plus a field. The doc never mentions `Embed.builder()` and never mentions field lookup. **Change:** specify `Embed.builder()`, `Embed.builder(Embed)` (copy), `Optional<String> field(String name)`, and `blankField()`; the embed builder is as load-bearing as `MessageCreate.builder()` and deserves a named place in section 7.2.
13. **`AllowedMentions` is used but never defined.** `.allowedMentions(AllowedMentions.roles(...))` in section 6. Section 7.2 says both builders write `allowed_mentions: none` at construction and that `allowedMentions(...)` overrides, but the type has no definition anywhere. The report alert needs "ping exactly this one role and nothing else", which is the interesting case. **Change:** define it: `AllowedMentions.none()`, `all()`, `users(Collection<Snowflake>)`, `roles(Collection<Snowflake>)`, `repliedUser(boolean)`, `everyone()`, and a `with*` combinator set.
14. **`FileUpload` cannot rename or stream.** `FileUpload.of(temp, "MyMap.msav")`, `FileUpload.ofStream("preview.png", supplier)` in section 4. The doc shows only `FileUpload.of(path)`, while the feature-set doc requires bytes/`Path`/`InputStream` supplier — and an `.msav` submitted as `tmp12345.msav` is useless. **Change:** `of(Path)`, `of(Path, String filename)`, `of(byte[], String)`, `ofStream(String, Supplier<InputStream>)`, `withDescription(String)`.
15. **`Message.content()` only. No stripped form.** `message.content()` in section 5. The bridge writes into a Mindustry chat box, so `<@123456789>` and `**bold**` are garbage on the other side; Imperium uses `contentStripped`. **Change:** `String strippedContent()` on `Message` (resolve mentions from `mentionedUsers()`/`mentionedRoleIds()` and the cache, escape markdown), or a public `MessageContent.strip(Message)` helper.
16. **`User` has no `system()`.** `message.type() != MessageType.DEFAULT` in section 5 is a weaker substitute for Imperium's `author.isSystem`. **Change:** add `boolean system()` to the `User` catalogue; it is one optional boolean in the payload.
17. **`Member` has no `permissions()` and no `boostingSince()`.** `member.raw().getString("premium_since")` in section 1, and the note in section 6. The payloads doc says interactions are the only source of `member.permissions`, and the reducer table says to store it — but nothing exposes it, so the one precomputed permission set Discord hands us is only reachable through `raw()`. Booster sync is an existing Imperium feature that has no typed path at all. **Change:** `Optional<PermissionSet> permissions()` and `Optional<Instant> boostingSince()` on `Member`; `PermissionSet appPermissions()` on the interaction event.
18. **`roleIds()` is a `List`.** `Set.copyOf(member.roleIds())` in section 1. Discord role membership is a set, every use is a membership test or a diff, and the doc's own example (`event.previous().map(Member::roleIds)`) exists to be diffed. **Change:** `Set<Snowflake> roleIds()` (ordered, e.g. `LinkedHashSet`) plus `boolean hasRole(Snowflake)`.
19. **No per-message or per-channel handler ordering.** The `states.put` / `editOriginal` pair in section 3. Section 8 guarantees ordering only inside the reducer; two fast clicks give two virtual threads racing on the same message. Every paginator in existence hits this. **Change:** do not add a framework, but dispatch with an optional ordering key (channel id by default) or document the race loudly next to the `DiscordEventSource` contract.
20. **No named exception for 50007 / 403.** `if (e.code() != 50007) throw e;` in section 4. A bot that DMs users hits "cannot send messages to this user" constantly; 404 got a subtype and this did not. **Change:** `DiscordForbiddenException extends DiscordRestException` plus a `DiscordErrorCode` constant table (`CANNOT_SEND_TO_USER = 50007`, `MISSING_PERMISSIONS = 50013`, `UNKNOWN_MESSAGE = 10008`).
21. **`createTextChannel` returns the wrong static type in one of the two places it is specified.** `(GuildTextChannel) guild.createTextChannel(...)` in section 5 — the `Guild` catalogue says `GuildTextChannel`, `DiscordRest.createChannel` says `GuildChannel`. **Change:** keep the narrow type on the entity verb and give `DiscordRest` typed variants (`createTextChannel`, `createCategory`) alongside the generic one.
22. **Read-your-writes after a REST create.** Same snippet. "REST never writes the cache" is right, but `liveChatChannel` scans the cache, creates on a miss, and a second call microseconds later scans a cache that still does not contain the new channel and creates a duplicate — Discord allows duplicate channel names, so nothing catches it. **Change:** document the gap where the rule is stated, and promote `duke.events().await(Class, Predicate, Duration)` from "for tests" to the blessed idiom for "wait for my write to land".
23. **No pre-ready listener registration.** Section 2 registers listeners after `login()` returns. The doc does not say whether events dispatched in that window are dropped, which matters precisely for the thread-joining flow. **Change:** `DukecordOptions.Builder.listener(Class<E>, Consumer<E>)`, or state that dispatch is buffered until `login()` returns.
24. **`close()` has no bounded form.** Section 2. Imperium does `shutdown()` / `awaitShutdown(15s)` / `shutdownNow()`. **Change:** `close(Duration)` or `DukecordOptions.shutdownTimeout`.
25. **Single-guild library, mandatory guild id.** `duke.requireGuild(config.guild())` everywhere; Imperium's `getMainServer()` is `jda.guildCache.first()`. **Change:** `Guild requireSingleGuild()` that throws when the bot is in zero or more than one guild. Trivial, and it removes a config key from every single-guild bot.
26. **Ephemerality is settable in two places with no stated winner.** `deferReply(Ephemeral.YES)` versus `MessageCreate.builder().ephemeral()`. Discord fixes ephemerality at acknowledgement, so the builder flag is a no-op — or worse, silently dropped — on the post-defer PATCH. **Change:** say so in the consumer table: ephemerality is decided by the acknowledgement, and the flag is honoured only on `reply` and `followUp` before one.
27. **Silent allow-list drops.** Section 7.2: "keys outside the allow-list are dropped deterministically". Deterministic is not the same as discoverable; a bot that sets `tts` on an interaction reply gets no signal. **Change:** drop silently in production, but log at `DEBUG` with the key names, and throw in a strict/dev mode.

### What felt good

- **Mention safety by default.** `allowed_mentions: none` at builder construction removes Imperium's most repeated defensive call and makes the dangerous thing the explicit one. `channel.send(String)` inheriting it is exactly right.
- **`MessageEdit` not writing `attachments` unless asked.** This deletes Imperium's ugliest code outright — the re-download-and-re-upload dance around `preview2.png` that earned a "why do I have to fight discord API" comment. Worth protecting against any future "simplification".
- **`message.member()` as the merged inline payload.** One call, no cache lookup, no REST fallback, and it is the value the bridge actually wants. This is the single clearest improvement over both JDA and Discord.Net.
- **Two verbs.** After writing 400 lines of this, I never once had to ask whether a call was going to block. `guild.channel(id, GuildTextChannel.class)` reading like a map lookup and `channel.send(...)` reading like I/O is the whole value proposition, and it holds up.
- **Typed cache lookups with a `Class` witness.** `guild.channel(id, CategoryChannel.class)` beats JDA's `getCategoryById` family and every bot's `as*Channel()` casts.
- **Unchecked `DiscordRestException` and `DiscordNotFoundException`.** `catch (DiscordNotFoundException ignored)` around an expiry teardown is the entire error handling a real bot needs, and nothing forces an `IOException` catch on the happy path.
- **`Snowflake` as a record.** Usable directly as a map key for per-message state, which is the single most common bot data structure.

## Proposed interaction API

Everything marked `// PROPOSED` above, collected. Names follow the design doc's rules: nouns read, verbs block, no futures.

```java
// ---- Entry point -------------------------------------------------------------
interface Dukecord {
    CommandRegistrar commands();
}

interface CommandRegistrar {
    CommandRegistrar register(Object container);       // scans @SlashCommand / @ComponentHandler / @ModalHandler
    CommandRegistrar register(SlashCommandSpec spec, Consumer<SlashCommandEvent> handler);
    CommandRegistrar onComponent(String customId, Consumer<ComponentEvent> handler);
    CommandRegistrar onModal(String customId, Consumer<ModalSubmitEvent> handler);
    void publishGuild(Snowflake guildId);              // PUT /applications/{id}/guilds/{g}/commands, blocking
    void publishGlobal();                              // PUT /applications/{id}/commands, blocking
}

@Retention(RUNTIME) @interface SlashCommand { String name(); String description(); Rank rank() default EVERYONE; }
@Retention(RUNTIME) @interface Option { String name(); String description(); boolean required() default true; }
@Retention(RUNTIME) @interface ComponentHandler { String[] value(); /* + host-defined gate attributes */ }
@Retention(RUNTIME) @interface ModalHandler { String value(); }

// Imperative equivalent, for bots that do not want annotations.
record SlashCommandSpec(String name, String description, List<OptionSpec> options, List<SlashCommandSpec> subcommands) {
    static Builder builder(String name);
}
record OptionSpec(String name, String description, OptionType type, boolean required,
                  OptionalLong min, OptionalLong max, List<Choice> choices) {}
enum OptionType { STRING, INTEGER, NUMBER, BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT }

// ---- Events (sealed, same family as section 5.2) -----------------------------
sealed interface InteractionEvent extends DiscordEvent {
    Snowflake id();
    String token();
    User user();
    Optional<Member> member();              // guild interactions; carries permissions()
    Optional<Snowflake> guildId();
    Snowflake channelId();
    PermissionSet appPermissions();         // app_permissions, interaction-only
    Instant expiresAt();                    // token deadline, id-derived + 15 min

    DeferredReply deferReply(Ephemeral ephemeral);   // callback type 5
    void reply(MessageCreate spec);                  // callback type 4, only before acknowledgement
    void replyModal(Modal modal);                    // callback type 9, only before acknowledgement
    Message fetchOriginal();                         // GET @original
    Message followUp(MessageCreate spec);            // POST webhook
    boolean acknowledged();
}

sealed interface ComponentEvent extends InteractionEvent {
    String customId();
    Message message();                               // the message the component lives on
    DeferredEdit deferEdit();                        // callback type 6, no loading state
    void updateMessage(MessageEdit spec);            // callback type 7
}

record SlashCommandEvent(...)  implements InteractionEvent {
    String commandPath();                            // "map search"
    <T> Optional<T> option(String name, Class<T> type);   // String, Long, Boolean, User, Channel, Role, Attachment
}
record ButtonEvent(...)        implements ComponentEvent {}
record StringSelectEvent(...)  implements ComponentEvent { List<String> values(); }
record ModalSubmitEvent(...)   implements InteractionEvent {
    String customId();
    Optional<String> value(String inputId);
    Optional<Message> message();                     // present when the modal came from a component
}

enum Ephemeral { YES, NO }

// ---- Acknowledgement handles: the lifecycle lives in the type system ---------
interface DeferredReply {                            // after callback type 5
    Message send(MessageCreate spec);                // PATCH @original
    Message send(String content);
    Message edit(MessageEdit spec);                  // PATCH @original again
    Message followUp(MessageCreate spec);
    void delete();
}
interface DeferredEdit {                             // after callback type 6
    Message editOriginal(MessageEdit spec);          // PATCH @original
    Message followUp(MessageCreate spec);
}

// ---- Components (values, like Embed and Attachment) --------------------------
sealed interface Component permits ActionRow, Button, StringSelect, TextDisplay, /* v2 ... */ UnknownComponent {
    Component asDisabled();
}
record ActionRow(List<Component> children) implements Component { static ActionRow of(Component... children); }
record Button(ButtonStyle style, String customId, String label, Optional<Emoji> emoji, boolean disabled, Optional<String> url)
        implements Component {
    static Button primary(String customId, String label);
    static Button secondary(String customId, String label);
    static Button success(String customId, String label);
    static Button danger(String customId, String label);
    static Button link(String url, String label);
    Button withDisabled(boolean disabled);
    Button withEmoji(Emoji emoji);
}
record StringSelect(String customId, Optional<String> placeholder, int minValues, int maxValues,
                    List<SelectOption> options, boolean disabled) implements Component { static Builder builder(String customId); }
record SelectOption(String label, String value, Optional<String> description, Optional<Emoji> emoji, boolean isDefault) {
    static SelectOption of(String label, String value);
}

// ---- Modals ------------------------------------------------------------------
record Modal(String customId, String title, List<ActionRow> rows) { static Builder builder(String customId); }
record TextInput(String id, String label, TextInputStyle style, Optional<String> placeholder,
                 Optional<String> value, boolean required, OptionalInt minLength, OptionalInt maxLength)
        implements Component {
    static Builder shortLine(String id, String label);
    static Builder paragraph(String id, String label);
}

// ---- Gaps in the core model the interaction code leans on --------------------
interface AllowedMentions {
    static AllowedMentions none(); static AllowedMentions all();
    static AllowedMentions users(Collection<Snowflake> ids);
    static AllowedMentions roles(Collection<Snowflake> ids);
    AllowedMentions withEveryone(); AllowedMentions withRepliedUser(boolean ping);
}
interface Embed {                                   // record + builder
    static Embed.Builder builder(); static Embed.Builder builder(Embed copyOf);
    Optional<String> field(String name);
    // Builder: color, title, url, description, field(name, value), field(name, value, inline),
    //          fieldIf(condition, ...), blankField(), author(...), footer(...), image(url), thumbnail(url), timestamp
}
interface FileUpload {
    static FileUpload of(Path path); static FileUpload of(Path path, String filename);
    static FileUpload of(byte[] bytes, String filename);
    static FileUpload ofStream(String filename, Supplier<InputStream> supplier);
    FileUpload withDescription(String description);
}
interface MessageEdit {
    static MessageEdit from(MessageCreate create);               // key-level copy through the edit allow-list
    static MessageEdit disablingComponentsOf(Message message);
}
// Message:        ThreadChannel createThread(ThreadStart spec);  void disableComponents();  String strippedContent();
// Member:         Set<Snowflake> roleIds();  boolean hasRole(Snowflake);  Optional<PermissionSet> permissions();
//                 Optional<Instant> boostingSince();  void addRole(Snowflake id, String reason);
// MemberEdit:     Builder.roles(Collection<Snowflake>), nick, timeoutUntil, mute, deaf, channelId, reason
// User:           boolean system();
// ThreadChannel:  boolean joined();
// CategoryChannel:List<GuildChannel> channels();  Optional<GuildChannel> channel(String name);
// GuildChannel:   boolean inCategory(Snowflake categoryId);
// Attachment:     InputStream open();  void downloadTo(Path target);   (or DiscordRest.download(String url))
// Errors:         DiscordForbiddenException extends DiscordRestException;  DiscordErrorCode constants
// Dukecord:       Guild requireSingleGuild();  void close(Duration timeout);
```
