// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

/** A guild channel of a type this library does not model (voice, stage, forum, ...); use {@code raw()}. */
public non-sealed interface UnknownGuildChannel extends GuildChannel {}
