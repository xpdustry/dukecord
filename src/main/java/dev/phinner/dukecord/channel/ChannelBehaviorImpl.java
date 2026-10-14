// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record ChannelBehaviorImpl(Dukecord duke, Snowflake id) implements ChannelBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof ChannelBehaviorImpl that && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ChannelBehavior{id=" + this.id + '}';
    }
}
