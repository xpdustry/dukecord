// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.DiscordEntityRepository;

public interface ChannelRepository
        extends DiscordEntityRepository.WithIdLookup<Channel>, DiscordEntityRepository.WithGuildLookup<Channel> {

    ChannelRestOperations rest();
}
