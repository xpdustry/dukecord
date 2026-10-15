// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.channel.ChannelRepository;
import dev.phinner.dukecord.gateway.DiscordEventSource;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.guild.GuildRepository;
import dev.phinner.dukecord.guild.MemberRepository;
import dev.phinner.dukecord.guild.RoleRepository;
import dev.phinner.dukecord.interaction.CommandRepository;
import dev.phinner.dukecord.interaction.InteractionRepository;
import dev.phinner.dukecord.message.MessageRepository;
import dev.phinner.dukecord.user.User;
import dev.phinner.dukecord.user.UserRepository;

/** A connected Discord client. */
public interface Dukecord extends AutoCloseable {

    GatewayIntentSet intents();

    DiscordEventSource events();

    User self();

    GuildRepository guilds();

    MemberRepository members();

    RoleRepository roles();

    ChannelRepository channels();

    MessageRepository messages();

    UserRepository users();

    CommandRepository commands();

    InteractionRepository interactions();

    @Override
    void close();
}
