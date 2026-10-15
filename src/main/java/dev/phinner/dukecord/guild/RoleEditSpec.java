// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import java.awt.Color;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** Changes to a role. Absent attributes stay as they are. */
@Value.Immutable
@JsonSerialize(as = ImmutableRoleEditSpec.class)
public interface RoleEditSpec {

    static Builder builder() {
        return ImmutableRoleEditSpec.builder();
    }

    Optional<String> name();

    Optional<PermissionSet> permissions();

    Optional<Color> color();

    Optional<Boolean> hoist();

    Optional<Boolean> mentionable();

    Optional<String> unicodeEmoji();

    interface Builder {
        Builder from(final RoleEditSpec instance);

        Builder name(final String name);

        Builder permissions(final PermissionSet permissions);

        Builder color(final Color color);

        Builder hoist(final boolean hoist);

        Builder mentionable(final boolean mentionable);

        Builder unicodeEmoji(final String unicodeEmoji);

        RoleEditSpec build();
    }
}
