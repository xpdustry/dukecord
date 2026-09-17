// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Optional;

public record MessageDeleteBulkEvent(List<Snowflake> messageIds, Snowflake channelId, Optional<Snowflake> guildId)
        implements MessageEvent {

    public MessageDeleteBulkEvent {
        messageIds = List.copyOf(messageIds);
    }
}
