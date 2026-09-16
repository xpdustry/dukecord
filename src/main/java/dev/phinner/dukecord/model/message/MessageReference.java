// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;

/** What a reply or forward points at. {@code channelId} is always present when received, optional when sending. */
public record MessageReference(
        Type type, Optional<Snowflake> messageId, Optional<Snowflake> channelId, Optional<Snowflake> guildId) {

    public enum Type {
        DEFAULT,
        FORWARD
    }

    public static MessageReference fromJson(final Json json) {
        return new MessageReference(
                json.optionalInteger("type").orElse(0) == 1 ? Type.FORWARD : Type.DEFAULT,
                json.optionalSnowflake("message_id"),
                json.optionalSnowflake("channel_id"),
                json.optionalSnowflake("guild_id"));
    }
}
