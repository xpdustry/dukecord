// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.Permission;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.model.user.Users;
import dev.phinner.dukecord.support.TestDukecord;
import dev.phinner.dukecord.support.TestDukecord.RestCall;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    private static final Snowflake GUILD_ID = Snowflake.of("266604749638606850");
    private static final Snowflake OTHER_GUILD_ID = Snowflake.of("266604749638606851");
    private static final Snowflake USER_ID = Snowflake.of("159985870458322944");
    private static final Snowflake MODERATOR_ID = Snowflake.of("739023850393600001");
    private static final Snowflake VETERAN_ID = Snowflake.of("739023850393600002");

    private static final String FULL = """
            {
              "user": {"id": "159985870458322944", "username": "phinner", "global_name": "Phinner"},
              "nick": "Duke",
              "avatar": "b_0011223344556677889900aabbccddee",
              "roles": ["739023850393600001", "739023850393600002"],
              "joined_at": "2021-05-01T12:00:00.000000+00:00",
              "premium_since": "2023-02-14T09:30:00.000000+00:00",
              "communication_disabled_until": "2099-01-01T00:00:00.000000+00:00",
              "deaf": false,
              "mute": false,
              "pending": true,
              "flags": 9,
              "permissions": "2147483648"
            }""";

    private final TestDukecord duke = new TestDukecord();

    private Member member(final String json) {
        return Members.fromJson(this.duke, GUILD_ID, Json.parse(json));
    }

    private Role putRole(final Snowflake guildId, final Snowflake roleId, final String name, final int position) {
        return this.duke.putRole(guildId, Json.parse("""
                        {"id": "%s", "name": "%s", "position": %d, "permissions": "0"}""".formatted(roleId.asString(), name, position)));
    }

    @Test
    void parsesEveryModelledFieldFromAnInlineUser() {
        final var member = this.member(FULL);

        assertThat(member.id()).isEqualTo(USER_ID);
        assertThat(member.guild().id()).isEqualTo(GUILD_ID);
        assertThat(member.user().username()).isEqualTo("phinner");
        assertThat(member.nickname()).contains("Duke");
        assertThat(member.avatarHash()).contains("b_0011223344556677889900aabbccddee");
        assertThat(member.roles().ids()).containsExactly(MODERATOR_ID, VETERAN_ID);
        assertThat(member.joinedAt()).contains(Instant.parse("2021-05-01T12:00:00Z"));
        assertThat(member.boostingSince()).contains(Instant.parse("2023-02-14T09:30:00Z"));
        assertThat(member.timeoutUntil()).contains(Instant.parse("2099-01-01T00:00:00Z"));
        assertThat(member.isPending()).isTrue();
        assertThat(member.duke()).isSameAs(this.duke);
    }

    @Test
    void idIsTheUserIdRatherThanAnyIdOnTheMemberObject() {
        final var member = this.member("""
                {"user": {"id": "159985870458322944", "username": "phinner"}, "roles": []}""");

        assertThat(member.id()).isEqualTo(USER_ID);
        assertThat(member.id()).isEqualTo(member.user().id());
    }

    @Test
    void leavesTheOptionalFieldsEmptyWhenTheyAreAbsent() {
        final var member = this.member("""
                {"user": {"id": "159985870458322944", "username": "phinner"}, "roles": []}""");

        assertThat(member.nickname()).isEmpty();
        assertThat(member.avatarHash()).isEmpty();
        assertThat(member.joinedAt()).isEmpty();
        assertThat(member.boostingSince()).isEmpty();
        assertThat(member.timeoutUntil()).isEmpty();
        assertThat(member.isPending()).isFalse();
        assertThat(member.flags()).isEmpty();
        assertThat(member.roles().isEmpty()).isTrue();
    }

    @Test
    void mergesASeparateUserIntoTheRawObject() {
        final var user = Users.fromJson(this.duke, Json.parse("""
                        {"id": "159985870458322944", "username": "phinner", "global_name": "Phinner"}"""));
        final var payload = Json.parse("""
                {"nick": "Duke", "roles": ["739023850393600001"], "joined_at": "2021-05-01T12:00:00.000000+00:00"}""");

        final var member = Members.fromJson(this.duke, GUILD_ID, payload, user);

        assertThat(payload.has("user")).isFalse();
        assertThat(member.raw().object("user")).isEqualTo(user.raw());
        assertThat(member.user()).isEqualTo(user);
        assertThat(member.id()).isEqualTo(USER_ID);
        assertThat(member.nickname()).contains("Duke");
        assertThat(member.roles().ids()).containsExactly(MODERATOR_ID);
    }

    @Test
    void decodesTheFlagsBitmask() {
        assertThat(this.member(FULL).flags())
                .containsExactlyInAnyOrder(MemberFlag.DID_REJOIN, MemberFlag.STARTED_ONBOARDING);
    }

    @Test
    void ignoresUnknownFlagBits() {
        final var member = this.member("""
                {"user": {"id": "159985870458322944", "username": "phinner"}, "roles": [], "flags": 262146}""");

        assertThat(member.flags()).containsExactly(MemberFlag.COMPLETED_ONBOARDING);
    }

    @Test
    void flagsAreACopyTheCallerMayMutate() {
        final var member = this.member(FULL);
        final var flags = member.flags();

        flags.clear();

        assertThat(member.flags()).hasSize(2);
    }

    @Test
    void exposesPrecomputedPermissionsWhenThePayloadCarriesThem() {
        assertThat(this.member(FULL).permissions()).contains(PermissionSet.of(Permission.USE_APPLICATION_COMMANDS));
    }

    @Test
    void hasNoPrecomputedPermissionsWithoutTheField() {
        assertThat(this.member("""
                                {"user": {"id": "159985870458322944", "username": "phinner"}, "roles": []}""").permissions()).isEmpty();
    }

    @Test
    void isTimedOutWhileTheTimeoutIsInTheFuture() {
        assertThat(this.member(FULL).isTimedOut()).isTrue();
    }

    @Test
    void isNotTimedOutOnceTheTimeoutHasPassed() {
        final var member = this.member("""
                {
                  "user": {"id": "159985870458322944", "username": "phinner"},
                  "roles": [],
                  "communication_disabled_until": "2020-01-01T00:00:00.000000+00:00"
                }""");

        assertThat(member.timeoutUntil()).contains(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(member.isTimedOut()).isFalse();
    }

    @Test
    void isNotTimedOutWithoutATimeout() {
        assertThat(this.member("""
                                {"user": {"id": "159985870458322944", "username": "phinner"}, "roles": []}""").isTimedOut()).isFalse();
    }

    @Test
    void displayNamePrefersTheNickname() {
        assertThat(this.member(FULL).displayName()).isEqualTo("Duke");
    }

    @Test
    void displayNameFallsBackToTheGlobalNameWithoutANickname() {
        final var member = this.member("""
                {
                  "user": {"id": "159985870458322944", "username": "phinner", "global_name": "Phinner"},
                  "nick": null,
                  "roles": []
                }""");

        assertThat(member.displayName()).isEqualTo("Phinner");
    }

    @Test
    void displayNameFallsBackToTheUsernameWithoutANicknameOrGlobalName() {
        final var member = this.member("""
                {
                  "user": {"id": "159985870458322944", "username": "phinner", "global_name": null},
                  "roles": []
                }""");

        assertThat(member.displayName()).isEqualTo("phinner");
    }

    @Test
    void mentionIsTheUserMention() {
        assertThat(this.member(FULL).mention()).isEqualTo("<@159985870458322944>");
    }

    @Test
    void hasRoleChecksTheRoleIdsWithoutReadingTheCache() {
        final var member = this.member(FULL);

        assertThat(member.hasRole(MODERATOR_ID)).isTrue();
        assertThat(member.hasRole(VETERAN_ID)).isTrue();
        assertThat(member.hasRole(GUILD_ID)).isFalse();
        assertThat(this.duke.restCalls()).isEmpty();
    }

    @Test
    void rolesGetOmitsUncachedAndForeignRoles() {
        final var moderator = this.putRole(GUILD_ID, MODERATOR_ID, "Moderator", 5);
        this.putRole(OTHER_GUILD_ID, VETERAN_ID, "Veteran", 3);

        final var member = this.member(FULL);

        assertThat(member.roles().ids()).containsExactly(MODERATOR_ID, VETERAN_ID);
        assertThat(member.roles().get()).containsExactly(moderator);
    }

    @Test
    void rolesFetchIssuesASingleRequestForEveryRoleOfTheGuild() {
        final var moderator = Roles.fromJson(this.duke, GUILD_ID, Json.parse("""
                        {"id": "739023850393600001", "name": "Moderator", "position": 5, "permissions": "0"}"""));
        final var unrelated = Roles.fromJson(this.duke, GUILD_ID, Json.parse("""
                        {"id": "739023850393600008", "name": "Unrelated", "position": 1, "permissions": "0"}"""));
        this.duke.answer("fetchRoles", args -> List.of(moderator, unrelated));

        assertThat(this.member(FULL).roles().fetch()).containsExactly(moderator);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("fetchRoles", List.of(GUILD_ID)));
    }

    @Test
    void roleRefsFetchOneRoleEach() {
        final var moderator = Roles.fromJson(this.duke, GUILD_ID, Json.parse("""
                        {"id": "739023850393600001", "name": "Moderator", "position": 5, "permissions": "0"}"""));
        this.duke.answer("fetchRole", args -> moderator);

        assertThat(this.member(FULL).roles().refs().getFirst().fetch()).isSameAs(moderator);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("fetchRole", List.of(GUILD_ID, MODERATOR_ID)));
    }

    @Test
    void highestRoleIsTheHighestPositionedCachedRole() {
        this.putRole(GUILD_ID, MODERATOR_ID, "Moderator", 5);
        final var veteran = this.putRole(GUILD_ID, VETERAN_ID, "Veteran", 9);

        assertThat(this.member(FULL).highestRole()).contains(veteran);
    }

    @Test
    void highestRoleIsEmptyWhileNoRoleIsCached() {
        assertThat(this.member(FULL).highestRole()).isEmpty();
    }

    @Test
    void twoSnapshotsOfTheSameJsonAreEqual() {
        assertThat(this.member(FULL)).isEqualTo(this.member(FULL)).hasSameHashCodeAs(this.member(FULL));
    }

    @Test
    void toStringNamesTheGuildAndTheUser() {
        assertThat(this.member(FULL)).hasToString("Member[266604749638606850/159985870458322944]");
    }

    @Test
    void addRoleDelegatesToTheRestFacade() {
        this.member(FULL).addRole(MODERATOR_ID);

        assertThat(this.duke.restCalls())
                .containsExactly(new RestCall("addMemberRole", List.of(GUILD_ID, USER_ID, MODERATOR_ID)));
    }

    @Test
    void addRoleWithAReasonDelegatesToTheRestFacade() {
        this.member(FULL).addRole(MODERATOR_ID, "rank sync");

        assertThat(this.duke.restCalls())
                .containsExactly(new RestCall("addMemberRole", List.of(GUILD_ID, USER_ID, MODERATOR_ID, "rank sync")));
    }

    @Test
    void removeRoleDelegatesToTheRestFacade() {
        this.member(FULL).removeRole(VETERAN_ID);

        assertThat(this.duke.restCalls())
                .containsExactly(new RestCall("removeMemberRole", List.of(GUILD_ID, USER_ID, VETERAN_ID)));
    }

    @Test
    void removeRoleWithAReasonDelegatesToTheRestFacade() {
        this.member(FULL).removeRole(VETERAN_ID, "rank sync");

        assertThat(this.duke.restCalls())
                .containsExactly(new RestCall("removeMemberRole", List.of(GUILD_ID, USER_ID, VETERAN_ID, "rank sync")));
    }

    @Test
    void editDelegatesToTheRestFacadeAndReturnsTheFreshSnapshot() {
        final var edited = this.member("""
                {"user": {"id": "159985870458322944", "username": "phinner"}, "nick": "Renamed", "roles": []}""");
        this.duke.answer("editMember", args -> edited);
        final var spec =
                MemberEditSpec.builder().nickname("Renamed").reason("rank sync").build();

        assertThat(this.member(FULL).edit(spec)).isSameAs(edited);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("editMember", List.of(GUILD_ID, USER_ID, spec)));
    }

    @Test
    void kickDelegatesToTheRestFacade() {
        this.member(FULL).kick("spam");

        assertThat(this.duke.restCalls())
                .containsExactly(new RestCall("kickMember", List.of(GUILD_ID, USER_ID, "spam")));
    }

    @Test
    void restVerbsWithoutAStubbedAnswerAreStillRecorded() {
        final var member = this.member(FULL);

        assertThatThrownBy(() -> member.edit(MemberEditSpec.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(this.duke.restCalls()).extracting(RestCall::method).containsExactly("editMember");
    }
}
