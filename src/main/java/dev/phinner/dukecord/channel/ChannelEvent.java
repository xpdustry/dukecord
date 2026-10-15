// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;
import org.immutables.value.Value;

public interface ChannelEvent extends DiscordGatewayEvent {

    @Value.Parameter
    Channel channel();

    @Value.Derived
    default Snowflake channelId() {
        return this.channel().id();
    }
}
