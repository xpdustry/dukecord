// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/** Where an interaction happened, or where a command may be used. */
public enum InteractionContext {
    @JsonEnumDefaultValue
    UNKNOWN(-1),
    GUILD(0),
    BOT_DM(1),
    /** Group DMs and DMs other than with the bot, for user-installed apps. */
    PRIVATE_CHANNEL(2);

    private final int value;

    InteractionContext(final int value) {
        this.value = value;
    }

    @JsonValue
    public int value() {
        return this.value;
    }
}
