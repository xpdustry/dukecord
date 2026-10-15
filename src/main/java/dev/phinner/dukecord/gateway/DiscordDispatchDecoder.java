// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

@FunctionalInterface
public interface DiscordDispatchDecoder {

    DiscordEvent decode(final String type, final String payload);

    static DiscordDispatchDecoder unknownOnly() {
        return UnknownDiscordEvent::new;
    }
}
