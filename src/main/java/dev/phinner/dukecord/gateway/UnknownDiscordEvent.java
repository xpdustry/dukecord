// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import com.grack.nanojson.JsonObject;

// TODO "payload" should be immutable
public record UnknownDiscordEvent(String type, JsonObject payload) implements DiscordEvent {}
