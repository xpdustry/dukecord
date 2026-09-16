// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowedMentionsTest {

    private static final Snowflake USER = Snowflake.of("302096570298990593");
    private static final Snowflake OTHER_USER = Snowflake.of("175928847299117063");
    private static final Snowflake ROLE = Snowflake.of("41771983423143937");

    @Test
    void noneSuppressesEverything() {
        final var mentions = AllowedMentions.none();
        assertThat(mentions.parse()).isEmpty();
        assertThat(mentions.users()).isEmpty();
        assertThat(mentions.roles()).isEmpty();
        assertThat(mentions.repliedUser()).isFalse();
        assertThat(mentions.toJson()).isEqualTo(Json.parse("{\"parse\": [], \"replied_user\": false}"));
    }

    @Test
    void allEnablesEveryParseTypeAndTheRepliedUser() {
        final var mentions = AllowedMentions.all();
        assertThat(mentions.parse()).containsExactlyInAnyOrder(AllowedMentions.Parse.values());
        assertThat(mentions.repliedUser()).isTrue();
        assertThat(mentions.toJson()).isEqualTo(Json.parse("""
                        {"parse": ["users", "roles", "everyone"], "replied_user": true}"""));
    }

    @Test
    void parseKeysAreDiscordsStrings() {
        assertThat(AllowedMentions.Parse.USERS.key()).isEqualTo("users");
        assertThat(AllowedMentions.Parse.ROLES.key()).isEqualTo("roles");
        assertThat(AllowedMentions.Parse.EVERYONE.key()).isEqualTo("everyone");
    }

    @Test
    void usersWhitelistsExactlyThoseIds() {
        final var mentions = AllowedMentions.users(List.of(USER, OTHER_USER));
        assertThat(mentions.parse()).isEmpty();
        assertThat(mentions.users()).containsExactlyInAnyOrder(USER, OTHER_USER);
        assertThat(mentions.toJson().strings("users"))
                .containsExactlyInAnyOrder(USER.asString(), OTHER_USER.asString());
        assertThat(mentions.toJson().has("roles")).isFalse();
    }

    @Test
    void rolesWhitelistsExactlyThoseIds() {
        final var mentions = AllowedMentions.roles(List.of(ROLE));
        assertThat(mentions.roles()).containsExactly(ROLE);
        assertThat(mentions.toJson().strings("roles")).containsExactly(ROLE.asString());
        assertThat(mentions.toJson().has("users")).isFalse();
    }

    @Test
    void withUsersAndWithRolesAccumulate() {
        final var mentions = AllowedMentions.none()
                .withUsers(List.of(USER))
                .withUsers(List.of(OTHER_USER))
                .withRoles(List.of(ROLE));
        assertThat(mentions.users()).containsExactlyInAnyOrder(USER, OTHER_USER);
        assertThat(mentions.roles()).containsExactly(ROLE);
        assertThat(AllowedMentions.none().users()).isEmpty();
    }

    @Test
    void withEveryoneAddsOnlyTheEveryoneParseType() {
        final var mentions = AllowedMentions.none().withEveryone();
        assertThat(mentions.parse()).containsExactly(AllowedMentions.Parse.EVERYONE);
        assertThat(mentions.toJson().strings("parse")).containsExactly("everyone");
        assertThat(AllowedMentions.none().parse()).isEmpty();
    }

    @Test
    void withRepliedUserTogglesThePing() {
        assertThat(AllowedMentions.none().withRepliedUser(true).repliedUser()).isTrue();
        assertThat(AllowedMentions.all().withRepliedUser(false).repliedUser()).isFalse();
        assertThat(AllowedMentions.all().withRepliedUser(false).toJson().bool("replied_user"))
                .isFalse();
    }

    @Test
    void omitsTheUserListWhenTheUsersParseTypeIsPresent() {
        final var mentions =
                new AllowedMentions(Set.of(AllowedMentions.Parse.USERS), Set.of(USER), Set.of(ROLE), false);
        final var json = mentions.toJson();
        assertThat(json.has("users")).isFalse();
        assertThat(json.strings("roles")).containsExactly(ROLE.asString());
    }

    @Test
    void omitsTheRoleListWhenTheRolesParseTypeIsPresent() {
        final var mentions =
                new AllowedMentions(Set.of(AllowedMentions.Parse.ROLES), Set.of(USER), Set.of(ROLE), false);
        final var json = mentions.toJson();
        assertThat(json.has("roles")).isFalse();
        assertThat(json.strings("users")).containsExactly(USER.asString());
    }

    @Test
    void writesParseInDeclarationOrderRegardlessOfSetOrder() {
        final var mentions = new AllowedMentions(
                Set.of(AllowedMentions.Parse.EVERYONE, AllowedMentions.Parse.USERS), Set.of(), Set.of(), false);
        assertThat(mentions.toJson().strings("parse")).containsExactly("users", "everyone");
    }

    @Test
    void isAValueWithUnmodifiableSets() {
        assertThat(AllowedMentions.none()).isEqualTo(AllowedMentions.none());
        assertThat(AllowedMentions.none()).hasSameHashCodeAs(AllowedMentions.none());
        assertThat(AllowedMentions.none()).isNotEqualTo(AllowedMentions.all());
        assertThatThrownBy(() -> AllowedMentions.users(List.of(USER)).users().add(OTHER_USER))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
