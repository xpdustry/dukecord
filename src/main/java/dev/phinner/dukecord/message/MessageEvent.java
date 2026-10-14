// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;

public interface MessageEvent extends DiscordGatewayEvent {

    Snowflake channelId();
}
