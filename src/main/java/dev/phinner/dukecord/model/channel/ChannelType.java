// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

public enum ChannelType {
    GUILD_TEXT(0),
    DM(1),
    GUILD_VOICE(2),
    GROUP_DM(3),
    GUILD_CATEGORY(4),
    GUILD_ANNOUNCEMENT(5),
    ANNOUNCEMENT_THREAD(10),
    PUBLIC_THREAD(11),
    PRIVATE_THREAD(12),
    GUILD_STAGE_VOICE(13),
    GUILD_DIRECTORY(14),
    GUILD_FORUM(15),
    GUILD_MEDIA(16),
    UNKNOWN(-1);

    private final int code;

    ChannelType(final int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public boolean isThread() {
        return this == ANNOUNCEMENT_THREAD || this == PUBLIC_THREAD || this == PRIVATE_THREAD;
    }

    public boolean isDm() {
        return this == DM || this == GROUP_DM;
    }

    public static ChannelType fromCode(final int code) {
        for (final var type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
