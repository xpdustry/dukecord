// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import dev.phinner.dukecord.model.message.Messages;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DmChannelTest {

    private static final Snowflake CHANNEL_ID = Snowflake.of("1800000000000000001");
    private static final Snowflake MESSAGE_ID = Snowflake.of("1300000000000000002");

    private static final String DM = """
            {
              "id": "1800000000000000001",
              "type": 1,
              "last_message_id": "1300000000000000002",
              "recipients": [
                {"id": "1400000000000000001", "username": "alice", "global_name": "Alice"},
                {"id": "1600000000000000001", "username": "bob"}
              ]
            }
            """;

    private static final String MINIMAL_DM = """
            {
              "id": "1800000000000000001",
              "type": 3
            }
            """;

    private static final String MESSAGE = """
            {
              "id": "1300000000000000002",
              "channel_id": "1800000000000000001",
              "author": {"id": "1400000000000000001", "username": "alice"},
              "content": "hi",
              "timestamp": "2026-09-15T12:00:00Z"
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private DmChannel channel() {
        return (DmChannel) Channels.fromJson(this.duke, Json.parse(DM));
    }

    @Test
    void parsesTheRecipients() {
        final var channel = this.channel();

        assertThat(channel.type()).isEqualTo(ChannelType.DM);
        assertThat(channel.recipients()).extracting(User::username).containsExactly("alice", "bob");
        assertThat(channel.recipients().getFirst().displayName()).isEqualTo("Alice");
    }

    @Test
    void hasNoRecipientsWhenTheFieldIsAbsent() {
        final var channel = (DmChannel) Channels.fromJson(this.duke, Json.parse(MINIMAL_DM));

        assertThat(channel.type()).isEqualTo(ChannelType.GROUP_DM);
        assertThat(channel.recipients()).isEmpty();
        assertThat(channel.lastMessage()).isEmpty();
    }

    @Test
    void lastMessageResolvesThroughTheCache() {
        final var channel = this.channel();
        assertThat(channel.lastMessage()).isPresent();
        assertThat(channel.lastMessage().orElseThrow().id()).isEqualTo(MESSAGE_ID);

        final var cached = this.duke.putMessage(Json.parse(MESSAGE));
        assertThat(channel.lastMessage().orElseThrow().get()).get().isSameAs(cached);
    }

    @Test
    void sendDelegatesToRest() {
        final var sent = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("sendMessage", args -> sent);

        assertThat(this.channel().send("hello")).isSameAs(sent);

        final var call = this.duke.restCalls().getFirst();
        assertThat(call.method()).isEqualTo("sendMessage");
        assertThat(call.args()).hasSize(2).first().isEqualTo(CHANNEL_ID);
        assertThat(((MessageCreateSpec) call.args().getLast()).payload().string("content"))
                .isEqualTo("hello");
        assertThat(this.duke.restCalls()).hasSize(1);
    }

    @Test
    void sendSpecDelegatesToRest() {
        final var sent = Messages.fromJson(this.duke, Json.parse(MESSAGE));
        this.duke.answer("sendMessage", args -> sent);
        final var spec = MessageCreateSpec.builder().content("body").build();

        this.channel().send(spec);

        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("sendMessage", List.of(CHANNEL_ID, spec)));
    }
}
