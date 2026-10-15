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
        Builder from(final RoleSpec instance);

        Builder name(final String name);

        Builder permissions(final PermissionSet permissions);

        Builder color(final Color color);

        Builder hoist(final boolean hoist);

        Builder mentionable(final boolean mentionable);

        Builder unicodeEmoji(final String unicodeEmoji);

        RoleSpec build();
    }
}
