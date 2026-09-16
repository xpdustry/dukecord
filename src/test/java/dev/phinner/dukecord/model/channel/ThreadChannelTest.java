// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.support.TestDukecord;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadChannelTest {

    private static final Snowflake GUILD_ID = Snowflake.of("1000000000000000001");
    private static final Snowflake PARENT_ID = Snowflake.of("1200000000000000001");
    private static final Snowflake THREAD_ID = Snowflake.of("1700000000000000001");
    private static final Snowflake OWNER_ID = Snowflake.of("1400000000000000001");

    private static final String THREAD = """
            {
              "id": "1700000000000000001",
              "type": 11,
              "guild_id": "1000000000000000001",
              "parent_id": "1200000000000000001",
              "owner_id": "1400000000000000001",
              "name": "help-me",
              "position": 0,
              "message_count": 7,
              "member_count": 3,
              "last_message_id": "1300000000000000001",
              "member": {"join_timestamp": "2026-09-15T12:00:00Z", "flags": 0},
              "thread_metadata": {
                "archived": true,
                "auto_archive_duration": 1440,
                "archive_timestamp": "2026-09-15T12:00:00Z",
                "locked": true,
                "invitable": false,
                "create_timestamp": "2026-09-01T08:30:00Z"
              }
            }
            """;

    private static final String MINIMAL_THREAD = """
            {
              "id": "1700000000000000001",
              "type": 12,
              "guild_id": "1000000000000000001",
              "parent_id": "1200000000000000001",
              "owner_id": "1400000000000000001",
              "name": "private-talk"
            }
            """;

    private static final String PARENT = """
            {
              "id": "1200000000000000001",
              "type": 0,
              "guild_id": "1000000000000000001",
              "name": "general"
            }
            """;

    private static final String OWNER = """
            {
              "user": {"id": "1400000000000000001", "username": "alice", "global_name": "Alice"},
              "roles": [],
              "joined_at": "2026-01-01T00:00:00Z",
              "flags": 0
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private ThreadChannel thread() {
        return (ThreadChannel) Channels.fromJson(this.duke, Json.parse(THREAD));
    }

    private ThreadChannel minimalThread() {
        return (ThreadChannel) Channels.fromJson(this.duke, Json.parse(MINIMAL_THREAD));
    }

    @Test
    void pointsAtItsParentTextChannelAndNeverAtACategory() {
        final var thread = this.thread();

        assertThat(thread.parent().id()).isEqualTo(PARENT_ID);
        assertThat(thread.parent().type()).isEqualTo(GuildChannel.class);
        assertThat(thread.category()).isEmpty();
        assertThat(thread.isInCategory(PARENT_ID)).isFalse();

        final var parent = this.duke.putChannel(Json.parse(PARENT));
        assertThat(thread.parent().get()).get().isSameAs(parent);
    }

    @Test
    void ownerResolvesToTheMemberOfTheThreadGuild() {
        final var thread = this.thread();
        assertThat(thread.owner().id()).isEqualTo(OWNER_ID);
        assertThat(thread.owner().get()).isEmpty();

        this.duke.putMember(Snowflake.of("1000000000000000009"), Json.parse(OWNER));
        assertThat(thread.owner().get()).as("member of another guild").isEmpty();

        final var owner = this.duke.putMember(GUILD_ID, Json.parse(OWNER));
        assertThat(thread.owner().get()).get().isSameAs(owner);
        assertThat(thread.owner().get().map(Member::displayName)).contains("Alice");
    }

    @Test
    void parsesThreadMetadata() {
        final var metadata = this.thread().metadata();

        assertThat(metadata.archived()).isTrue();
        assertThat(metadata.autoArchiveDuration()).isEqualTo(Duration.ofDays(1));
        assertThat(metadata.archiveTimestamp()).isEqualTo(Instant.parse("2026-09-15T12:00:00Z"));
        assertThat(metadata.locked()).isTrue();
        assertThat(metadata.invitable()).contains(false);
        assertThat(metadata.createTimestamp()).contains(Instant.parse("2026-09-01T08:30:00Z"));
    }

    @Test
    void fallsBackToDefaultMetadataWhenAbsent() {
        final var metadata = this.minimalThread().metadata();

        assertThat(metadata.archived()).isFalse();
        assertThat(metadata.autoArchiveDuration()).isEqualTo(Duration.ZERO);
        assertThat(metadata.archiveTimestamp()).isEqualTo(Instant.EPOCH);
        assertThat(metadata.locked()).isFalse();
        assertThat(metadata.invitable()).isEmpty();
        assertThat(metadata.createTimestamp()).isEmpty();
    }

    @Test
    void readsTheApproximateCountsAndDefaultsThemToZero() {
        assertThat(this.thread().messageCount()).isEqualTo(7);
        assertThat(this.thread().memberCount()).isEqualTo(3);
        assertThat(this.minimalThread().messageCount()).isZero();
        assertThat(this.minimalThread().memberCount()).isZero();
    }

    @Test
    void derivesJoinedFromThePresenceOfTheMemberField() {
        assertThat(this.thread().isJoined()).isTrue();
        assertThat(this.minimalThread().isJoined()).isFalse();
    }

    @Test
    void letsTheCallerForceTheJoinedFlag() {
        final var forcedJoined = (ThreadChannel) Channels.fromJson(this.duke, Json.parse(MINIMAL_THREAD), true);
        final var forcedLeft = (ThreadChannel) Channels.fromJson(this.duke, Json.parse(THREAD), false);

        assertThat(forcedJoined.isJoined()).isTrue();
        assertThat(forcedLeft.isJoined()).isFalse();
    }

    @Test
    void exposesTheLastMessageReference() {
        assertThat(this.thread().lastMessage())
                .get()
                .extracting(ref -> ref.id())
                .isEqualTo(Snowflake.of("1300000000000000001"));
        assertThat(this.minimalThread().lastMessage()).isEmpty();
    }

    @Test
    void joinAndLeaveDelegateToRest() {
        final var thread = this.thread();

        thread.join();
        thread.leave();

        assertThat(this.duke.restCalls())
                .containsExactly(
                        new TestDukecord.RestCall("joinThread", List.of(THREAD_ID)),
                        new TestDukecord.RestCall("leaveThread", List.of(THREAD_ID)));
    }
}
