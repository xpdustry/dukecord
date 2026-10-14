// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import dev.phinner.dukecord.DiscordJson;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.channel.ChannelCreateEvent;
import dev.phinner.dukecord.channel.ChannelDeleteEvent;
import dev.phinner.dukecord.channel.ChannelUpdateEvent;
import dev.phinner.dukecord.guild.GuildCreatePayload;
import dev.phinner.dukecord.guild.GuildDeletePayload;
import dev.phinner.dukecord.guild.GuildUpdateEvent;
import dev.phinner.dukecord.message.MessageCreateEvent;
import dev.phinner.dukecord.message.MessageDeleteBulkEvent;
import dev.phinner.dukecord.message.MessageDeleteEvent;
import dev.phinner.dukecord.user.UserUpdateEvent;
import java.util.Objects;

// TODO Unslop this
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
