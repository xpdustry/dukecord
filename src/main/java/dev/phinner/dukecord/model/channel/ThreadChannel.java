// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.guild.Member;

public non-sealed interface ThreadChannel extends GuildChannel, MessageChannel {

    /** The text, announcement or forum channel this thread lives in. */
    DiscordEntityRef<GuildChannel> parent();

    DiscordEntityRef<Member> owner();

    ThreadMetadata metadata();

    /** Approximate on the gateway; a hint, not a count. */
    int messageCount();

    /** Capped at 50 by Discord; a hint, not a count. */
    int memberCount();

    /** Whether the bot is a member of this thread, tracked by the cache from thread member events. */
    boolean isJoined();

    default void join() {
        this.duke().rest().joinThread(this.id());
    }

    default void leave() {
        this.duke().rest().leaveThread(this.id());
    }
}
