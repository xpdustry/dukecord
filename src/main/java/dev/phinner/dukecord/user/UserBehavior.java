// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.channel.DmChannel;
import java.util.Optional;

public interface UserBehavior extends DiscordEntityBehavior<User> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static UserBehavior of(@JacksonInject final Dukecord duke, final Snowflake id) {
        return new UserBehaviorImpl(duke, id);
    }

    default Optional<User> get() {
        return this.duke().users().getById(this.id());
    }

    @Override
    default User fetch() {
        return this.duke().users().fetchById(this.id());
    }

    @Override
    default User fetch(final boolean force) {
        return this.duke().users().fetchById(this.id(), force);
    }

    default DmChannel createDm() {
        return this.duke().rest().channels().createDm(this.id());
    }
}
