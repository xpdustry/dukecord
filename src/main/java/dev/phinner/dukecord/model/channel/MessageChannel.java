// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;

public sealed interface MessageChannel extends Channel permits DmChannel, GuildTextChannel, ThreadChannel {

    Optional<Snowflake> lastMessageId();
}
