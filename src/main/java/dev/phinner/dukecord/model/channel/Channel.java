// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.DiscordEntity;

/** The root of the channel hierarchy. Unknown types parse as {@link UnknownChannel}; nothing throws on them. */
public sealed interface Channel extends DiscordEntity permits GuildChannel, MessageChannel, UnknownChannel {

    ChannelType type();

    /** The raw {@code type} code, meaningful when {@link #type()} is {@link ChannelType#UNKNOWN}. */
    int typeCode();
}
