// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

/** A gateway dispatch for which Dukecord has no typed event, or whose typed payload could not be decoded. */
public record UnknownDiscordEvent(String type, String payload) implements DiscordEvent {}
