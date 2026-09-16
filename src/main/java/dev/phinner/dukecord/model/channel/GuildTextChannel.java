// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import java.time.Duration;
import java.util.Optional;

/** A guild text or announcement channel; {@link #type()} tells which. */
public non-sealed interface GuildTextChannel extends GuildChannel, MessageChannel {

    Optional<String> topic();

    boolean isNsfw();

    /** The slowmode delay, {@link Duration#ZERO} when off. */
    Duration rateLimitPerUser();
}
