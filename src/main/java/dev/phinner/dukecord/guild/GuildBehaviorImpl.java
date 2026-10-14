// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record GuildBehaviorImpl(Dukecord duke, Snowflake id) implements GuildBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof GuildBehaviorImpl that && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "GuildBehavior{id=" + this.id + '}';
    }
}
