// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCalculatorTest {

    private static final Snowflake GUILD_ID = Snowflake.of("41771983423143936");
    private static final Snowflake MODERATOR_ID = Snowflake.of("41771983423143937");
    private static final Snowflake VETERAN_ID = Snowflake.of("41771983423143938");
    private static final Snowflake USER_ID = Snowflake.of("302096570298990593");
    private static final Snowflake OWNER_ID = Snowflake.of("175928847299117063");

    private TestDukecord duke;

    @BeforeEach
    void setUp() {
        this.duke = new TestDukecord();
    }

    private Role role(final Snowflake id, final String name, final int position, final PermissionSet permissions) {
        return this.duke.putRole(
                GUILD_ID,
                Json.empty()
                        .with("id", id.asString())
                        .with("name", name)
                        .with("position", position)
                        .with("permissions", permissions.asString()));
    }

    private Role everyone(final PermissionSet permissions) {
        return this.role(GUILD_ID, "@everyone", 0, permissions);
    }

    private Member member(final Snowflake... roleIds) {
        return this.duke.putMember(
                GUILD_ID,
                Json.parse("""
                                {
                                  "user": {"id": "302096570298990593", "username": "phinner"},
                                  "nick": "phinner",
                                  "joined_at": "2016-04-30T11:18:25.796Z",
                                  "roles": []
                                }""")
                        .with(
                                "roles",
                                List.of(roleIds).stream()
                                        .map(Snowflake::asString)
                                        .toList()));
    }

    private static PermissionOverwrite overwrite(
            final Snowflake id,
            final PermissionOverwrite.Type type,
            final PermissionSet allow,
            final PermissionSet deny) {
        return new PermissionOverwrite(id, type, allow, deny);
    }

    // ---- guild level ----

    @Test
    void ownerGetsEveryPermission() {
        final var member = this.member();
        final var permissions =
                PermissionCalculator.guild(USER_ID, this.everyone(PermissionSet.NONE), member, List.of());
        assertThat(permissions).isEqualTo(PermissionSet.ALL);
    }

    @Test
    void nonOwnerGetsTheUnionOfEveryoneAndTheirRoles() {
        final var everyone = this.everyone(PermissionSet.of(Permission.VIEW_CHANNEL));
        final var moderator = this.role(MODERATOR_ID, "moderator", 1, PermissionSet.of(Permission.MANAGE_MESSAGES));
        final var veteran = this.role(VETERAN_ID, "veteran", 2, PermissionSet.of(Permission.SEND_MESSAGES));
        final var member = this.member(MODERATOR_ID, VETERAN_ID);

        assertThat(PermissionCalculator.guild(OWNER_ID, everyone, member, List.of(moderator, veteran)))
                .isEqualTo(PermissionSet.of(
                        Permission.VIEW_CHANNEL, Permission.MANAGE_MESSAGES, Permission.SEND_MESSAGES));
    }

    @Test
    void administratorRoleGetsEveryPermission() {
        final var everyone = this.everyone(PermissionSet.of(Permission.VIEW_CHANNEL));
        final var admin = this.role(MODERATOR_ID, "admin", 1, PermissionSet.of(Permission.ADMINISTRATOR));
        final var member = this.member(MODERATOR_ID);

        assertThat(PermissionCalculator.guild(OWNER_ID, everyone, member, List.of(admin)))
                .isEqualTo(PermissionSet.ALL);
    }

    @Test
    void administratorOnEveryoneGetsEveryPermission() {
        final var everyone = this.everyone(PermissionSet.of(Permission.ADMINISTRATOR));
        assertThat(PermissionCalculator.guild(OWNER_ID, everyone, this.member(), List.of()))
                .isEqualTo(PermissionSet.ALL);
    }

    // ---- channel level ----

    @Test
    void administratorSkipsChannelOverwrites() {
        final var member = this.member(MODERATOR_ID);
        final var denyEverything =
                overwrite(GUILD_ID, PermissionOverwrite.Type.ROLE, PermissionSet.NONE, PermissionSet.ALL);

        assertThat(PermissionCalculator.channel(PermissionSet.ALL, GUILD_ID, member, List.of(denyEverything)))
                .isEqualTo(PermissionSet.ALL);
    }

    @Test
    void appliesEveryoneThenRolesThenMemberOverwrites() {
        final var member = this.member(MODERATOR_ID);
        final var base =
                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES, Permission.READ_MESSAGE_HISTORY);

        // @everyone denies sending, the member's role gives it back, the member overwrite takes it away again.
        final var everyoneDeny = overwrite(
                GUILD_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.NONE,
                PermissionSet.of(Permission.SEND_MESSAGES));
        final var roleAllow = overwrite(
                MODERATOR_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.of(Permission.SEND_MESSAGES),
                PermissionSet.NONE);
        final var memberDeny = overwrite(
                USER_ID,
                PermissionOverwrite.Type.MEMBER,
                PermissionSet.NONE,
                PermissionSet.of(Permission.SEND_MESSAGES));

        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of(everyoneDeny, roleAllow)))
                .isEqualTo(base);
        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of(everyoneDeny, roleAllow, memberDeny)))
                .isEqualTo(base.without(Permission.SEND_MESSAGES).without(PermissionCalculatorTest.requiresSend()));
    }

    @Test
    void roleOverwritesUnionDeniesBeforeUnionOfAllows() {
        final var member = this.member(MODERATOR_ID, VETERAN_ID);
        final var base = PermissionSet.of(
                Permission.VIEW_CHANNEL,
                Permission.SEND_MESSAGES,
                Permission.MANAGE_MESSAGES,
                Permission.ADD_REACTIONS);

        final var moderatorDeny = overwrite(
                MODERATOR_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.NONE,
                PermissionSet.of(Permission.SEND_MESSAGES, Permission.MANAGE_MESSAGES));
        final var veteranAllow = overwrite(
                VETERAN_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.of(Permission.SEND_MESSAGES),
                PermissionSet.NONE);

        // The allow of one role beats the deny of another; a deny nobody allows back sticks.
        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of(moderatorDeny, veteranAllow)))
                .isEqualTo(base.without(Permission.MANAGE_MESSAGES));
    }

    @Test
    void ignoresOverwritesForRolesTheMemberDoesNotHave() {
        final var member = this.member(MODERATOR_ID);
        final var base = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES);
        final var foreign = overwrite(
                VETERAN_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.NONE,
                PermissionSet.of(Permission.SEND_MESSAGES));
        final var otherMember = overwrite(
                OWNER_ID,
                PermissionOverwrite.Type.MEMBER,
                PermissionSet.NONE,
                PermissionSet.of(Permission.SEND_MESSAGES));

        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of(foreign, otherMember)))
                .isEqualTo(base);
    }

    @Test
    void deniedViewChannelYieldsNothingAtAll() {
        final var member = this.member();
        final var base =
                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES, Permission.MANAGE_MESSAGES);
        final var deny = overwrite(
                GUILD_ID, PermissionOverwrite.Type.ROLE, PermissionSet.NONE, PermissionSet.of(Permission.VIEW_CHANNEL));

        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of(deny)))
                .isEqualTo(PermissionSet.NONE);
        assertThat(PermissionCalculator.channel(PermissionSet.NONE, GUILD_ID, member, List.of()))
                .isEqualTo(PermissionSet.NONE);
    }

    @Test
    void deniedSendMessagesStripsTheSendDependentBits() {
        final var member = this.member();
        final var base = PermissionSet.of(
                Permission.VIEW_CHANNEL,
                Permission.READ_MESSAGE_HISTORY,
                Permission.MENTION_EVERYONE,
                Permission.SEND_TTS_MESSAGES,
                Permission.ATTACH_FILES,
                Permission.EMBED_LINKS);

        final var permissions = PermissionCalculator.channel(base, GUILD_ID, member, List.of());
        assertThat(permissions.asSet()).containsExactly(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY);
    }

    @Test
    void keepsTheSendDependentBitsWhenSendMessagesIsGranted() {
        final var member = this.member();
        final var base = PermissionSet.of(
                Permission.VIEW_CHANNEL,
                Permission.SEND_MESSAGES,
                Permission.MENTION_EVERYONE,
                Permission.ATTACH_FILES,
                Permission.EMBED_LINKS,
                Permission.SEND_TTS_MESSAGES);

        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of()))
                .isEqualTo(base);
    }

    @Test
    void deniedConnectStripsTheVoiceBits() {
        final var member = this.member();
        final var voice = PermissionSet.of(
                Permission.SPEAK,
                Permission.STREAM,
                Permission.MUTE_MEMBERS,
                Permission.DEAFEN_MEMBERS,
                Permission.MOVE_MEMBERS,
                Permission.USE_VAD,
                Permission.PRIORITY_SPEAKER,
                Permission.REQUEST_TO_SPEAK,
                Permission.USE_SOUNDBOARD,
                Permission.USE_EXTERNAL_SOUNDS,
                Permission.USE_EMBEDDED_ACTIVITIES);
        final var base = voice.with(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES);

        assertThat(PermissionCalculator.channel(base, GUILD_ID, member, List.of()))
                .isEqualTo(PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES));

        assertThat(PermissionCalculator.channel(base.with(Permission.CONNECT), GUILD_ID, member, List.of()))
                .isEqualTo(base.with(Permission.CONNECT));
    }

    private static PermissionSet requiresSend() {
        return PermissionSet.of(
                Permission.MENTION_EVERYONE,
                Permission.SEND_TTS_MESSAGES,
                Permission.ATTACH_FILES,
                Permission.EMBED_LINKS);
    }
}
