// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

@FunctionalInterface
public interface DiscordDispatchDecoder {

    DiscordEvent decode(String type, String payload);

    static DiscordDispatchDecoder unknownOnly() {
        return UnknownDiscordEvent::new;
    }
}
