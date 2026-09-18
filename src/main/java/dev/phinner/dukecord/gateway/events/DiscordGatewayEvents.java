// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.gateway.DiscordDispatchDecoder;
import dev.phinner.dukecord.gateway.DiscordEvent;
import dev.phinner.dukecord.gateway.DiscordEventDecodingException;
import dev.phinner.dukecord.gateway.UnknownDiscordEvent;
import dev.phinner.dukecord.model.DiscordJson;
import java.util.Objects;

public final class DiscordGatewayEvents {

    public static DiscordDispatchDecoder decoder(final Dukecord duke) {
        final var context = Objects.requireNonNull(duke);
        return (type, payload) -> decode(context, type, payload);
    }

    private static DiscordEvent decode(final Dukecord duke, final String type, final String payload) {
        try {
            return switch (type) {
                case "GUILD_CREATE" -> DiscordJson.read(duke, payload, GuildCreatePayload.class);
                case "GUILD_UPDATE" -> DiscordJson.read(duke, payload, GuildUpdateEvent.class);
                case "GUILD_DELETE" -> DiscordJson.read(duke, payload, GuildDeletePayload.class);
                case "CHANNEL_CREATE" -> DiscordJson.read(duke, payload, ChannelCreateEvent.class);
                case "CHANNEL_UPDATE" -> DiscordJson.read(duke, payload, ChannelUpdateEvent.class);
                case "CHANNEL_DELETE" -> DiscordJson.read(duke, payload, ChannelDeleteEvent.class);
                case "MESSAGE_CREATE" -> DiscordJson.read(duke, payload, MessageCreateEvent.class);
                case "MESSAGE_DELETE" -> DiscordJson.read(duke, payload, MessageDeleteEvent.class);
                case "MESSAGE_DELETE_BULK" -> DiscordJson.read(duke, payload, MessageDeleteBulkEvent.class);
                case "USER_UPDATE" -> DiscordJson.read(duke, payload, UserUpdateEvent.class);
                default -> new UnknownDiscordEvent(type, payload);
            };
        } catch (final RuntimeException e) {
            throw new DiscordEventDecodingException(type, e);
        }
    }

    private DiscordGatewayEvents() {}
}
