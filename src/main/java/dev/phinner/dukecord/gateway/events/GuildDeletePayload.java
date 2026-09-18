// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "unavailable",
        defaultImpl = ImmutableGuildDeleteEvent.class)
@JsonSubTypes(@JsonSubTypes.Type(value = ImmutableGuildUnavailableEvent.class, name = "true"))
interface GuildDeletePayload extends DiscordGatewayEvent {}
