// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record UserBehaviorImpl(Dukecord duke, Snowflake id) implements UserBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof UserBehaviorImpl that && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "UserBehavior{id=" + this.id + '}';
    }
}
