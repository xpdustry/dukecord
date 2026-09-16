// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.ThreadCreateSpec;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    private static final Snowflake GUILD_ID = Snowflake.of("1000000000000000001");
    private static final Snowflake CHANNEL_ID = Snowflake.of("1200000000000000001");
    private static final Snowflake MESSAGE_ID = Snowflake.of("1300000000000000001");
    private static final Snowflake BOB_ID = Snowflake.of("1600000000000000001");
    private static final Snowflake ROLE_ID = Snowflake.of("1500000000000000001");

    private static final String CHANNEL = """
            {
              "id": "1200000000000000001",
              "type": 0,
              "guild_id": "1000000000000000001",
              "name": "general"
            }
            """;

    private static final String ROLE = """
            {"id": "1500000000000000001", "name": "staff", "position": 2, "permissions": "0"}
            """;

    private static final String BOB_MEMBER = """
            {
              "user": {"id": "1600000000000000001", "username": "bob", "global_name": "Bob"},
              "nick": "Bobby",
              "roles": [],
              "joined_at": "2026-01-01T00:00:00Z",
              "flags": 0
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private Message message(final String content) {
        return this.message(content, "[]");
    }

    private Message message(final String content, final String mentions) {
        return Messages.fromJson(this.duke, Json.parse("""
                        {
                          "id": "1300000000000000001",
                          "channel_id": "1200000000000000001",
                          "guild_id": "1000000000000000001",
                          "author": {"id": "1400000000000000001", "username": "alice", "global_name": "Alice"},
                          "content": "%s",
                          "timestamp": "2026-09-15T12:00:00Z",
                          "type": 0,
                          "mentions": %s,
                          "components": [
                            {
                              "type": 1,
                              "components": [
                                {"type": 2, "style": 1, "custom_id": "go", "label": "Go"},
                                {"type": 2, "style": 4, "custom_id": "stop", "label": "Stop", "disabled": false}
                              ]
                            }
                          ]
                        }
                        """.formatted(content, mentions)));
    }

    // ---- strippedContent ----

    @Test
    void resolvesAUserMentionToTheCachedMemberDisplayName() {
        this.duke.putMember(GUILD_ID, Json.parse(BOB_MEMBER));

        assertThat(this.message("hi <@1600000000000000001>").strippedContent()).isEqualTo("hi @Bobby");
        assertThat(this.message("hi <@!1600000000000000001>").strippedContent()).isEqualTo("hi @Bobby");
    }

    @Test
    void fallsBackToTheMentionedUsersList() {
        final var mentions = """
                [{"id": "1600000000000000001", "username": "bob", "global_name": "Bob"}]
                """;

        assertThat(this.message("hi <@1600000000000000001>", mentions).strippedContent())
                .isEqualTo("hi @Bob");
    }

    @Test
    void fallsBackToTheCachedUser() {
        this.duke.putUser(Json.parse("""
                {"id": "1600000000000000001", "username": "bob"}
                """));

        assertThat(this.message("hi <@1600000000000000001>").strippedContent()).isEqualTo("hi @bob");
    }

    @Test
    void fallsBackToUnknownUser() {
        assertThat(this.duke.cache().user(BOB_ID)).isEmpty();

        assertThat(this.message("hi <@1600000000000000001>").strippedContent()).isEqualTo("hi @unknown-user");
    }

    @Test
    void resolvesRoleMentions() {
        assertThat(this.message("ping <@&1500000000000000001>").strippedContent())
                .isEqualTo("ping @deleted-role");

        this.duke.putRole(GUILD_ID, Json.parse(ROLE));
        assertThat(this.duke.cache().role(ROLE_ID)).isPresent();
        assertThat(this.message("ping <@&1500000000000000001>").strippedContent())
                .isEqualTo("ping @staff");
    }

    @Test
    void resolvesChannelMentions() {
        assertThat(this.message("see <#1200000000000000001>").strippedContent()).isEqualTo("see #deleted-channel");

        this.duke.putChannel(Json.parse(CHANNEL));
        assertThat(this.message("see <#1200000000000000001>").strippedContent()).isEqualTo("see #general");
    }

    @Test
    void resolvesCustomAndAnimatedEmoji() {
        assertThat(this.message("nice <:duke:1900000000000000001> <a:wave:1900000000000000002>")
                        .strippedContent())
                .isEqualTo("nice :duke: :wave:");
    }

    @Test
    void escapesMarkdownCharacters() {
        final var content = "*b* _i_ ~s~ `c` |x| > q \\\\ end";

        assertThat(this.message(content).strippedContent())
                .isEqualTo("\\*b\\* \\_i\\_ \\~s\\~ \\`c\\` \\|x\\| \\> q \\\\ end");
    }

    @Test
    void leavesPlainTextUntouched() {
        assertThat(this.message("just a plain sentence, with punctuation!").strippedContent())
                .isEqualTo("just a plain sentence, with punctuation!");
        assertThat(this.message("").strippedContent()).isEmpty();
    }

    // ---- REST ----

    @Test
    void replySendsToTheChannelWithAMessageReference() {
        final var sent = this.message("pong");
        this.duke.answer("sendMessage", args -> sent);
        final var spec = MessageCreateSpec.builder().content("pong").build();

        assertThat(this.message("ping").reply(spec)).isSameAs(sent);

        final var call = this.duke.restCalls().getFirst();
        assertThat(call.method()).isEqualTo("sendMessage");
        assertThat(call.args()).hasSize(2).first().isEqualTo(CHANNEL_ID);
        final var payload = ((MessageCreateSpec) call.args().getLast()).payload();
        assertThat(payload.string("content")).isEqualTo("pong");
        assertThat(payload.object("message_reference").snowflake("message_id")).isEqualTo(MESSAGE_ID);
        assertThat(payload.object("message_reference").snowflake("channel_id")).isEqualTo(CHANNEL_ID);
        assertThat(payload.object("message_reference").bool("fail_if_not_exists"))
                .isFalse();
    }

    @Test
    void replyWithAStringBuildsTheSpec() {
        final var sent = this.message("pong");
        this.duke.answer("sendMessage", args -> sent);

        this.message("ping").reply("pong");

        final var payload =
                ((MessageCreateSpec) this.duke.restCalls().getFirst().args().getLast()).payload();
        assertThat(payload.string("content")).isEqualTo("pong");
        assertThat(payload.object("allowed_mentions").strings("parse")).isEmpty();
        assertThat(payload.object("message_reference").snowflake("message_id")).isEqualTo(MESSAGE_ID);
    }

    @Test
    void editDelegatesToRest() {
        final var edited = this.message("edited");
        this.duke.answer("editMessage", args -> edited);
        final var spec = MessageEditSpec.builder().content("edited").build();

        assertThat(this.message("original").edit(spec)).isSameAs(edited);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("editMessage", List.of(CHANNEL_ID, MESSAGE_ID, spec)));
    }

    @Test
    void deleteDelegatesToRestWithAndWithoutAReason() {
        this.message("x").delete();
        this.message("x").delete("spam");

        assertThat(this.duke.restCalls())
                .containsExactly(
                        new TestDukecord.RestCall("deleteMessage", List.of(CHANNEL_ID, MESSAGE_ID)),
                        new TestDukecord.RestCall("deleteMessage", List.of(CHANNEL_ID, MESSAGE_ID, "spam")));
    }

    @Test
    void disableComponentsEditsTheMessageWithEveryComponentDisabled() {
        final var message = this.message("x");
        this.duke.answer("editMessage", args -> message);

        message.disableComponents();

        final var call = this.duke.restCalls().getFirst();
        assertThat(call.method()).isEqualTo("editMessage");
        assertThat(call.args()).hasSize(3);
        final var payload = ((MessageEditSpec) call.args().getLast()).payload();
        final var row = payload.objects("components").getFirst();
        assertThat(row.integer("type")).isEqualTo(Component.TYPE_ACTION_ROW);
        assertThat(row.objects("components")).hasSize(2).allSatisfy(button -> assertThat(button.bool("disabled"))
                .isTrue());
    }

    @Test
    void createThreadUsesTheMessageAnchoredRoute() {
        final var thread = this.duke.putChannel(Json.parse("""
                {
                  "id": "1700000000000000001",
                  "type": 11,
                  "guild_id": "1000000000000000001",
                  "parent_id": "1200000000000000001",
                  "owner_id": "1400000000000000001",
                  "name": "spin-off"
                }
                """));
        this.duke.answer("createThread", args -> thread);
        final var spec = ThreadCreateSpec.builder("spin-off").build();

        assertThat(this.message("x").createThread(spec)).isSameAs(thread);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("createThread", List.of(CHANNEL_ID, MESSAGE_ID, spec)));
    }

    @Test
    void addReactionDelegatesToRest() {
        final var emoji = Emoji.unicode("👍");

        this.message("x").addReaction(emoji);

        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("addReaction", List.of(CHANNEL_ID, MESSAGE_ID, emoji)));
    }

    @Test
    void pinAndUnpinDelegateToRest() {
        this.message("x").pin();
        this.message("x").unpin();

        assertThat(this.duke.restCalls())
                .containsExactly(
                        new TestDukecord.RestCall("pinMessage", List.of(CHANNEL_ID, MESSAGE_ID)),
                        new TestDukecord.RestCall("unpinMessage", List.of(CHANNEL_ID, MESSAGE_ID)));
    }
}
