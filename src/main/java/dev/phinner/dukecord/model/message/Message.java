// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.DiscordEntityRefList;
import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.channel.MessageChannel;
import dev.phinner.dukecord.model.channel.ThreadChannel;
import dev.phinner.dukecord.model.channel.ThreadCreateSpec;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.model.user.User;
import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public interface Message extends DiscordEntity {

    DiscordEntityRef<MessageChannel> channel();

    /** Empty for direct messages. */
    Optional<DiscordEntityRef<Guild>> guild();

    /** When {@link #isFromWebhook()}, this describes the webhook, not a user. */
    User author();

    boolean isFromWebhook();

    /** The inline member from the gateway payload, merged with {@link #author()}; never a cache lookup. */
    Optional<Member> member();

    /** Empty without the {@code MESSAGE_CONTENT} intent; check {@code duke().intents()} to tell the two apart. */
    String content();

    /** {@link #content()} with mentions resolved through the cache to plain names and markdown escaped. */
    String strippedContent();

    Instant timestamp();

    Optional<Instant> editedAt();

    MessageType type();

    EnumSet<MessageFlag> flags();

    boolean isTts();

    boolean isPinned();

    List<User> mentionedUsers();

    DiscordEntityRefList<Role> mentionedRoles();

    boolean mentionsEveryone();

    List<Attachment> attachments();

    List<Embed> embeds();

    List<Component> components();

    Optional<MessageReference> reference();

    ReferencedMessage referencedMessage();

    default URI jumpUrl() {
        final var guild = this.guild().map(ref -> ref.id().asString()).orElse("@me");
        return URI.create(
                "https://discord.com/channels/" + guild + "/" + this.channel().id() + "/" + this.id());
    }

    // ---- REST ----

    default Message reply(final MessageCreateSpec spec) {
        return this.duke()
                .rest()
                .sendMessage(
                        this.channel().id(),
                        MessageCreateSpec.builder(spec)
                                .reference(this.id(), this.channel().id(), false)
                                .build());
    }

    default Message reply(final String content) {
        return this.reply(MessageCreateSpec.builder().content(content).build());
    }

    default Message edit(final MessageEditSpec spec) {
        return this.duke().rest().editMessage(this.channel().id(), this.id(), spec);
    }

    default void delete() {
        this.duke().rest().deleteMessage(this.channel().id(), this.id());
    }

    default void delete(final String reason) {
        this.duke().rest().deleteMessage(this.channel().id(), this.id(), reason);
    }

    /** Edits the message so every component is disabled; the usual teardown for expired interactive messages. */
    default void disableComponents() {
        this.edit(MessageEditSpec.disablingComponentsOf(this));
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec) {
        return this.duke().rest().createThread(this.channel().id(), this.id(), spec);
    }

    default void addReaction(final Emoji emoji) {
        this.duke().rest().addReaction(this.channel().id(), this.id(), emoji);
    }

    default void pin() {
        this.duke().rest().pinMessage(this.channel().id(), this.id());
    }

    default void unpin() {
        this.duke().rest().unpinMessage(this.channel().id(), this.id());
    }
}
