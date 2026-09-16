// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;

public sealed interface GuildChannel extends Channel permits CategoryChannel, GuildTextChannel, ThreadChannel {

    Snowflake guildId();

    @Value.Default
    default String name() {
        return "";
    }

    @Value.Default
    default int position() {
        return 0;
    }

    Optional<Snowflake> parentId();

    @Value.Default
    default int flags() {
        return 0;
    }
}
