// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import java.awt.Color;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** A new role, or changes to one. Absent attributes take Discord's default on creation and stay as they are on edits. */
@Value.Immutable
@JsonSerialize(as = ImmutableRoleSpec.class)
public interface RoleSpec {

    static Builder builder() {
        return ImmutableRoleSpec.builder();
    }

    Optional<String> name();

    Optional<PermissionSet> permissions();

    Optional<Color> color();

    Optional<Boolean> hoist();

    Optional<Boolean> mentionable();

    Optional<String> unicodeEmoji();

    interface Builder {
        Builder from(RoleSpec instance);

        Builder name(String name);

        Builder name(Optional<String> name);

        Builder permissions(PermissionSet permissions);

        Builder permissions(Optional<? extends PermissionSet> permissions);

        Builder color(Color color);

        Builder color(Optional<? extends Color> color);

        Builder hoist(boolean hoist);

        Builder hoist(Optional<Boolean> hoist);

        Builder mentionable(boolean mentionable);

        Builder mentionable(Optional<Boolean> mentionable);

        Builder unicodeEmoji(String unicodeEmoji);

        Builder unicodeEmoji(Optional<String> unicodeEmoji);

        RoleSpec build();
    }
}
