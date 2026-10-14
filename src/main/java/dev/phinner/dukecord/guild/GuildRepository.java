// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.DiscordEntityRepository;

public interface GuildRepository
        extends DiscordEntityRepository.WithIdLookup<Guild>, DiscordEntityRepository.WithGlobalLookup<Guild> {

    GuildRestOperations rest();
}
