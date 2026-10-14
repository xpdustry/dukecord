// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record MessageBehaviorImpl(Dukecord duke, Snowflake channelId, Snowflake id) implements MessageBehavior {

    @Override
    public boolean equals(final Object other) {
        return other instanceof MessageBehaviorImpl that
                && this.channelId.equals(that.channelId)
                && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31 * this.channelId.hashCode() + this.id.hashCode();
    }

    @Override
    public String toString() {
        return "MessageBehavior{channelId=" + this.channelId + ", id=" + this.id + '}';
    }
}
