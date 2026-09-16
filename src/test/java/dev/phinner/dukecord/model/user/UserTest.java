// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.user;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channels;
import dev.phinner.dukecord.model.channel.DmChannel;
import dev.phinner.dukecord.model.guild.Members;
import dev.phinner.dukecord.support.TestDukecord;
import dev.phinner.dukecord.support.TestDukecord.RestCall;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final String FULL = """
            {
              "id": "159985870458322944",
              "username": "phinner",
              "global_name": "Phinner",
              "discriminator": "0",
              "avatar": "a_1f4d0e6b9c2a8e7f3b1d5c9a0e2f4b6d",
              "bot": true,
              "system": false
            }""";

    private final TestDukecord duke = new TestDukecord();

    private User user(final String json) {
        return Users.fromJson(this.duke, Json.parse(json));
    }

    @Test
    void parsesEveryModelledField() {
        final var user = this.user(FULL);

        assertThat(user.id()).isEqualTo(Snowflake.of("159985870458322944"));
        assertThat(user.username()).isEqualTo("phinner");
        assertThat(user.globalName()).contains("Phinner");
        assertThat(user.avatarHash()).contains("a_1f4d0e6b9c2a8e7f3b1d5c9a0e2f4b6d");
        assertThat(user.isBot()).isTrue();
        assertThat(user.isSystem()).isFalse();
        assertThat(user.duke()).isSameAs(this.duke);
    }

    @Test
    void keepsUnmodelledFieldsInTheRawObject() {
        assertThat(this.user(FULL).raw().string("discriminator")).isEqualTo("0");
    }

    @Test
    void treatsAnAbsentBotAndSystemFlagAsFalse() {
        final var user = this.user("""
                {"id": "266604749638606850", "username": "duke"}""");

        assertThat(user.isBot()).isFalse();
        assertThat(user.isSystem()).isFalse();
    }

    @Test
    void readsANullGlobalNameAndAvatarAsAbsent() {
        final var user = this.user("""
                {"id": "266604749638606850", "username": "duke", "global_name": null, "avatar": null}""");

        assertThat(user.globalName()).isEmpty();
        assertThat(user.avatarHash()).isEmpty();
    }

    @Test
    void displayNamePrefersTheGlobalName() {
        assertThat(this.user(FULL).displayName()).isEqualTo("Phinner");
    }

    @Test
    void displayNameFallsBackToTheUsername() {
        assertThat(this.user("""
                        {"id": "266604749638606850", "username": "duke", "global_name": null}""").displayName()).isEqualTo("duke");
    }

    @Test
    void mentionUsesTheUserId() {
        assertThat(this.user(FULL).mention()).isEqualTo("<@159985870458322944>");
    }

    @Test
    void twoSnapshotsOfTheSameJsonAreEqual() {
        final var one = this.user(FULL);
        final var other = this.user(FULL);

        assertThat(one).isNotSameAs(other).isEqualTo(other).hasSameHashCodeAs(other);
    }

    @Test
    void snapshotsOfDifferentJsonAreNotEqual() {
        assertThat(this.user(FULL)).isNotEqualTo(this.user("""
                        {"id": "159985870458322944", "username": "phinner"}"""));
    }

    @Test
    void neverEqualsAMemberBuiltFromTheSameRawObject() {
        final var raw = Json.parse("""
                {
                  "id": "159985870458322944",
                  "username": "phinner",
                  "user": {"id": "159985870458322944", "username": "phinner"},
                  "roles": []
                }""");
        final var user = Users.fromJson(this.duke, raw);
        final var member = Members.fromJson(this.duke, Snowflake.of("266604749638606850"), raw);

        assertThat(member.raw()).isEqualTo(user.raw());
        assertThat(user).isNotEqualTo(member);
        assertThat(member).isNotEqualTo(user);
    }

    @Test
    void toStringNamesTheId() {
        assertThat(this.user(FULL)).hasToString("User[159985870458322944]");
    }

    @Test
    void openDmDelegatesToTheRestFacade() {
        final var dm = (DmChannel) Channels.fromJson(this.duke, Json.parse("""
                        {
                          "id": "739023850393600000",
                          "type": 1,
                          "recipients": [{"id": "159985870458322944", "username": "phinner"}]
                        }"""));
        this.duke.answer("openDm", args -> dm);
        final var user = this.user(FULL);

        assertThat(user.openDm()).isSameAs(dm);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("openDm", List.of(user.id())));
    }
}
