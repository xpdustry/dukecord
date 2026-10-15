// Imperium's Discord module, rewritten against the current Dukecord API.
//
// See docs/api-shape-imperium.md for the original design check and the Imperium sources it follows. This file
// compiles against build/classes/java/main but cannot run yet: nothing implements Dukecord. Imperium's own
// services (accounts, maps, rendering) are stubbed at the top.
//
// Sections: 1. role sync, 2. startup and commands, 3. map search paging, 4. map submit and review,
// 5. chat bridge, 6. report alerts.

import dev.phinner.dukecord.*;
import dev.phinner.dukecord.channel.*;
import dev.phinner.dukecord.component.*;
import dev.phinner.dukecord.guild.*;
import dev.phinner.dukecord.interaction.*;
import dev.phinner.dukecord.message.*;
import dev.phinner.dukecord.user.*;
import java.awt.Color;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.*;

// --- Imperium's non-Discord world, stubbed ---
enum Rank { EVERYONE, VERIFIED, MODERATOR, ADMIN }
enum Gamemode { SURVIVAL, ATTACK, PVP }
record Account(int id, Rank rank, Optional<Snowflake> discord) {}
interface Accounts { Optional<Account> byId(int id); Optional<Account> byDiscord(Snowflake id); }
interface Achievements { Set<String> completed(int accountId); }
record MindustryMap(int id, String name) {}
interface Maps { List<MindustryMap> search(Optional<String> name, Set<Gamemode> modes); }
record Config(Snowflake guild, Snowflake mapsChannel, Snowflake reportsChannel, Snowflake liveChatCategory,
              Optional<Snowflake> alertsRole, Map<Rank, Snowflake> ranks, Map<String, Snowflake> achievements) {}

final class Imperium {
    static final Color ACCENT = new Color(0xFFD37F);
    final Dukecord duke; final Config config; final Accounts accounts; final Achievements achievements; final Maps maps;
    Imperium(Dukecord duke, Config config, Accounts accounts, Achievements achievements, Maps maps) {
        this.duke = duke; this.config = config; this.accounts = accounts; this.achievements = achievements; this.maps = maps;
    }

    // 1. Role synchronization: diff from the cache, then one PATCH with the whole role list.
    void syncRoles(int accountId) {
        var account = accounts.byId(accountId).orElse(null);
        if (account == null || account.discord().isEmpty()) return;
        var member = duke.members().getByGuildAndId(config.guild(), account.discord().get()).orElse(null);
        if (member == null) return;

        var current = member.roleIds();
        var target = new HashSet<>(current);
        var unlocked = achievements.completed(accountId);
        config.achievements().forEach((name, role) -> { if (unlocked.contains(name)) target.add(role); else target.remove(role); });
        config.ranks().forEach((rank, role) -> {
            if (rank.compareTo(account.rank()) <= 0) target.add(role); else target.remove(role);
        });
        if (target.equals(current)) return;

        member.edit(MemberEditSpec.builder().roles(target).build(), "rank and achievement synchronization");
    }

    // 2. Startup: register listeners and publish the command tree.
    void install() {
        duke.events().register(GuildMemberAddEvent.class, event ->
                accounts.byDiscord(event.member().id()).ifPresent(a -> syncRoles(a.id())));
        duke.events().register(CommandInteractionEvent.class, this::onCommand);
        duke.events().register(ComponentInteractionEvent.class, this::onComponent);
        duke.events().register(ModalSubmitInteractionEvent.class, this::onModal);
        duke.events().register(MessageCreateEvent.class, this::onBridgeMessage);

        duke.commands().rest().overwriteGuild(config.guild(), List.of(
                CommandCreateSpec.chatInput("sync-roles", "Synchronize your Discord roles with your CN account"),
                CommandCreateSpec.chatInput("map", "Maps",
                        CommandOption.subcommand("search", "Search the map pool",
                                CommandOption.string("name", "Name filter")),
                        CommandOption.subcommand("submit", "Submit a map for review",
                                CommandOption.attachment("map", "The .msav file", true),
                                CommandOption.string("notes", "Notes for the reviewers")))));
    }

    void onCommand(CommandInteractionEvent event) {
        switch (event.data().path()) {
            case "sync-roles" -> {
                var reply = event.deferReply(true);
                var account = accounts.byDiscord(event.invoker().id()).orElse(null);
                if (account == null) { reply.edit("You are not linked to a CN account."); return; }
                syncRoles(account.id());
                reply.edit("Your Discord roles have been synchronized.");
            }
            case "map search" -> onMapSearch(event);
            case "map submit" -> onMapSubmit(event);
            default -> event.reply(MessageCreateSpec.ephemeral("Unknown command."));
        }
    }

