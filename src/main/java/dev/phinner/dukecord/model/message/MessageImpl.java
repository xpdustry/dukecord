// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.DiscordEntityRefList;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channels;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.channel.MessageChannel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Guilds;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Members;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.model.guild.Roles;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MessageImpl extends AbstractDiscordEntity implements Message {

    private static final Pattern MENTION = Pattern.compile("<(@[!&]?|#|a?:[A-Za-z0-9_~]+:)(\\d{15,20})>");
    private static final Pattern MARKDOWN = Pattern.compile("([*_~`|\\\\>])");

    private final DiscordEntityRef<MessageChannel> channel;
    private final Optional<DiscordEntityRef<Guild>> guild;
    private final User author;
    private final boolean fromWebhook;
    private final Optional<Member> member;
    private final String content;
    private final Instant timestamp;
    private final Optional<Instant> editedAt;
    private final MessageType type;
    private final EnumSet<MessageFlag> flags;
    private final boolean tts;
    private final boolean pinned;
    private final List<User> mentionedUsers;
    private final DiscordEntityRefList<Role> mentionedRoles;
    private final boolean mentionsEveryone;
    private final List<Attachment> attachments;
    private final List<Embed> embeds;
    private final List<Component> components;
    private final Optional<MessageReference> reference;
    private final ReferencedMessage referencedMessage;

    MessageImpl(final Dukecord duke, final Json raw) {
        super(duke, raw);
        final var channelId = raw.snowflake("channel_id");
        final var guildId = raw.optionalSnowflake("guild_id");
        this.channel = Channels.ref(duke, MessageChannel.class, channelId);
        this.guild = guildId.map(id -> Guilds.ref(duke, id));
        this.author = Users.fromJson(duke, raw.object("author"));
        this.fromWebhook = raw.isPresent("webhook_id");
        this.member = guildId.flatMap(
                id -> raw.optionalObject("member").map(m -> Members.fromJson(duke, id, m, this.author)));
        this.content = raw.optionalString("content").orElse("");
        this.timestamp = raw.instant("timestamp");
        this.editedAt = raw.optionalInstant("edited_timestamp");
        this.type = MessageType.fromCode(raw.optionalInteger("type").orElse(0));
        this.flags = MessageFlag.fromBits(raw.optionalInteger("flags").orElse(0));
        this.tts = raw.bool("tts", false);
        this.pinned = raw.bool("pinned", false);
        this.mentionedUsers = raw.objects("mentions").stream()
                .map(mention -> Users.fromJson(duke, mention))
                .toList();
        this.mentionedRoles = guildId.map(id -> Roles.refs(duke, id, raw.snowflakes("mention_roles")))
                .orElseGet(() -> DiscordEntityRefList.of(
                        Role.class,
                        raw.snowflakes("mention_roles"),
                        id -> duke.cache().role(id),
                        id -> {
                            throw new IllegalStateException("Cannot fetch role " + id + " outside of a guild");
                        }));
        this.mentionsEveryone = raw.bool("mention_everyone", false);
        this.attachments =
                raw.objects("attachments").stream().map(Attachment::fromJson).toList();
        this.embeds = raw.objects("embeds").stream().map(Embed::fromJson).toList();
        this.components =
                raw.objects("components").stream().map(Component::fromJson).toList();
        this.reference = raw.optionalObject("message_reference").map(MessageReference::fromJson);
        this.referencedMessage = !raw.has("referenced_message")
                ? ReferencedMessage.UNKNOWN
                : raw.optionalObject("referenced_message")
                        .<ReferencedMessage>map(
                                referenced -> new ReferencedMessage.Present(new MessageImpl(duke, referenced)))
                        .orElse(ReferencedMessage.DELETED);
    }

    @Override
    public DiscordEntityRef<MessageChannel> channel() {
        return this.channel;
    }

    @Override
    public Optional<DiscordEntityRef<Guild>> guild() {
        return this.guild;
    }

    @Override
    public User author() {
        return this.author;
    }

    @Override
    public boolean isFromWebhook() {
        return this.fromWebhook;
    }

    @Override
    public Optional<Member> member() {
        return this.member;
    }

    @Override
    public String content() {
        return this.content;
    }

    @Override
    public String strippedContent() {
        final var matcher = MENTION.matcher(this.content);
        final var result = new StringBuilder();
        while (matcher.find()) {
            final var kind = matcher.group(1);
            final var id = Snowflake.of(matcher.group(2));
            matcher.appendReplacement(result, Matcher.quoteReplacement(this.resolveMention(kind, id)));
        }
        matcher.appendTail(result);
        return MARKDOWN.matcher(result).replaceAll("\\\\$1");
    }

    private String resolveMention(final String kind, final Snowflake id) {
        if (kind.startsWith("@&")) {
            return "@" + this.duke().cache().role(id).map(Role::name).orElse("deleted-role");
        }
        if (kind.startsWith("@")) {
            final var member = this.guild().flatMap(guild -> this.duke().cache().member(guild.id(), id));
            return "@"
                    + member.map(Member::displayName)
                            .or(() -> this.mentionedUsers.stream()
                                    .filter(user -> user.id().equals(id))
                                    .findFirst()
                                    .map(User::displayName))
                            .or(() -> this.duke().cache().user(id).map(User::displayName))
                            .orElse("unknown-user");
        }
        if (kind.equals("#")) {
            return "#"
                    + this.duke()
                            .cache()
                            .channel(id, GuildChannel.class)
                            .map(GuildChannel::name)
                            .orElse("deleted-channel");
        }
        return kind.substring(kind.indexOf(':'));
    }

    @Override
    public Instant timestamp() {
        return this.timestamp;
    }

    @Override
    public Optional<Instant> editedAt() {
        return this.editedAt;
    }

    @Override
    public MessageType type() {
        return this.type;
    }

    @Override
    public EnumSet<MessageFlag> flags() {
        return EnumSet.copyOf(this.flags);
    }

    @Override
    public boolean isTts() {
        return this.tts;
    }

    @Override
    public boolean isPinned() {
        return this.pinned;
    }

    @Override
    public List<User> mentionedUsers() {
        return this.mentionedUsers;
    }

    @Override
    public DiscordEntityRefList<Role> mentionedRoles() {
        return this.mentionedRoles;
    }

    @Override
    public boolean mentionsEveryone() {
        return this.mentionsEveryone;
    }

    @Override
    public List<Attachment> attachments() {
        return this.attachments;
    }

    @Override
    public List<Embed> embeds() {
        return this.embeds;
    }

    @Override
    public List<Component> components() {
        return this.components;
    }

    @Override
    public Optional<MessageReference> reference() {
        return this.reference;
    }

    @Override
    public ReferencedMessage referencedMessage() {
        return this.referencedMessage;
    }

    @Override
    public String toString() {
        return "Message[" + this.channel.id() + "/" + this.id() + "]";
    }
}
