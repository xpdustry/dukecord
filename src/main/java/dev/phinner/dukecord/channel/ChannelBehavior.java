// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import java.util.Optional;

public interface ChannelBehavior extends DiscordEntityBehavior<Channel> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ChannelBehavior of(@JacksonInject final Dukecord duke, final Snowflake id) {
        return new ChannelBehaviorImpl(duke, id);
    }

    default Optional<? extends Channel> get() {
        return this.duke().channels().getById(this.id());
    }

    @Override
    default Channel fetch() {
        return this.duke().channels().fetchById(this.id());
    }

    @Override
    default Channel fetch(final boolean force) {
        return this.duke().channels().fetchById(this.id(), force);
    }

    default void delete() {
        this.duke().channels().rest().delete(this.id());
    }

    default void delete(final String reason) {
        this.duke().channels().rest().delete(this.id(), reason);
    }
}
