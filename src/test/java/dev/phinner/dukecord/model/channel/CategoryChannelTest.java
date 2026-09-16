// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.support.TestDukecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryChannelTest {

    private static final Snowflake CATEGORY_ID = Snowflake.of("1100000000000000001");

    private static final String CATEGORY = """
            {
              "id": "1100000000000000001",
              "type": 4,
              "guild_id": "1000000000000000001",
              "name": "Staff",
              "position": 1,
              "parent_id": null,
              "permission_overwrites": []
            }
            """;

    private final TestDukecord duke = new TestDukecord();

    private CategoryChannel category() {
        return (CategoryChannel) this.duke.putChannel(Json.parse(CATEGORY));
    }

    private void putChild(final String id, final String name, final int position, final String parentId) {
        this.putChild(id, name, position, parentId, "1000000000000000001");
    }

    private void putChild(
            final String id, final String name, final int position, final String parentId, final String guildId) {
        this.duke.putChannel(Json.parse("""
                {
                  "id": "%s",
                  "type": 0,
                  "guild_id": "%s",
                  "name": "%s",
                  "position": %d,
                  "parent_id": %s
                }
                """.formatted(id, guildId, name, position, parentId)));
    }

    @Test
    void hasNoCategoryOfItsOwn() {
        final var category = this.category();

        assertThat(category.type()).isEqualTo(ChannelType.GUILD_CATEGORY);
        assertThat(category.category()).isEmpty();
        assertThat(category.isInCategory(CATEGORY_ID)).isFalse();
        assertThat(category.name()).isEqualTo("Staff");
        assertThat(category.position()).isEqualTo(1);
    }

    @Test
    void listsOnlyItsOwnChildrenOrderedByPosition() {
        final var category = this.category();
        this.putChild("1200000000000000001", "late", 7, "\"1100000000000000001\"");
        this.putChild("1200000000000000002", "early", 2, "\"1100000000000000001\"");
        this.putChild("1200000000000000003", "other-category", 1, "\"1100000000000000009\"");
        this.putChild("1200000000000000004", "no-category", 0, "null");
        this.putChild("1200000000000000005", "other-guild", 0, "\"1100000000000000001\"", "1000000000000000009");

        assertThat(category.channels()).extracting(GuildChannel::name).containsExactly("early", "late");
    }

    @Test
    void findsAChildByItsExactName() {
        final var category = this.category();
        this.putChild("1200000000000000001", "general", 1, "\"1100000000000000001\"");
        this.putChild("1200000000000000002", "general-two", 2, "\"1100000000000000001\"");

        assertThat(category.channel("general"))
                .get()
                .extracting(GuildChannel::name)
                .isEqualTo("general");
        assertThat(category.channel("General")).isEmpty();
        assertThat(category.channel("missing")).isEmpty();
    }

    @Test
    void hasNoChannelsWhenNothingIsCached() {
        assertThat(this.category().channels()).isEmpty();
    }
}
