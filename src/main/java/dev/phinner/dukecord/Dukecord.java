// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.gateway.DiscordEventSource;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.model.user.User;

/** A connected Discord client. */
public interface Dukecord extends AutoCloseable {

    GatewayIntentSet intents();

    DiscordEventSource events();

    User self();

    @Override
    void close();
}
