// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Role;
import java.util.List;

/**
 * Discord's effective permission algorithm. Pure: takes snapshots in, returns a set. See the "Permission Overwrites"
 * section of the permissions documentation for the eight steps this implements.
 */
public final class PermissionCalculator {

    private static final PermissionSet REQUIRES_SEND = PermissionSet.of(
            Permission.MENTION_EVERYONE, Permission.SEND_TTS_MESSAGES, Permission.ATTACH_FILES, Permission.EMBED_LINKS);
    private static final PermissionSet REQUIRES_CONNECT = PermissionSet.of(
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

    /** Guild-level permissions: owner gets all, otherwise the union of {@code @everyone} and the member's roles. */
    public static PermissionSet guild(
            final Snowflake ownerId, final Role everyone, final Member member, final List<Role> memberRoles) {
        if (member.id().equals(ownerId)) {
            return PermissionSet.ALL;
        }
        var permissions = everyone.permissions();
        for (final var role : memberRoles) {
            permissions = permissions.with(role.permissions());
        }
        return permissions.contains(Permission.ADMINISTRATOR) ? PermissionSet.ALL : permissions;
    }

    /** Applies channel overwrites on top of guild-level permissions, then Discord's implicit denies. */
    public static PermissionSet channel(
            final PermissionSet guildPermissions,
            final Snowflake guildId,
            final Member member,
            final List<PermissionOverwrite> overwrites) {
        if (guildPermissions.contains(Permission.ADMINISTRATOR)) {
            return PermissionSet.ALL;
        }
        var permissions = guildPermissions;

        for (final var overwrite : overwrites) {
            if (overwrite.type() == PermissionOverwrite.Type.ROLE
                    && overwrite.id().equals(guildId)) {
                permissions = permissions.without(overwrite.deny()).with(overwrite.allow());
            }
        }

        var roleAllow = PermissionSet.NONE;
        var roleDeny = PermissionSet.NONE;
        for (final var overwrite : overwrites) {
            if (overwrite.type() == PermissionOverwrite.Type.ROLE
                    && !overwrite.id().equals(guildId)
                    && member.roles().contains(overwrite.id())) {
                roleAllow = roleAllow.with(overwrite.allow());
                roleDeny = roleDeny.with(overwrite.deny());
            }
        }
        permissions = permissions.without(roleDeny).with(roleAllow);

        for (final var overwrite : overwrites) {
            if (overwrite.type() == PermissionOverwrite.Type.MEMBER
                    && overwrite.id().equals(member.id())) {
                permissions = permissions.without(overwrite.deny()).with(overwrite.allow());
            }
        }

        if (!permissions.contains(Permission.VIEW_CHANNEL)) {
            return PermissionSet.NONE;
        }
        if (!permissions.contains(Permission.SEND_MESSAGES)) {
            permissions = permissions.without(REQUIRES_SEND);
        }
        if (!permissions.contains(Permission.CONNECT)) {
            permissions = permissions.without(REQUIRES_CONNECT);
        }
        return permissions;
    }

    private PermissionCalculator() {}
}
