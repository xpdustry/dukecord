// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.JsonException;
import dev.phinner.dukecord.model.Snowflake;

/** Builds {@link Channel} snapshots. Never throws on an unknown {@code type}; unknown types become {@code Unknown*}. */
public final class Channels {

    /**
     * Parses any channel object. Guild-bound types require {@code guild_id}: children of {@code GUILD_CREATE} lack it,
     * so the cache backfills it before calling this. For threads, joined is derived from the presence of the
     * {@code member} field, which REST and {@code GUILD_CREATE} set when the bot is a member.
     *
     * @throws JsonException if a guild-bound type has no {@code guild_id}
     */
    public static Channel fromJson(final Dukecord duke, final Json json) {
        return fromJson(duke, json, json.isPresent("member"));
    }

    /** Parses a channel object, forcing the thread joined flag; ignored for non-threads. */
    public static Channel fromJson(final Dukecord duke, final Json json, final boolean joined) {
        final var type = ChannelType.fromCode(json.integer("type"));
        if (type.isDm()) {
            return new DmChannelImpl(duke, json);
        }
        final boolean guildBound = json.isPresent("guild_id");
        if (type == ChannelType.UNKNOWN) {
            return guildBound ? new UnknownGuildChannelImpl(duke, json) : new UnknownChannelImpl(duke, json);
        }
        if (!guildBound) {
            throw new JsonException("Channel " + json.raw("id") + " of type " + type + " has no guild_id");
        }
        return switch (type) {
            case GUILD_TEXT, GUILD_ANNOUNCEMENT -> new GuildTextChannelImpl(duke, json);
            case GUILD_CATEGORY -> new CategoryChannelImpl(duke, json);
            case ANNOUNCEMENT_THREAD, PUBLIC_THREAD, PRIVATE_THREAD -> new ThreadChannelImpl(duke, json, joined);
            default -> new UnknownGuildChannelImpl(duke, json);
        };
    }

    /**
     * A typed channel reference. {@code fetch()} throws {@link IllegalStateException} if the channel exists but is not
     * of the requested type.
     */
    public static <C extends Channel> DiscordEntityRef<C> ref(
            final Dukecord duke, final Class<C> type, final Snowflake channelId) {
        return DiscordEntityRef.of(type, channelId, id -> duke.cache().channel(id, type), id -> {
            final var channel = duke.rest().fetchChannel(id);
            if (!type.isInstance(channel)) {
                throw new IllegalStateException(
                        "Channel " + id + " is a " + channel.type() + ", not a " + type.getSimpleName());
            }
            return type.cast(channel);
        });
    }

    private Channels() {}
}