    // 3. Map search: one page body for the first reply and every update.
    static final String PREV = "map-search-previous:1", NEXT = "map-search-next:1", MODES = "map-search-gamemode:1";
    record State(Optional<String> query, int page, Set<Gamemode> modes, Snowflake owner) {}
    final Map<Snowflake, State> states = new ConcurrentHashMap<>();

    void onMapSearch(CommandInteractionEvent event) {
        var reply = event.deferReply();
        var state = new State(event.data().stringOption("name"), 0, Set.of(), event.invoker().id());
        Message message = reply.edit(page(state));
        states.put(message.id(), state);
    }

    void onComponent(ComponentInteractionEvent event) {
        if (event.customId().equals(ACCEPT) || event.customId().equals(REJECT)) { onReview(event); return; }
        var state = states.get(event.message().id());
        if (state == null) { event.reply(MessageCreateSpec.ephemeral("This button has expired.")); return; }
        if (!state.owner().equals(event.invoker().id())) {
            event.reply(MessageCreateSpec.ephemeral("This button is not for you.")); return;
        }
        UnaryOperator<State> change = switch (event.customId()) {
            case PREV -> s -> new State(s.query(), s.page() - 1, s.modes(), s.owner());
            case NEXT -> s -> new State(s.query(), s.page() + 1, s.modes(), s.owner());
            case MODES -> s -> new State(s.query(), 0,
                    event.data().values().stream().map(Gamemode::valueOf).collect(Collectors.toSet()), s.owner());
            default -> s -> s;
        };
        var next = change.apply(state);
        states.put(event.message().id(), next);
        event.update(page(next));
    }

    MessageEditSpec page(State state) {
        var result = maps.search(state.query(), state.modes());
        var last = Math.max(0, (result.size() - 1) / 15);
        var embed = Embed.builder()
                .color(ACCENT)
                .description(result.stream().skip(state.page() * 15L).limit(15)
                        .map(m -> "- " + m.name()).collect(Collectors.joining("\n", "", "")))
                .build();
        var paging = ActionRow.of(
                Button.primary(PREV, "Previous").withDisabled(state.page() == 0),
                Button.secondary("page", (state.page() + 1) + " / " + (last + 1)).withDisabled(true),
                Button.primary(NEXT, "Next").withDisabled(state.page() == last));
        var modes = ActionRow.of(StringSelectMenu.builder()
                .customId(MODES)
                .placeholder("Gamemode")
                .minValues(0)
                .maxValues(Gamemode.values().length)
                .options(Stream.of(Gamemode.values())
                        .map(g -> StringSelectMenu.Option.builder()
                                .label(g.name().toLowerCase()).value(g.name()).selected(state.modes().contains(g)).build())
                        .toList())
                .build());
        return MessageEditSpec.builder().embeds(List.of(embed)).components(List.of(paging, modes)).build();
    }

    // Expiry: disable every button of a message we posted.
    void disableComponents(Message message) {
        message.edit(MessageEditSpec.builder()
                .components(message.components().stream().map(c -> c.withDisabled(true)).toList())
                .build());
    }

    // 4. Map submit: attachment option, two uploads, embed image via attachment://, review buttons, reject modal.
    static final String ACCEPT = "map-submission-accept:2", REJECT = "map-submission-reject:2", REASON = "map-submission-reason:2";

    void onMapSubmit(CommandInteractionEvent event) {
        var reply = event.deferReply();
        Attachment map = event.data().attachmentOption("map").orElseThrow();
        if (!map.filename().endsWith(".msav")) { reply.edit("Invalid map file!"); return; }

        Path file = download(map);
        Message submission = MessageChannelBehavior.of(duke, config.mapsChannel()).send(MessageCreateSpec.builder()
                .addFiles(FileUpload.of("map.msav", readAll(file)), FileUpload.of("preview.png", render(file)))
                .addEmbeds(Embed.builder()
                        .color(ACCENT)
                        .title("Map Submission")
                        .addFields(Embed.Field.of("Submitter", Mentions.user(event.invoker().id())))
                        .addFields(Embed.Field.of("Notes", event.data().stringOption("notes").orElse("none")))
                        .image(Embed.Media.of("attachment://preview.png"))
                        .build())
                .addComponents(ActionRow.of(
                        Button.success(ACCEPT, "Accept").withEmoji(Emoji.unicode("✅")),
                        Button.danger(REJECT, "Reject").withEmoji(Emoji.unicode("🗑"))))
                .build());
        submission.createThread(ThreadCreateSpec.builder().name("Comments for " + map.filename())
                .autoArchiveDuration(ThreadCreateSpec.ArchiveDuration.THREE_DAYS).build());
        reply.edit("Your map has been submitted for review.");
    }

