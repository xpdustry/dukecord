// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import dev.phinner.dukecord.model.message.MessageEditSpec;
import dev.phinner.dukecord.model.message.MessageQuery;
import dev.phinner.dukecord.model.message.Messages;
import dev.phinner.dukecord.model.permission.Permission;
import dev.phinner.dukecord.model.permission.PermissionOverwrite;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.support.TestDukecord;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuildTextChannelTest {

    private static final Snowflake GUILD_ID = Snowflake.of("1000000000000000001");
    private static final Snowflake CATEGORY_ID = Snowflake.of("1100000000000000001");
    private static final Snowflake CHANNEL_ID = Snowflake.of("1200000000000000001");
    private static final Snowflake MESSAGE_ID = Snowflake.of("1300000000000000001");
    private static final Snowflake USER_ID = Snowflake.of("1400000000000000001");

    private static final String MINIMAL_TEXT_CHANNEL = """
            {
              "id": "1200000000000000001",
              "type": 0,
              "guild_id": "1000000000000000001",
              "name": "general"
            }
            """;

    private static final String ANNOUNCEMENT_CHANNEL = """
            {
              "id": "1200000000000000002",
              "type": 5,
              "guild_id": "1000000000000000001",
              "name": "news"
            }
            """;

    private static final String GUILD = """
            {
              "id": "1000000000000000001",
              "name": "Test Guild",
              "owner_id": "1900000000000000001",
              "features": []
            }
            """;

    private static final String EVERYONE_ROLE = """
            {
              "id": "1000000000000000001",
              "name": "@everyone",
              "position": 0,
              "permissions": "3072"
            }
            """;

    private static final String MEMBER = """
            {
              "user": {"id": "1400000000000000001", "username": "alice"},
              "roles": [],
              "joined_at": "2026-01-01T00:00:00Z",
              "flags": 0
            }
            """;

    private static final String MESSAGE = """
            {
              "id": "1300000000000000001",
              "channel_id": "1200000000000000001",
              "guild_id": "1000000000000000001",
              "author": {"id": "1400000000000000001", "username": "alice"},
              "content": "hi",
              "timestamp": "2026-09-15T12:00:00Z"
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private GuildTextChannel channel() {
        return this.channel(0);
    }

    private GuildTextChannel channel(final int flags) {
        return (GuildTextChannel) Channels.fromJson(this.duke, Json.parse("""
                        {
                          "id": "1200000000000000001",
                          "type": 0,
                          "guild_id": "1000000000000000001",
                          "name": "general",
                          "position": 3,
                          "parent_id": "1100000000000000001",
                          "topic": "welcome here",
                          "nsfw": true,
                          "rate_limit_per_user": 30,
                          "last_message_id": "1300000000000000001",
                          "flags": %d,
                          "permission_overwrites": [
                            {"id": "1000000000000000001", "type": 0, "allow": "0", "deny": "2048"},
                            {"id": "1400000000000000001", "type": 1, "allow": "8192", "deny": "0"}
                          ]
                        }
                        """.formatted(flags)));
    }

    private GuildTextChannel minimalChannel() {
        return (GuildTextChannel) Channels.fromJson(this.duke, Json.parse(MINIMAL_TEXT_CHANNEL));
    }

    // ---- GuildChannel common fields ----

    @Test
    void exposesTheGuildReferenceTheNameAndThePosition() {
        final var channel = this.channel();

        assertThat(channel.guild().id()).isEqualTo(GUILD_ID);
        assertThat(channel.guild().get()).isEmpty();
        assertThat(channel.name()).isEqualTo("general");
        assertThat(channel.position()).isEqualTo(3);

        final var guild = this.duke.putGuild(Json.parse(GUILD));
        assertThat(channel.guild().get()).get().isSameAs(guild);
    }

    @Test
    void exposesTheParentCategoryReference() {
        final var channel = this.channel();

        assertThat(channel.category()).get().extracting(ref -> ref.id()).isEqualTo(CATEGORY_ID);
        assertThat(channel.isInCategory(CATEGORY_ID)).isTrue();
        assertThat(channel.isInCategory(GUILD_ID)).isFalse();
    }

    @Test
    void hasNoCategoryWhenParentIdIsAbsent() {
        final var channel = this.minimalChannel();

        assertThat(channel.category()).isEmpty();
        assertThat(channel.isInCategory(CATEGORY_ID)).isFalse();
        assertThat(channel.position()).isZero();
    }

    @Test
    void parsesPermissionOverwrites() {
        final var channel = this.channel();

        assertThat(channel.overwrites()).hasSize(2);
        assertThat(channel.overwrites().getFirst())
                .isEqualTo(new PermissionOverwrite(
                        GUILD_ID,
                        PermissionOverwrite.Type.ROLE,
                        PermissionSet.NONE,
                        PermissionSet.of(Permission.SEND_MESSAGES)));
        assertThat(channel.overwrites().getLast().type()).isEqualTo(PermissionOverwrite.Type.MEMBER);
        assertThat(this.minimalChannel().overwrites()).isEmpty();
    }

    @Test
    void readsTheObfuscationFlag() {
        assertThat(this.channel().flags()).isZero();
        assertThat(this.channel().isObfuscated()).isFalse();
        assertThat(this.channel(GuildChannel.OBFUSCATED_FLAG).flags()).isEqualTo(131072);
        assertThat(this.channel(GuildChannel.OBFUSCATED_FLAG).isObfuscated()).isTrue();
        assertThat(this.channel(1 << 16).isObfuscated()).isFalse();
        assertThat(this.minimalChannel().flags()).isZero();
    }

    @Test
    void permissionsOfDelegatesToTheGuild() {
        this.duke.putGuild(Json.parse(GUILD));
        this.duke.putRole(GUILD_ID, Json.parse(EVERYONE_ROLE));
        final var member = this.duke.putMember(GUILD_ID, Json.parse(MEMBER));

        assertThat(this.channel().permissionsOf(member).asSet())
                .containsExactlyInAnyOrder(Permission.VIEW_CHANNEL, Permission.MANAGE_MESSAGES);
    }

    @Test
    void permissionsOfFailsWhenTheGuildIsNotCached() {
        final var member = this.duke.putMember(GUILD_ID, Json.parse(MEMBER));
        final var channel = this.channel();

        assertThatThrownBy(() -> channel.permissionsOf(member))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(GUILD_ID.asString())
                .hasMessageContaining("not cached");
    }

    @Test
    void deleteDelegatesToRest() {
        this.channel().delete("spring cleaning");

        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("deleteChannel", List.of(CHANNEL_ID, "spring cleaning")));
    }

    // ---- GuildTextChannel fields ----

    @Test
    void parsesTopicNsfwAndSlowmode() {
        final var channel = this.channel();

        assertThat(channel.topic()).contains("welcome here");
        assertThat(channel.isNsfw()).isTrue();
        assertThat(channel.rateLimitPerUser()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void defaultsTopicNsfwAndSlowmodeWhenAbsent() {
        final var channel = this.minimalChannel();

        assertThat(channel.topic()).isEmpty();
        assertThat(channel.isNsfw()).isFalse();
        assertThat(channel.rateLimitPerUser()).isEqualTo(Duration.ZERO);
    }

    @Test
    void parsesAnnouncementChannelsAsTextChannels() {
        final var channel = (GuildTextChannel) Channels.fromJson(this.duke, Json.parse(ANNOUNCEMENT_CHANNEL));

        assertThat(channel.type()).isEqualTo(ChannelType.GUILD_ANNOUNCEMENT);
        assertThat(channel.name()).isEqualTo("news");
    }

    @Test
    void lastMessageResolvesThroughTheCacheAndFetchesThroughRest() {
        final var channel = this.channel();
        assertThat(channel.lastMessage()).isPresent();
        assertThat(channel.lastMessage().orElseThrow().id()).isEqualTo(MESSAGE_ID);
        assertThat(channel.lastMessage().orElseThrow().get()).isEmpty();

        final var cached = this.duke.putMessage(Json.parse(MESSAGE));
        assertThat(channel.lastMessage().orElseThrow().get()).get().isSameAs(cached);

        final var fetched = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("fetchMessage", args -> fetched);
        assertThat(channel.lastMessage().orElseThrow().fetch()).isSameAs(fetched);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("fetchMessage", List.of(CHANNEL_ID, MESSAGE_ID)));
    }

    @Test
    void lastMessageIsEmptyWhenAbsent() {
        assertThat(this.minimalChannel().lastMessage()).isEmpty();
    }

    // ---- MessageChannel REST defaults ----

    @Test
    void sendDelegatesTheSpecUntouched() {
        final var sent = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("sendMessage", args -> sent);
        final var spec = MessageCreateSpec.builder().content("hello").build();

        assertThat(this.channel().send(spec)).isSameAs(sent);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("sendMessage", List.of(CHANNEL_ID, spec)));
    }

    @Test
    void sendStringBuildsASpecWithTheContentAndNoAllowedMentions() {
        final var sent = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("sendMessage", args -> sent);

        this.channel().send("hello world");

        final var spec =
                (MessageCreateSpec) this.duke.restCalls().getFirst().args().getLast();
        assertThat(spec.payload().string("content")).isEqualTo("hello world");
        assertThat(spec.payload().object("allowed_mentions").strings("parse")).isEmpty();
        assertThat(spec.payload().object("allowed_mentions").bool("replied_user"))
                .isFalse();
        assertThat(spec.files()).isEmpty();
    }

    @Test
    void fetchMessageDelegatesToRest() {
        final var fetched = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("fetchMessage", args -> fetched);

        assertThat(this.channel().fetchMessage(MESSAGE_ID)).isSameAs(fetched);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("fetchMessage", List.of(CHANNEL_ID, MESSAGE_ID)));
    }

    @Test
    void fetchMessagesDelegatesTheQuery() {
        final List<Message> history = List.of(Messages.fromJson(this.duke, Json.parse(MESSAGE)));
        this.duke.answer("fetchMessages", args -> history);
        final var query = MessageQuery.before(MESSAGE_ID, 50);

        assertThat(this.channel().fetchMessages(query)).isEqualTo(history);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("fetchMessages", List.of(CHANNEL_ID, query)));
    }

    @Test
    void editMessageDelegatesToRest() {
        final var edited = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("editMessage", args -> edited);
        final var spec = MessageEditSpec.builder().content("fixed").build();

        assertThat(this.channel().editMessage(MESSAGE_ID, spec)).isSameAs(edited);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("editMessage", List.of(CHANNEL_ID, MESSAGE_ID, spec)));
    }

    @Test
    void bulkDeleteDelegatesToRest() {
        final var ids = List.of(MESSAGE_ID, USER_ID);

        this.channel().bulkDelete(ids);

        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("bulkDeleteMessages", List.of(CHANNEL_ID, ids)));
    }

    @Test
    void createThreadUsesTheChannelRoute() {
        final var thread = Channels.fromJson(this.duke, Json.parse("""
                        {
                          "id": "1700000000000000001",
                          "type": 11,
                          "guild_id": "1000000000000000001",
                          "parent_id": "1200000000000000001",
                          "owner_id": "1400000000000000001",
                          "name": "help-me"
                        }
                        """));
        this.duke.answer("createThread", args -> thread);
        final var spec = ThreadCreateSpec.builder("help-me").build();

        assertThat(this.channel().createThread(spec)).isSameAs(thread);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("createThread", List.of(CHANNEL_ID, spec)));
    }

    @Test
    void typingDelegatesToRest() {
        this.channel().typing();

        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("triggerTyping", List.of(CHANNEL_ID)));
    }
}
