// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageReferenceTest {

    private static final String RECEIVED_REPLY = """
            {
              "type": 0,
              "message_id": "1300000000000000001",
              "channel_id": "1200000000000000001",
              "guild_id": "1000000000000000001"
            }
            """;

    private static final String FORWARD = """
            {
              "type": 1,
              "message_id": "1300000000000000001",
              "channel_id": "1200000000000000001"
            }
            """;

    @Test
    void parsesAReceivedReplyReference() {
        final var reference = MessageReference.fromJson(Json.parse(RECEIVED_REPLY));

        assertThat(reference.type()).isEqualTo(MessageReference.Type.DEFAULT);
        assertThat(reference.messageId()).contains(Snowflake.of("1300000000000000001"));
        assertThat(reference.channelId()).contains(Snowflake.of("1200000000000000001"));
        assertThat(reference.guildId()).contains(Snowflake.of("1000000000000000001"));
    }

    @Test
    void readsTypeOneAsAForward() {
        final var reference = MessageReference.fromJson(Json.parse(FORWARD));

        assertThat(reference.type()).isEqualTo(MessageReference.Type.FORWARD);
        assertThat(reference.guildId()).isEmpty();
    }

    @Test
    void defaultsToAReplyWhenTheTypeIsAbsentOrUnknown() {
        assertThat(MessageReference.fromJson(Json.parse("""
                                {"message_id": "1300000000000000001"}
                                """)).type()).isEqualTo(MessageReference.Type.DEFAULT);
        assertThat(MessageReference.fromJson(Json.parse(RECEIVED_REPLY).with("type", 99))
                        .type())
                .isEqualTo(MessageReference.Type.DEFAULT);
    }

    @Test
    void leavesTheSendingShapeIdsEmptyWhenAbsent() {
        final var reference = MessageReference.fromJson(Json.parse("""
                {"message_id": "1300000000000000001"}
                """));

        assertThat(reference.messageId()).contains(Snowflake.of("1300000000000000001"));
        assertThat(reference.channelId()).isEmpty();
        assertThat(reference.guildId()).isEmpty();
    }
}
