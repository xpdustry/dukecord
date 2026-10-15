// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record RoleBehaviorImpl(Dukecord duke, Snowflake guildId, Snowflake id) implements RoleBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof RoleBehaviorImpl that && this.guildId.equals(that.guildId) && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31 * this.guildId.hashCode() + this.id.hashCode();
    }

    @Override
    public String toString() {
        return "RoleBehavior{guildId=" + this.guildId + ", id=" + this.id + '}';
    }
}
