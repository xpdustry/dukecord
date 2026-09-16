// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.Json;

/** Builds {@link Message} snapshots from gateway or REST message objects. */
public final class Messages {

    /**
     * Requires {@code id}, {@code channel_id}, {@code author} and {@code timestamp}. The inline {@code member} is
     * merged with {@code author} when {@code guild_id} is present.
     */
    public static Message fromJson(final Dukecord duke, final Json json) {
        return new MessageImpl(duke, json);
    }

    private Messages() {}
}
