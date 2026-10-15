// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record MemberBehaviorImpl(Dukecord duke, Snowflake guildId, Snowflake id) implements MemberBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof MemberBehaviorImpl that && this.guildId.equals(that.guildId) && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31 * this.guildId.hashCode() + this.id.hashCode();
    }

    @Override
    public String toString() {
        return "MemberBehavior{guildId=" + this.guildId + ", id=" + this.id + '}';
    }
}
