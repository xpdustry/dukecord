// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.permission.PermissionSet;

public interface Role extends DiscordEntity {

    DiscordEntityRef<Guild> guild();

    String name();

    /** RGB, {@code 0} meaning "no colour". */
    int color();

    int position();

    PermissionSet permissions();

    boolean isHoisted();

    boolean isManaged();

    boolean isMentionable();

    /** The {@code @everyone} role shares the guild's id. */
    default boolean isEveryone() {
        return this.id().equals(this.guild().id());
    }

    default String mention() {
        return "<@&" + this.id() + ">";
    }
}
