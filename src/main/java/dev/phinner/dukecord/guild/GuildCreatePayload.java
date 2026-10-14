// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "unavailable",
        defaultImpl = ImmutableGuildCreateEvent.class)
@JsonSubTypes(@JsonSubTypes.Type(value = ImmutableGuildUnavailableEvent.class, name = "true"))
public interface GuildCreatePayload extends DiscordGatewayEvent {}
