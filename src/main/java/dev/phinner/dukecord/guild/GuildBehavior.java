// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import java.util.Optional;

public interface GuildBehavior extends DiscordEntityBehavior<Guild> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static GuildBehavior of(@JacksonInject final Dukecord duke, final Snowflake id) {
        return new GuildBehaviorImpl(duke, id);
    }

    default Optional<Guild> get() {
        return this.duke().guilds().getById(this.id());
    }

    @Override
    default Guild fetch() {
        return this.duke().guilds().fetchById(this.id());
    }

    @Override
    default Guild fetch(final boolean force) {
        return this.duke().guilds().fetchById(this.id(), force);
    }
}
