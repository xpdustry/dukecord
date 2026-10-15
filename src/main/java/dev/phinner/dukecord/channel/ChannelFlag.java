// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.BitFlag;

/** A flag of a channel. */
public enum ChannelFlag implements BitFlag {
    /** Bits Dukecord does not know. */
    UNKNOWN(-1),
    /** A thread pinned to the top of its forum or media channel. */
    PINNED(1),
    /** Threads created in this forum or media channel need a tag. */
    REQUIRE_TAG(4),
    /** Hides the download options of embedded media. Only for media channels. */
    HIDE_MEDIA_DOWNLOAD_OPTIONS(15),
    /** The bot cannot view this channel, so its details are hidden. */
    CHANNEL_OBFUSCATED(17),
    /** Users must opt in to view its contents. */
    IS_SPOILER_CHANNEL(21);

    private final long mask;

    ChannelFlag(final int bit) {
        this.mask = bit < 0 ? 0 : 1L << bit;
    }

    @Override
    public long mask() {
        return this.mask;
    }
}
