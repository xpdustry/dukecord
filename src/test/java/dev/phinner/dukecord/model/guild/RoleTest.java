// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.Permission;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.support.TestDukecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    private static final Snowflake GUILD_ID = Snowflake.of("266604749638606850");
    private static final String MODERATOR = """
            {
              "id": "739023850393600001",
              "name": "Moderator",
              "color": 3447003,
              "hoist": true,
              "position": 5,
              "permissions": "8192",
              "managed": false,
              "mentionable": true
            }""";

    private final TestDukecord duke = new TestDukecord();

    private Role role(final String json) {
        return Roles.fromJson(this.duke, GUILD_ID, Json.parse(json));
    }

    @Test
    void parsesEveryModelledField() {
        final var role = this.role(MODERATOR);

        assertThat(role.id()).isEqualTo(Snowflake.of("739023850393600001"));
        assertThat(role.guild().id()).isEqualTo(GUILD_ID);
        assertThat(role.name()).isEqualTo("Moderator");
        assertThat(role.color()).isEqualTo(3447003);
        assertThat(role.position()).isEqualTo(5);
        assertThat(role.permissions()).isEqualTo(PermissionSet.of(Permission.MANAGE_MESSAGES));
        assertThat(role.isHoisted()).isTrue();
        assertThat(role.isManaged()).isFalse();
        assertThat(role.isMentionable()).isTrue();
        assertThat(role.duke()).isSameAs(this.duke);
    }

    @Test
    void defaultsTheBooleanFlagsToFalseWhenTheyAreAbsent() {
        final var role = this.role("""
                {"id": "739023850393600001", "name": "Plain", "position": 0, "permissions": "0"}""");

        assertThat(role.isHoisted()).isFalse();
        assertThat(role.isManaged()).isFalse();
        assertThat(role.isMentionable()).isFalse();
        assertThat(role.permissions()).isEqualTo(PermissionSet.NONE);
    }

    @Test
    void prefersThePrimaryColorOfTheColorsObjectOverTheLegacyColor() {
        final var role = this.role("""
                {
                  "id": "739023850393600001",
                  "name": "Gradient",
                  "color": 3447003,
                  "colors": {"primary_color": 16711680, "secondary_color": 255, "tertiary_color": null},
                  "position": 1,
                  "permissions": "0"
                }""");

        assertThat(role.color()).isEqualTo(16711680);
    }

    @Test
    void fallsBackToTheLegacyColorWhenTheColorsObjectHasNoPrimaryColor() {
        final var role = this.role("""
                {
                  "id": "739023850393600001",
                  "name": "Gradient",
                  "color": 3447003,
                  "colors": {"secondary_color": null},
                  "position": 1,
                  "permissions": "0"
                }""");

        assertThat(role.color()).isEqualTo(3447003);
    }

    @Test
    void fallsBackToTheLegacyColorWhenTheColorsObjectIsAbsent() {
        assertThat(this.role(MODERATOR).color()).isEqualTo(3447003);
    }

    @Test
    void defaultsTheColorToZeroWhenNeitherFormIsPresent() {
        final var role = this.role("""
                {"id": "739023850393600001", "name": "Colourless", "position": 0, "permissions": "0"}""");

        assertThat(role.color()).isZero();
    }

    @Test
    void isEveryoneWhenItsIdIsTheGuildId() {
        final var role = this.role("""
                {"id": "266604749638606850", "name": "@everyone", "position": 0, "permissions": "3072"}""");

        assertThat(role.isEveryone()).isTrue();
    }

    @Test
    void isNotEveryoneForAnyOtherId() {
        assertThat(this.role(MODERATOR).isEveryone()).isFalse();
    }

    @Test
    void mentionUsesTheRoleId() {
        assertThat(this.role(MODERATOR).mention()).isEqualTo("<@&739023850393600001>");
    }

    @Test
    void guildReferenceResolvesThroughTheCache() {
        final var guild = this.duke.putGuild(Json.parse("""
                {"id": "266604749638606850", "name": "Xpdustry", "owner_id": "159985870458322944"}"""));

        assertThat(this.role(MODERATOR).guild().get()).contains(guild);
        assertThat(this.duke.restCalls()).isEmpty();
    }

    @Test
    void guildReferenceIsEmptyWhileTheGuildIsNotCached() {
        assertThat(this.role(MODERATOR).guild().get()).isEmpty();
    }

    @Test
    void toStringNamesTheId() {
        assertThat(this.role(MODERATOR)).hasToString("Role[739023850393600001]");
    }
}
