// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.JsonException;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelsTest {

    private static final String UNBOUND_CHANNEL = """
            {
              "id": "1200000000000000009",
              "type": %d,
              "name": "mystery"
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private static Json guildChannel(final int type) {
        return Json.parse("""
                {
                  "id": "1200000000000000001",
                  "type": %d,
                  "guild_id": "1000000000000000001",
                  "name": "general",
                  "position": 2,
                  "permission_overwrites": []
                }
                """.formatted(type));
    }

    private static Json thread(final int type) {
        return Json.parse("""
                {
                  "id": "1700000000000000001",
                  "type": %d,
                  "guild_id": "1000000000000000001",
                  "parent_id": "1200000000000000001",
                  "owner_id": "1400000000000000001",
                  "name": "help-me"
                }
                """.formatted(type));
    }

    private static Json dm(final int type) {
        return Json.parse("""
                {
                  "id": "1800000000000000001",
                  "type": %d,
                  "recipients": [{"id": "1400000000000000001", "username": "alice"}]
                }
                """.formatted(type));
    }

    @Test
    void parsesTextAndAnnouncementChannelsAsGuildTextChannels() {
        assertThat(Channels.fromJson(this.duke, guildChannel(0)))
                .isInstanceOf(GuildTextChannel.class)
                .extracting(Channel::type)
                .isEqualTo(ChannelType.GUILD_TEXT);
        assertThat(Channels.fromJson(this.duke, guildChannel(5)))
                .isInstanceOf(GuildTextChannel.class)
                .extracting(Channel::type)
                .isEqualTo(ChannelType.GUILD_ANNOUNCEMENT);
    }

    @Test
    void parsesCategoryChannels() {
        assertThat(Channels.fromJson(this.duke, guildChannel(4)))
                .isInstanceOf(CategoryChannel.class)
                .extracting(Channel::type)
                .isEqualTo(ChannelType.GUILD_CATEGORY);
    }

    @Test
    void parsesEveryThreadTypeAsThreadChannel() {
        for (final var type : List.of(10, 11, 12)) {
            assertThat(Channels.fromJson(this.duke, thread(type)))
                    .as("type %d", type)
                    .isInstanceOf(ThreadChannel.class);
        }
    }

    @Test
    void parsesDirectAndGroupChannelsAsDmChannels() {
        assertThat(Channels.fromJson(this.duke, dm(1)))
                .isInstanceOf(DmChannel.class)
                .extracting(Channel::type)
                .isEqualTo(ChannelType.DM);
        assertThat(Channels.fromJson(this.duke, dm(3)))
                .isInstanceOf(DmChannel.class)
                .extracting(Channel::type)
                .isEqualTo(ChannelType.GROUP_DM);
    }

    @Test
    void parsesUnmappedGuildTypesAsUnknownGuildChannels() {
        for (final var type : List.of(2, 13, 15, 16, 99)) {
            assertThat(Channels.fromJson(this.duke, guildChannel(type)))
                    .as("type %d", type)
                    .isInstanceOf(UnknownGuildChannel.class);
        }
    }

    @Test
    void parsesAnUnmappedTypeWithoutGuildIdAsUnknownChannel() {
        final var channel = Channels.fromJson(this.duke, Json.parse(UNBOUND_CHANNEL.formatted(99)));

        assertThat(channel).isInstanceOf(UnknownChannel.class).isNotInstanceOf(GuildChannel.class);
        assertThat(channel.type()).isEqualTo(ChannelType.UNKNOWN);
        assertThat(channel.typeCode()).isEqualTo(99);
    }

    @Test
    void rejectsAGuildBoundTypeWithoutGuildId() {
        final var json = Json.parse(UNBOUND_CHANNEL.formatted(0));

        assertThatThrownBy(() -> Channels.fromJson(this.duke, json))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("1200000000000000009")
                .hasMessageContaining("GUILD_TEXT")
                .hasMessageContaining("guild_id");
    }

    @Test
    void exposesTheTypeAndTheRawTypeCode() {
        final var known = Channels.fromJson(this.duke, guildChannel(0));
        final var unknown = Channels.fromJson(this.duke, guildChannel(42));

        assertThat(known.type()).isEqualTo(ChannelType.GUILD_TEXT);
        assertThat(known.typeCode()).isZero();
        assertThat(unknown.type()).isEqualTo(ChannelType.UNKNOWN);
        assertThat(unknown.typeCode()).isEqualTo(42);
    }

    @Test
    void coversTheSealedHierarchyWithoutADefaultBranch() {
        final var channels = List.of(
                Channels.fromJson(this.duke, guildChannel(0)),
                Channels.fromJson(this.duke, guildChannel(4)),
                Channels.fromJson(this.duke, thread(11)),
                Channels.fromJson(this.duke, guildChannel(2)),
                Channels.fromJson(this.duke, dm(1)),
                Channels.fromJson(this.duke, Json.parse(UNBOUND_CHANNEL.formatted(99))));

        assertThat(channels.stream().map(ChannelsTest::describe))
                .containsExactly("text", "category", "thread", "unknown-guild", "dm", "unknown");
    }

    /** Compiles without a {@code default} branch only while {@link Channel} stays sealed over these six leaves. */
    private static String describe(final Channel channel) {
        return switch (channel) {
            case GuildTextChannel ignored -> "text";
            case CategoryChannel ignored -> "category";
            case ThreadChannel ignored -> "thread";
            case UnknownGuildChannel ignored -> "unknown-guild";
            case DmChannel ignored -> "dm";
            case UnknownChannel ignored -> "unknown";
        };
    }

    @Test
    void refResolvesThroughTheCache() {
        final var channel = this.duke.putChannel(guildChannel(0));
        final var ref = Channels.ref(this.duke, GuildTextChannel.class, channel.id());

        assertThat(ref.id()).isEqualTo(channel.id());
        assertThat(ref.type()).isEqualTo(GuildTextChannel.class);
        assertThat(ref.get()).get().isSameAs(channel);
        assertThat(this.duke.restCalls()).isEmpty();
    }

    @Test
    void refIgnoresACachedChannelOfAnotherType() {
        final var channel = this.duke.putChannel(guildChannel(0));

        assertThat(Channels.ref(this.duke, CategoryChannel.class, channel.id()).get())
                .isEmpty();
    }

    @Test
    void refFetchReturnsTheTypedChannel() {
        final var fetched = Channels.fromJson(this.duke, guildChannel(0));
        this.duke.answer("fetchChannel", args -> fetched);
        final var ref = Channels.ref(this.duke, GuildTextChannel.class, fetched.id());

        assertThat(ref.fetch()).isSameAs(fetched);
        assertThat(this.duke.restCalls())
                .singleElement()
                .isEqualTo(new TestDukecord.RestCall("fetchChannel", List.of(fetched.id())));
    }

    @Test
    void refFetchRejectsAChannelOfAnotherType() {
        final var fetched = Channels.fromJson(this.duke, dm(1));
        this.duke.answer("fetchChannel", args -> fetched);
        final var ref = Channels.ref(this.duke, GuildTextChannel.class, fetched.id());

        assertThatThrownBy(ref::fetch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DM")
                .hasMessageContaining("GuildTextChannel");
    }
}
