// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.guild.Guild;

public interface GuildStore
        extends DiscordEntityStore.WithIdLookup<Guild>, DiscordEntityStore.WithGlobalLookup<Guild> {}
