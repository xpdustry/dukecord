// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.MemberCompleteness;
import dev.phinner.dukecord.support.TestDukecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DukecordTest {

    private static final Snowflake GUILD_ID = Snowflake.of("266604749638606850");
    private static final Snowflake OTHER_GUILD_ID = Snowflake.of("266604749638606851");
    private static final Snowflake UNKNOWN_ID = Snowflake.of("999999999999999999");

    private final TestDukecord duke = new TestDukecord();

    private Json guildJson(final Snowflake id, final String name) {
        return Json.parse("""
                {"id": "%s", "name": "%s", "owner_id": "159985870458322944"}""".formatted(id.asString(), name));
    }

    @Test
    void guildReadsTheCache() {
        final var guild = this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"));

        assertThat(this.duke.guild(GUILD_ID)).contains(guild);
    }

    @Test
    void guildIsEmptyForAnUnknownId() {
        assertThat(this.duke.guild(UNKNOWN_ID)).isEmpty();
    }

    @Test
    void requireGuildReturnsAnAvailableGuild() {
        final var guild = this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"));

        assertThat(this.duke.requireGuild(GUILD_ID)).isEqualTo(guild);
    }

    @Test
    void requireGuildBlamesTheCacheForAnUnknownGuild() {
        assertThatThrownBy(() -> this.duke.requireGuild(UNKNOWN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Guild 999999999999999999 is not in the cache. "
                        + "Is the bot a member of it, and has login() completed?");
    }

    @Test
    void requireGuildBlamesTheOutageForAnUnavailableGuild() {
        this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"), false, MemberCompleteness.NONE);

        assertThatThrownBy(() -> this.duke.requireGuild(GUILD_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Guild 266604749638606850 is currently unavailable (Discord outage)");
    }

    @Test
    void requireSingleGuildReturnsTheOnlyGuild() {
        final var guild = this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"));

        assertThat(this.duke.requireSingleGuild()).isEqualTo(guild);
    }

    @Test
    void requireSingleGuildThrowsWithoutAnyGuild() {
        assertThatThrownBy(this.duke::requireSingleGuild)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected exactly one guild but the bot is in 0");
    }

    @Test
    void requireSingleGuildThrowsWithSeveralGuilds() {
        this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"));
        this.duke.putGuild(this.guildJson(OTHER_GUILD_ID, "Chaotic Neutral"));

        assertThatThrownBy(this.duke::requireSingleGuild)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected exactly one guild but the bot is in 2");
    }

    @Test
    void requireSingleGuildStillReportsAnOutageOfTheOnlyGuild() {
        this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"), false, MemberCompleteness.NONE);

        assertThatThrownBy(this.duke::requireSingleGuild)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Guild 266604749638606850 is currently unavailable (Discord outage)");
    }

    @Test
    void cacheLookupsNeverTouchTheRestFacade() {
        this.duke.putGuild(this.guildJson(GUILD_ID, "Xpdustry"));

        assertThat(this.duke.guild(GUILD_ID)).isPresent();
        assertThat(this.duke.requireGuild(GUILD_ID)).isNotNull();
        assertThat(this.duke.requireSingleGuild()).isNotNull();
        assertThat(this.duke.restCalls()).isEmpty();
    }
}
