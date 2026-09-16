// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.CategoryChannel;
import dev.phinner.dukecord.model.channel.ChannelCreateSpec;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.channel.GuildTextChannel;
import dev.phinner.dukecord.model.channel.ThreadChannel;
import dev.phinner.dukecord.model.permission.Permission;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.support.TestDukecord;
import dev.phinner.dukecord.support.TestDukecord.RestCall;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuildTest {

    private static final Snowflake GUILD_ID = Snowflake.of("266604749638606850");
    private static final Snowflake OTHER_GUILD_ID = Snowflake.of("266604749638606851");
    private static final Snowflake OWNER_ID = Snowflake.of("159985870458322944");
    private static final Snowflake SELF_ID = Snowflake.of("1");
    private static final Snowflake MEMBER_ID = Snowflake.of("409751293745692672");
    private static final Snowflake MODERATOR_ID = Snowflake.of("739023850393600001");
    private static final Snowflake GENERAL_ID = Snowflake.of("739023850393700001");
    private static final Snowflake ANNOUNCEMENTS_ID = Snowflake.of("739023850393700002");
    private static final Snowflake CATEGORY_ID = Snowflake.of("739023850393700003");
    private static final Snowflake THREAD_ID = Snowflake.of("739023850393700004");

    private static final String FULL = """
            {
              "id": "266604749638606850",
              "name": "Xpdustry",
              "owner_id": "159985870458322944",
              "description": "A Mindustry community",
              "icon": "c_9f8e7d6c5b4a39281706f5e4d3c2b1a0",
              "features": ["COMMUNITY", "NEWS"],
              "member_count": 4200,
              "premium_subscription_count": 7,
              "large": false
            }""";

    private final TestDukecord duke = new TestDukecord();

    private Guild guild() {
        return this.duke.putGuild(Json.parse(FULL));
    }

    private Member putMember(final Snowflake guildId, final Snowflake userId, final List<Snowflake> roleIds) {
        return this.duke.putMember(guildId, Json.parse("""
                        {
                          "user": {"id": "%s", "username": "user-%s"},
                          "roles": [%s],
                          "joined_at": "2021-05-01T12:00:00.000000+00:00"
                        }""".formatted(
                        userId.asString(),
                        userId.asString(),
                        roleIds.stream().map(id -> "\"" + id.asString() + "\"").collect(Collectors.joining(", ")))));
    }

    private Role putRole(
            final Snowflake guildId, final Snowflake roleId, final int position, final String permissions) {
        return this.duke.putRole(
                guildId, Json.parse("""
                        {"id": "%s", "name": "role-%s", "position": %d, "permissions": "%s"}""".formatted(roleId.asString(), roleId.asString(), position, permissions)));
    }

    private GuildTextChannel putTextChannel(
            final Snowflake guildId, final Snowflake channelId, final String name, final int position) {
        return (GuildTextChannel) this.duke.putChannel(
                Json.parse("""
                        {"id": "%s", "type": 0, "guild_id": "%s", "name": "%s", "position": %d}""".formatted(channelId.asString(), guildId.asString(), name, position)));
    }

    // ---- scalars ----

    @Test
    void parsesEveryModelledField() {
        final var guild = this.guild();

        assertThat(guild.id()).isEqualTo(GUILD_ID);
        assertThat(guild.name()).isEqualTo("Xpdustry");
        assertThat(guild.owner().id()).isEqualTo(OWNER_ID);
        assertThat(guild.description()).contains("A Mindustry community");
        assertThat(guild.iconHash()).contains("c_9f8e7d6c5b4a39281706f5e4d3c2b1a0");
        assertThat(guild.features()).containsExactlyInAnyOrder("COMMUNITY", "NEWS");
        assertThat(guild.memberCount()).contains(4200);
        assertThat(guild.boostCount()).contains(7);
        assertThat(guild.duke()).isSameAs(this.duke);
    }

    @Test
    void singleArgumentFactoryDerivesAvailabilityFromTheUnavailableFlag() {
        final var available = Guilds.fromJson(this.duke, Json.parse(FULL));
        final var unavailable = Guilds.fromJson(this.duke, Json.parse("""
                        {"id": "266604749638606850", "name": "Xpdustry", "owner_id": "159985870458322944", "unavailable": true}"""));

        assertThat(available.isAvailable()).isTrue();
        assertThat(available.memberCompleteness()).isEqualTo(MemberCompleteness.NONE);
        assertThat(unavailable.isAvailable()).isFalse();
    }

    @Test
    void cacheFactoryTakesAvailabilityAndCompletenessFromItsCaller() {
        final var guild = Guilds.fromJson(this.duke, Json.parse(FULL), false, MemberCompleteness.COMPLETE);

        assertThat(guild.isAvailable()).isFalse();
        assertThat(guild.memberCompleteness()).isEqualTo(MemberCompleteness.COMPLETE);
    }

    @Test
    void countsAreAbsentWhenTheFieldsAre() {
        final var guild = Guilds.fromJson(this.duke, Json.parse("""
                        {"id": "266604749638606850", "name": "Xpdustry", "owner_id": "159985870458322944"}"""));

        assertThat(guild.memberCount()).isEmpty();
        assertThat(guild.boostCount()).isEmpty();
        assertThat(guild.features()).isEmpty();
        assertThat(guild.description()).isEmpty();
        assertThat(guild.iconHash()).isEmpty();
    }

    // ---- member navigation ----

    @Test
    void memberReadsTheCacheOfThisGuild() {
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of());
        this.putMember(OTHER_GUILD_ID, OWNER_ID, List.of());

        assertThat(this.guild().member(MEMBER_ID)).contains(member);
        assertThat(this.guild().member(OWNER_ID)).isEmpty();
    }

    @Test
    void membersListsTheKnownMembersOfThisGuildOnly() {
        final var first = this.putMember(GUILD_ID, MEMBER_ID, List.of());
        final var second = this.putMember(GUILD_ID, OWNER_ID, List.of());
        this.putMember(OTHER_GUILD_ID, SELF_ID, List.of());

        assertThat(this.guild().members()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void selfMemberIsTheBotsOwnMember() {
        final var self = this.putMember(GUILD_ID, SELF_ID, List.of());

        assertThat(this.guild().selfMember()).isEqualTo(self);
    }

    @Test
    void selfMemberThrowsWhenTheBotIsNotCached() {
        final var guild = this.guild();

        assertThatThrownBy(guild::selfMember)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bot member of guild 266604749638606850 is not cached");
    }

    // ---- role navigation ----

    @Test
    void roleFiltersOutRolesOfOtherGuilds() {
        final var moderator = this.putRole(GUILD_ID, MODERATOR_ID, 5, "0");
        final var foreign = this.putRole(OTHER_GUILD_ID, Snowflake.of("739023850393600009"), 5, "0");

        assertThat(this.guild().role(MODERATOR_ID)).contains(moderator);
        assertThat(this.guild().role(foreign.id())).isEmpty();
    }

    @Test
    void everyoneRoleIsTheRoleSharingTheGuildId() {
        final var everyone = this.putRole(GUILD_ID, GUILD_ID, 0, "3072");

        assertThat(this.guild().everyoneRole()).isEqualTo(everyone);
    }

    @Test
    void everyoneRoleThrowsWhenItIsNotCached() {
        final var guild = this.guild();

        assertThatThrownBy(guild::everyoneRole)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("@everyone role of guild 266604749638606850 is not cached");
    }

    @Test
    void rolesAreOrderedByPositionHighestFirst() {
        final var everyone = this.putRole(GUILD_ID, GUILD_ID, 0, "0");
        final var moderator = this.putRole(GUILD_ID, MODERATOR_ID, 5, "0");
        final var admin = this.putRole(GUILD_ID, Snowflake.of("739023850393600003"), 9, "0");
        this.putRole(OTHER_GUILD_ID, Snowflake.of("739023850393600004"), 99, "0");

        assertThat(this.guild().roles()).containsExactly(admin, moderator, everyone);
    }

    // ---- channel navigation ----

    @Test
    void channelFiltersOutChannelsOfOtherGuilds() {
        final var general = this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        this.putTextChannel(OTHER_GUILD_ID, ANNOUNCEMENTS_ID, "announcements", 1);

        assertThat(this.guild().channel(GENERAL_ID)).contains(general);
        assertThat(this.guild().channel(ANNOUNCEMENTS_ID)).isEmpty();
    }

    @Test
    void channelFiltersByRequestedType() {
        this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        this.duke.putChannel(Json.parse("""
                {"id": "739023850393700003", "type": 4, "guild_id": "266604749638606850", "name": "text", "position": 0}"""));

        assertThat(this.guild().channel(GENERAL_ID, GuildTextChannel.class)).isPresent();
        assertThat(this.guild().channel(GENERAL_ID, CategoryChannel.class)).isEmpty();
        assertThat(this.guild().channel(CATEGORY_ID, CategoryChannel.class)).isPresent();
    }

    @Test
    void channelIgnoresChannelsThatAreNotBoundToAGuild() {
        final var dm = this.duke.putChannel(Json.parse("""
                {"id": "739023850393700008", "type": 1, "recipients": [{"id": "1", "username": "dukecord"}]}"""));

        assertThat(this.guild().channel(dm.id())).isEmpty();
        assertThat(this.guild().channels()).isEmpty();
    }

    @Test
    void channelsAreOrderedByPosition() {
        final var second = this.putTextChannel(GUILD_ID, ANNOUNCEMENTS_ID, "announcements", 5);
        final var first = this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        this.putTextChannel(OTHER_GUILD_ID, Snowflake.of("739023850393700009"), "foreign", 0);

        assertThat(this.guild().channels()).containsExactly(first, second);
    }

    @Test
    void threadsListOnlyTheCachedThreads() {
        this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        final var thread = this.putThread(GUILD_ID, THREAD_ID, GENERAL_ID);

        assertThat(this.guild().threads()).containsExactly(thread);
    }

    private ThreadChannel putThread(final Snowflake guildId, final Snowflake threadId, final Snowflake parentId) {
        return (ThreadChannel) this.duke.putChannel(
                Json.parse("""
                        {
                          "id": "%s",
                          "type": 11,
                          "guild_id": "%s",
                          "parent_id": "%s",
                          "owner_id": "159985870458322944",
                          "name": "help-thread",
                          "thread_metadata": {
                            "archived": false,
                            "auto_archive_duration": 1440,
                            "archive_timestamp": "2024-01-01T00:00:00.000000+00:00",
                            "locked": false
                          }
                        }""".formatted(threadId.asString(), guildId.asString(), parentId.asString())));
    }

    // ---- permissions ----

    @Test
    void guildPermissionsUnionTheEveryoneRoleAndTheMemberRoles() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        this.putRole(GUILD_ID, MODERATOR_ID, 5, "8192");
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of(MODERATOR_ID));

        assertThat(this.guild().permissionsOf(member))
                .isEqualTo(PermissionSet.of(
                        Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES, Permission.MANAGE_MESSAGES));
    }

    @Test
    void guildPermissionsOfTheOwnerAreEverything() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "0");
        final var owner = this.putMember(GUILD_ID, OWNER_ID, List.of());

        assertThat(this.guild().permissionsOf(owner)).isEqualTo(PermissionSet.ALL);
    }

    @Test
    void channelPermissionsApplyTheOverwritesOnTopOfTheGuildOnes() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        this.putRole(GUILD_ID, MODERATOR_ID, 5, "8192");
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of(MODERATOR_ID));
        final var channel = (GuildChannel) this.duke.putChannel(Json.parse(this.lockedChannel(GENERAL_ID)));

        assertThat(this.guild().permissionsOf(member, channel))
                .isEqualTo(PermissionSet.of(
                        Permission.VIEW_CHANNEL, Permission.MANAGE_MESSAGES, Permission.ADD_REACTIONS));
    }

    private String lockedChannel(final Snowflake channelId) {
        return """
                {
                  "id": "%s",
                  "type": 0,
                  "guild_id": "266604749638606850",
                  "name": "general",
                  "position": 1,
                  "permission_overwrites": [
                    {"id": "266604749638606850", "type": 0, "allow": "0", "deny": "2048"},
                    {"id": "409751293745692672", "type": 1, "allow": "64", "deny": "0"}
                  ]
                }""".formatted(channelId.asString());
    }

    @Test
    void channelPermissionsOfTheOwnerIgnoreEveryOverwrite() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        final var owner = this.putMember(GUILD_ID, OWNER_ID, List.of());
        final var channel = (GuildChannel) this.duke.putChannel(Json.parse(this.lockedChannel(GENERAL_ID)));

        assertThat(this.guild().permissionsOf(owner, channel)).isEqualTo(PermissionSet.ALL);
    }

    @Test
    void threadPermissionsResolveThroughTheParentChannel() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        this.putRole(GUILD_ID, MODERATOR_ID, 5, "8192");
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of(MODERATOR_ID));
        this.duke.putChannel(Json.parse(this.lockedChannel(GENERAL_ID)));
        final var thread = this.putThread(GUILD_ID, THREAD_ID, GENERAL_ID);

        assertThat(thread.overwrites()).isEmpty();
        assertThat(this.guild().permissionsOf(member, thread))
                .isEqualTo(PermissionSet.of(
                        Permission.VIEW_CHANNEL, Permission.MANAGE_MESSAGES, Permission.ADD_REACTIONS));
    }

    @Test
    void threadPermissionsThrowWhenTheParentIsNotCached() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of());
        final var thread = this.putThread(GUILD_ID, THREAD_ID, GENERAL_ID);
        final var guild = this.guild();

        assertThatThrownBy(() -> guild.permissionsOf(member, thread))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Parent of thread 739023850393700004 is not cached");
    }

    // ---- REST ----

    @Test
    void fetchMemberDelegatesToTheRestFacade() {
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of());
        this.duke.answer("fetchMember", args -> member);

        assertThat(this.guild().fetchMember(MEMBER_ID)).isSameAs(member);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("fetchMember", List.of(GUILD_ID, MEMBER_ID)));
    }

    @Test
    void fetchMembersDelegatesToTheRestFacade() {
        final var member = this.putMember(GUILD_ID, MEMBER_ID, List.of());
        this.duke.answer("fetchMembers", args -> List.of(member));
        final var query = MemberQuery.list(100);

        assertThat(this.guild().fetchMembers(query)).containsExactly(member);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("fetchMembers", List.of(GUILD_ID, query)));
    }

    @Test
    void createTextChannelDelegatesToTheRestFacade() {
        final var created = this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        this.duke.answer("createTextChannel", args -> created);
        final var spec = ChannelCreateSpec.text("general").build();

        assertThat(this.guild().createTextChannel(spec)).isSameAs(created);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("createTextChannel", List.of(GUILD_ID, spec)));
    }

    @Test
    void createCategoryDelegatesToTheRestFacade() {
        final var created = (CategoryChannel) this.duke.putChannel(Json.parse("""
                {"id": "739023850393700003", "type": 4, "guild_id": "266604749638606850", "name": "staff", "position": 0}"""));
        this.duke.answer("createCategory", args -> created);
        final var spec = ChannelCreateSpec.category("staff").build();

        assertThat(this.guild().createCategory(spec)).isSameAs(created);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("createCategory", List.of(GUILD_ID, spec)));
    }

    @Test
    void fetchActiveThreadsDelegatesToTheRestFacade() {
        this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        final var thread = this.putThread(GUILD_ID, THREAD_ID, GENERAL_ID);
        this.duke.answer("fetchActiveThreads", args -> List.of(thread));

        assertThat(this.guild().fetchActiveThreads()).containsExactly(thread);
        assertThat(this.duke.restCalls()).containsExactly(new RestCall("fetchActiveThreads", List.of(GUILD_ID)));
    }

    @Test
    void cacheNavigationNeverTouchesTheRestFacade() {
        this.putRole(GUILD_ID, GUILD_ID, 0, "3072");
        final var member = this.putMember(GUILD_ID, SELF_ID, List.of());
        this.putTextChannel(GUILD_ID, GENERAL_ID, "general", 1);
        final var guild = this.guild();

        assertThat(guild.members()).containsExactly(member);
        assertThat(guild.roles()).hasSize(1);
        assertThat(guild.channels()).hasSize(1);
        assertThat(guild.threads()).isEmpty();
        assertThat(guild.selfMember()).isEqualTo(member);
        assertThat(guild.permissionsOf(member))
                .isEqualTo(PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES));
        assertThat(this.duke.restCalls()).isEmpty();
    }
}
