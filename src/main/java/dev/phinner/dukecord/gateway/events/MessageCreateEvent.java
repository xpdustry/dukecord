// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.Message;

public record MessageCreateEvent(Message message) implements MessageEvent {

    @Override
    public Snowflake channelId() {
        return this.message.channel().id();
    }
}
