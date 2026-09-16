// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.JsonException;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.MessageChannel;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.support.TestDukecord;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class MessagesTest {

    private static final Snowflake GUILD_ID = Snowflake.of("1000000000000000001");
    private static final Snowflake CHANNEL_ID = Snowflake.of("1200000000000000001");
    private static final Snowflake MESSAGE_ID = Snowflake.of("1300000000000000001");
    private static final Snowflake AUTHOR_ID = Snowflake.of("1400000000000000001");
    private static final Snowflake ROLE_ID = Snowflake.of("1500000000000000001");
    private static final Snowflake BOB_ID = Snowflake.of("1600000000000000001");

    private static final String GUILD_MESSAGE = """
            {
              "id": "1300000000000000001",
              "channel_id": "1200000000000000001",
              "guild_id": "1000000000000000001",
              "author": {"id": "1400000000000000001", "username": "alice", "global_name": "Alice"},
              "member": {
                "nick": "Al",
                "roles": ["1500000000000000001"],
                "joined_at": "2026-01-01T00:00:00Z",
                "flags": 0
              },
              "content": "hello <@1600000000000000001>",
              "timestamp": "2026-09-15T12:00:00Z",
              "edited_timestamp": "2026-09-15T12:05:00Z",
              "type": 19,
              "flags": 4,
              "tts": true,
              "pinned": true,
              "mention_everyone": true,
              "mentions": [{"id": "1600000000000000001", "username": "bob"}],
              "mention_roles": ["1500000000000000001"],
              "attachments": [
                {
                  "id": "1900000000000000001",
                  "filename": "map.msav",
                  "size": 2048,
                  "url": "https://cdn.discordapp.com/attachments/1/2/map.msav",
                  "proxy_url": "https://media.discordapp.net/attachments/1/2/map.msav"
                }
              ],
              "embeds": [{"title": "Server", "description": "started"}],
              "components": [
                {"type": 1, "components": [{"type": 2, "style": 1, "custom_id": "go", "label": "Go"}]}
              ],
              "message_reference": {
                "type": 0,
                "message_id": "1300000000000000000",
                "channel_id": "1200000000000000001",
                "guild_id": "1000000000000000001"
              },
              "referenced_message": {
                "id": "1300000000000000000",
                "channel_id": "1200000000000000001",
                "author": {"id": "1600000000000000001", "username": "bob"},
                "content": "the question",
                "timestamp": "2026-09-15T11:59:00Z"
              }
            }
            """;

    private static final String DM_MESSAGE = """
            {
              "id": "1300000000000000002",
              "channel_id": "1800000000000000001",
              "author": {"id": "1400000000000000001", "username": "alice"},
              "timestamp": "2026-09-15T12:00:00Z",
              "mention_roles": ["1500000000000000001"]
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private Message guildMessage() {
        return Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE));
    }

    private Message dmMessage() {
        return Messages.fromJson(this.duke, Json.parse(DM_MESSAGE));
    }

    @Test
    void parsesTheRequiredFields() {
        final var message = this.guildMessage();

        assertThat(message.id()).isEqualTo(MESSAGE_ID);
        assertThat(message.channel().id()).isEqualTo(CHANNEL_ID);
        assertThat(message.channel().type()).isEqualTo(MessageChannel.class);
        assertThat(message.timestamp()).isEqualTo(Instant.parse("2026-09-15T12:00:00Z"));
        assertThat(message.duke()).isSameAs(this.duke);
    }

    @Test
    void rejectsAPayloadWithoutTheRequiredFields() {
        final var noTimestamp = Json.parse("""
                {
                  "id": "1300000000000000001",
                  "channel_id": "1200000000000000001",
                  "author": {"id": "1400000000000000001", "username": "alice"}
                }
                """);
        final var noAuthor = Json.parse("""
                {
                  "id": "1300000000000000001",
                  "channel_id": "1200000000000000001",
                  "timestamp": "2026-09-15T12:00:00Z"
                }
                """);
        final var noChannel = Json.parse("""
                {
                  "id": "1300000000000000001",
                  "author": {"id": "1400000000000000001", "username": "alice"},
                  "timestamp": "2026-09-15T12:00:00Z"
                }
                """);

        assertThatThrownBy(() -> Messages.fromJson(this.duke, noTimestamp))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("timestamp");
        assertThatThrownBy(() -> Messages.fromJson(this.duke, noAuthor))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("author");
        assertThatThrownBy(() -> Messages.fromJson(this.duke, noChannel))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("channel_id");
    }

    @Test
    void channelReferenceResolvesThroughTheCache() {
        final var channel = this.duke.putChannel(Json.parse("""
                {
                  "id": "1200000000000000001",
                  "type": 0,
                  "guild_id": "1000000000000000001",
                  "name": "general"
                }
                """));

        assertThat(this.guildMessage().channel().get()).get().isSameAs(channel);
    }

    @Test
    void exposesTheGuildReferenceOnlyForGuildMessages() {
        assertThat(this.guildMessage().guild())
                .get()
                .extracting(ref -> ref.id())
                .isEqualTo(GUILD_ID);
        assertThat(this.dmMessage().guild()).isEmpty();
    }

    @Test
    void parsesTheAuthor() {
        final var author = this.guildMessage().author();

        assertThat(author.id()).isEqualTo(AUTHOR_ID);
        assertThat(author.username()).isEqualTo("alice");
        assertThat(author.displayName()).isEqualTo("Alice");
        assertThat(this.guildMessage().isFromWebhook()).isFalse();
    }

    @Test
    void isFromWebhookWhenTheWebhookIdIsPresent() {
        final var message =
                Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).with("webhook_id", "1900000000000000009"));

        assertThat(message.isFromWebhook()).isTrue();
    }

    @Test
    void mergesTheInlineMemberWithTheAuthor() {
        final var message = this.guildMessage();

        assertThat(message.member()).isPresent();
        final var member = message.member().orElseThrow();
        assertThat(member.id()).isEqualTo(AUTHOR_ID);
        assertThat(member.user()).isEqualTo(message.author());
        assertThat(member.raw().object("user")).isEqualTo(message.author().raw());
        assertThat(member.nickname()).contains("Al");
        assertThat(member.displayName()).isEqualTo("Al");
        assertThat(member.guild().id()).isEqualTo(GUILD_ID);
        assertThat(member.roles().ids()).containsExactly(ROLE_ID);
    }

    @Test
    void hasNoMemberWithoutAGuildOrWithoutAnInlineMemberObject() {
        assertThat(this.dmMessage().member()).isEmpty();
        assertThat(Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).without("member"))
                        .member())
                .isEmpty();
        assertThat(Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).without("guild_id"))
                        .member())
                .isEmpty();
    }

    @Test
    void contentDefaultsToEmpty() {
        assertThat(this.guildMessage().content()).isEqualTo("hello <@1600000000000000001>");
        assertThat(this.dmMessage().content()).isEmpty();
    }

    @Test
    void parsesTheTimestamps() {
        assertThat(this.guildMessage().editedAt()).contains(Instant.parse("2026-09-15T12:05:00Z"));
        assertThat(this.dmMessage().editedAt()).isEmpty();
        assertThat(Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).with("edited_timestamp", null))
                        .editedAt())
                .isEmpty();
    }

    @Test
    void decodesTheTypeAndTheFlags() {
        assertThat(this.guildMessage().type()).isEqualTo(MessageType.REPLY);
        assertThat(this.guildMessage().flags()).containsExactly(MessageFlag.SUPPRESS_EMBEDS);
        assertThat(this.dmMessage().type()).isEqualTo(MessageType.DEFAULT);
        assertThat(this.dmMessage().flags()).isEmpty();
    }

    @Test
    void decodesAnUnmappedTypeCodeAsUnknown() {
        final var message =
                Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).with("type", 250));

        assertThat(message.type()).isEqualTo(MessageType.UNKNOWN);
    }

    @Test
    void ttsAndPinnedDefaultToFalse() {
        assertThat(this.guildMessage().isTts()).isTrue();
        assertThat(this.guildMessage().isPinned()).isTrue();
        assertThat(this.dmMessage().isTts()).isFalse();
        assertThat(this.dmMessage().isPinned()).isFalse();
    }

    @Test
    void parsesTheMentionedUsers() {
        assertThat(this.guildMessage().mentionedUsers())
                .extracting(User::id, User::username)
                .containsExactly(tuple(BOB_ID, "bob"));
        assertThat(this.guildMessage().mentionsEveryone()).isTrue();
        assertThat(this.dmMessage().mentionedUsers()).isEmpty();
        assertThat(this.dmMessage().mentionsEveryone()).isFalse();
    }

    @Test
    void mentionedRolesCarryTheirIdsAndResolveThroughTheCache() {
        final var message = this.guildMessage();
        assertThat(message.mentionedRoles().ids()).containsExactly(ROLE_ID);
        assertThat(message.mentionedRoles().get()).isEmpty();

        final var role = this.duke.putRole(GUILD_ID, Json.parse("""
                        {"id": "1500000000000000001", "name": "staff", "position": 2, "permissions": "0"}
                        """));

        assertThat(message.mentionedRoles().get()).containsExactly(role);
    }

    @Test
    void mentionedRolesCannotBeFetchedOutsideAGuild() {
        final var roles = this.dmMessage().mentionedRoles();

        assertThat(roles.ids()).containsExactly(ROLE_ID);
        assertThatThrownBy(roles::fetch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outside of a guild");
    }

    @Test
    void parsesAttachmentsEmbedsAndComponents() {
        final var message = this.guildMessage();

        assertThat(message.attachments())
                .singleElement()
                .extracting(Attachment::filename, Attachment::size)
                .containsExactly("map.msav", 2048L);
        assertThat(message.embeds()).hasSize(1);
        assertThat(message.embeds().getFirst().title()).contains("Server");
        assertThat(message.components()).singleElement().isInstanceOf(ActionRow.class);
        assertThat(((ActionRow) message.components().getFirst()).components())
                .singleElement()
                .isInstanceOf(Button.class);
        assertThat(this.dmMessage().attachments()).isEmpty();
        assertThat(this.dmMessage().embeds()).isEmpty();
        assertThat(this.dmMessage().components()).isEmpty();
    }

    @Test
    void parsesTheMessageReference() {
        assertThat(this.guildMessage().reference())
                .contains(new MessageReference(
                        MessageReference.Type.DEFAULT,
                        Optional.of(Snowflake.of("1300000000000000000")),
                        Optional.of(CHANNEL_ID),
                        Optional.of(GUILD_ID)));
        assertThat(this.dmMessage().reference()).isEmpty();
    }

    @Test
    void referencedMessageIsUnknownWhenAbsentDeletedWhenNullAndPresentOtherwise() {
        assertThat(Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).without("referenced_message"))
                        .referencedMessage())
                .isEqualTo(ReferencedMessage.UNKNOWN);
        assertThat(Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).with("referenced_message", null))
                        .referencedMessage())
                .isEqualTo(ReferencedMessage.DELETED);

        final var referenced = this.guildMessage().referencedMessage();
        assertThat(referenced).isInstanceOf(ReferencedMessage.Present.class);
        assertThat(((ReferencedMessage.Present) referenced).message().content()).isEqualTo("the question");
        assertThat(((ReferencedMessage.Present) referenced).message().guild()).isEmpty();
    }

    @Test
    void buildsTheJumpUrlForGuildAndDirectMessages() {
        assertThat(this.guildMessage().jumpUrl())
                .isEqualTo(URI.create(
                        "https://discord.com/channels/1000000000000000001/1200000000000000001/1300000000000000001"));
        assertThat(this.dmMessage().jumpUrl())
                .isEqualTo(URI.create("https://discord.com/channels/@me/1800000000000000001/1300000000000000002"));
    }

    @Test
    void comparesByRawJsonAndNamesItselfByChannelAndId() {
        final var message = this.guildMessage();

        assertThat(message).isEqualTo(this.guildMessage()).hasSameHashCodeAs(this.guildMessage());
        assertThat(message).isNotEqualTo(this.dmMessage());
        assertThat(message)
                .isNotEqualTo(
                        Messages.fromJson(this.duke, Json.parse(GUILD_MESSAGE).with("content", "other")));
        assertThat(message).hasToString("Message[1200000000000000001/1300000000000000001]");
    }

    @Test
    void exposesTheInlineMemberWithoutReadingTheCache() {
        final var message = this.guildMessage();

        assertThat(this.duke.cache().member(GUILD_ID, AUTHOR_ID)).isEmpty();
        assertThat(message.member().map(Member::displayName)).contains("Al");
        assertThat(this.duke.restCalls()).isEmpty();
    }
}
