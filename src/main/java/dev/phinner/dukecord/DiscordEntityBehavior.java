// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.immutables.value.Value;

/**
 * The IDs a Discord object's routes need, plus the actions on it. Entities extend their behavior, so actions work the
 * same on a snapshot and on a bare behavior decoded from an ID. {@code fetch} follows {@link DiscordEntityRepository};
 * behaviors of cached kinds also declare {@code get}.
 */
public interface DiscordEntityBehavior<T extends DiscordEntity> {

    Snowflake id();

    /** The client this behavior was decoded by. */
    @JsonIgnore
    @JacksonInject(useInput = OptBoolean.FALSE)
    @Value.Auxiliary
    Dukecord duke();

    T fetch();

    T fetch(final boolean force);
}