    void onReview(ComponentInteractionEvent event) {
        if (event.customId().equals(REJECT)) {
            event.showModal(ModalCreateSpec.of(REASON + ":" + event.message().id(), "Reject map submission",
                    Label.of("Reason", TextInput.builder().customId("reason").style(TextInput.Style.PARAGRAPH)
                            .required(true).maxLength(400).build())));
            return;
        }
        event.deferUpdate();
        finishReview(event.message(), event.invoker(), Color.GREEN, "accepted");
    }

    void onModal(ModalSubmitInteractionEvent event) {
        if (!event.customId().startsWith(REASON)) return;
        event.deferUpdate();
        var reason = event.data().value("reason").orElse("no reason given");
        finishReview(event.message().orElseThrow(), event.invoker(), Color.RED, "rejected: " + reason);
    }

    void finishReview(Message submission, User reviewer, Color color, String verb) {
        var old = submission.embeds().getFirst();
        submission.edit(MessageEditSpec.builder()
                .embeds(List.of(Embed.builder().from(old).color(color)
                        .addFields(Embed.Field.of("Reviewer", Mentions.user(reviewer.id()))).build()))
                .components(List.of())          // removes the buttons, keeps both uploads
                .build());
        var submitter = Snowflake.of(old.fields().getFirst().value().replaceAll("\\D", ""));
        try {
            UserBehavior.of(duke, submitter).createDm().send("Your map submission has been " + verb + ".");
        } catch (DiscordRestException e) {
            if (e.code() != 50007) throw e;     // DMs closed
        }
    }

    // 5. Chat bridge: filter, check the category, forward the content.
    void onBridgeMessage(MessageCreateEvent event) {
        var message = event.message();
        if (message.webhookId().isPresent() || message.author().bot() || message.author().system()) return;
        var channel = duke.channels().getById(message.channelId()).orElse(null);
        if (!(channel instanceof GuildTextChannel text)) return;
        if (text.parent().map(ChannelBehavior::id).filter(config.liveChatCategory()::equals).isEmpty()) return;
        var name = message.member().map(Member::displayName).orElse(message.author().username());
        System.out.println("[" + text.name() + "] " + name + ": " + message.content());
    }

    /** Finds the per-server channel under the live-chat category, or creates it. */
    GuildTextChannel liveChatChannel(String server) {
        var category = (CategoryChannel) duke.channels().getById(config.liveChatCategory()).orElseThrow();
        return category.channels().stream()
                .filter(c -> c instanceof GuildTextChannel && c.name().equals(server))
                .map(GuildTextChannel.class::cast)
                .findFirst()
                .orElseGet(() -> category.guild().createTextChannel(
                        ChannelCreateSpec.builder().name(server).parentId(category.id()).build()));
    }

    // 6. Report alert: a deliberate role ping against the mention-safe default.
    void onReport(String target, String reason) {
        var mentions = AllowedMentions.builder();
        config.alertsRole().ifPresent(mentions::addRoles);
        MessageChannelBehavior.of(duke, config.reportsChannel()).send(MessageCreateSpec.builder()
                .content(config.alertsRole().map(Mentions::role).orElse(""))
                .allowedMentions(mentions.build())
                .addEmbeds(Embed.builder().color(Color.YELLOW)
                        .addFields(Embed.Field.of("Target", target, true), Embed.Field.of("Reason", reason))
                        .build())
                .addComponents(ActionRow.of(Button.primary("report-review:1", "Review"), Button.secondary("report-ignore:1", "Ignore")))
                .build());
    }

    Path download(Attachment attachment) {
        try (var in = duke.messages().rest().download(attachment)) {
            var file = Files.createTempFile("submission", ".msav");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
    static byte[] readAll(Path file) { throw new UnsupportedOperationException(); }
    static byte[] render(Path file) { throw new UnsupportedOperationException(); }
}
