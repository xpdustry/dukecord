// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.BitFlag;

/** A flag of an attachment. */
public enum AttachmentFlag implements BitFlag {
    /** Bits Dukecord does not know. */
    UNKNOWN(-1),
    /** A clip from a stream. */
    IS_CLIP(0),
    /** The thumbnail of a thread in a media channel. */
    IS_THUMBNAIL(1),
    /** Edited with the remix feature on mobile. */
    IS_REMIX(2),
    /** Blurred until clicked. */
    IS_SPOILER(3),
    /** An animated image. */
    IS_ANIMATED(5);

    private final long mask;

    AttachmentFlag(final int bit) {
        this.mask = bit < 0 ? 0 : 1L << bit;
    }

    @Override
    public long mask() {
        return this.mask;
    }
}
