// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import java.awt.Color;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** A new role. Absent attributes take Discord's defaults. */
@Value.Immutable
@JsonSerialize(as = ImmutableRoleCreateSpec.class)
public interface RoleCreateSpec {

    static Builder builder() {
        return ImmutableRoleCreateSpec.builder();
    }

    Optional<String> name();

    Optional<PermissionSet> permissions();

    Optional<Color> color();

    Optional<Boolean> hoist();

    Optional<Boolean> mentionable();

    Optional<String> unicodeEmoji();

    interface Builder {
        Builder from(final RoleCreateSpec instance);

        Builder name(final String name);

        Builder permissions(final PermissionSet permissions);

        Builder color(final Color color);

        Builder hoist(final boolean hoist);

        Builder mentionable(final boolean mentionable);

        Builder unicodeEmoji(final String unicodeEmoji);

        RoleCreateSpec build();
    }
}
