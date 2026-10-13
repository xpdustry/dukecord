// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import org.immutables.value.Value;

public interface ChannelEvent extends DiscordGatewayEvent {

    Channel channel();

    @Value.Derived
    default Snowflake channelId() {
        return this.channel().id();
    }
}
