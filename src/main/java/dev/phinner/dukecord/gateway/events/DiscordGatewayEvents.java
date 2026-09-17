// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.gateway.DiscordDispatchDecoder;
import dev.phinner.dukecord.gateway.DiscordEvent;
import dev.phinner.dukecord.gateway.DiscordEventDecodingException;
import dev.phinner.dukecord.gateway.UnknownDiscordEvent;
import dev.phinner.dukecord.model.DiscordJson;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.user.User;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public final class DiscordGatewayEvents {

    public static DiscordDispatchDecoder decoder(final Dukecord duke) {
        final var context = Objects.requireNonNull(duke);
        return (type, payload) -> decode(context, type, payload);
    }

    private static DiscordEvent decode(final Dukecord duke, final String type, final String payload) {
        try {
            return switch (type) {
                case "GUILD_CREATE" -> decodeGuildCreate(duke, payload);
                case "GUILD_UPDATE" -> new GuildUpdateEvent(DiscordJson.read(duke, payload, Guild.class));
                case "GUILD_DELETE" -> decodeGuildDelete(payload);
                case "CHANNEL_CREATE" -> new ChannelCreateEvent(DiscordJson.read(duke, payload, Channel.class));
                case "CHANNEL_UPDATE" -> new ChannelUpdateEvent(DiscordJson.read(duke, payload, Channel.class));
                case "CHANNEL_DELETE" -> new ChannelDeleteEvent(DiscordJson.read(duke, payload, Channel.class));
                case "MESSAGE_CREATE" -> new MessageCreateEvent(DiscordJson.read(duke, payload, Message.class));
                case "MESSAGE_DELETE" -> decodeMessageDelete(payload);
                case "MESSAGE_DELETE_BULK" -> decodeMessageDeleteBulk(payload);
                case "USER_UPDATE" -> new UserUpdateEvent(DiscordJson.read(duke, payload, User.class));
                default -> new UnknownDiscordEvent(type, payload);
            };
        } catch (final RuntimeException | JsonParserException e) {
            throw new DiscordEventDecodingException(type, e);
        }
    }

    private static DiscordEvent decodeGuildCreate(final Dukecord duke, final String payload)
            throws JsonParserException {
        final var data = JsonParser.object().from(payload);
        if (data.getBoolean("unavailable")) {
            return new GuildUnavailableEvent(snowflake(data, "id"));
        }
        return new GuildCreateEvent(DiscordJson.read(duke, payload, Guild.class));
    }

    private static DiscordEvent decodeGuildDelete(final String payload) throws JsonParserException {
        final var data = JsonParser.object().from(payload);
        final var guildId = snowflake(data, "id");
        return data.getBoolean("unavailable") ? new GuildUnavailableEvent(guildId) : new GuildDeleteEvent(guildId);
    }

    private static MessageDeleteEvent decodeMessageDelete(final String payload) throws JsonParserException {
        final var data = JsonParser.object().from(payload);
        return new MessageDeleteEvent(
                snowflake(data, "id"), snowflake(data, "channel_id"), optionalSnowflake(data, "guild_id"));
    }

    private static MessageDeleteBulkEvent decodeMessageDeleteBulk(final String payload) throws JsonParserException {
        final var data = JsonParser.object().from(payload);
        final JsonArray ids = data.getArray("ids");
        final var messageIds = new ArrayList<Snowflake>(ids.size());
        for (final var id : ids) {
            if (!(id instanceof String value)) {
                throw new IllegalArgumentException("Discord field ids must contain only snowflake strings");
            }
            messageIds.add(Snowflake.of(value));
        }
        return new MessageDeleteBulkEvent(
                messageIds, snowflake(data, "channel_id"), optionalSnowflake(data, "guild_id"));
    }

    private static Snowflake snowflake(final JsonObject data, final String name) {
        final var value = data.getString(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing Discord snowflake field " + name);
        }
        return Snowflake.of(value);
    }

    private static Optional<Snowflake> optionalSnowflake(final JsonObject data, final String name) {
        return Optional.ofNullable(data.getString(name)).map(Snowflake::of);
    }

    private DiscordGatewayEvents() {}
}
