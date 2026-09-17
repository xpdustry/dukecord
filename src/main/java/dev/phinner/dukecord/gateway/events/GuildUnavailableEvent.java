// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.Snowflake;

public record GuildUnavailableEvent(Snowflake guildId) implements GuildEvent {}
