// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.channel.Channel;

public interface ChannelStore
        extends DiscordEntityStore.WithIdLookup<Channel>, DiscordEntityStore.WithGuildLookup<Channel> {}
