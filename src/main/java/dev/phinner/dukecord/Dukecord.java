// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.gateway.DiscordEventSource;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.store.ChannelStore;
import dev.phinner.dukecord.store.GuildStore;
import dev.phinner.dukecord.store.MessageStore;
import dev.phinner.dukecord.store.UserStore;

/** A connected Discord client. */
public interface Dukecord extends AutoCloseable {

    GatewayIntentSet intents();

    DiscordEventSource events();

    User self();

    GuildStore guilds();

    ChannelStore channels();

    MessageStore messages();

    UserStore users();

    @Override
    void close();
}
