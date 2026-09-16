// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.cache.DiscordCache;
import dev.phinner.dukecord.gateway.DiscordEventSource;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.rest.DiscordRest;
import java.time.Duration;
import java.util.Optional;

/**
 * The client. Nouns read the cache and never do I/O; verbs on {@link #rest()} and on entities block.
 *
 * <p>The blocking {@code login(token, options)} factory arrives with the typed gateway (phase 3 of
 * {@code docs/entity-model.md}).
 */
public interface Dukecord extends AutoCloseable {

    /** The bot user, from READY and USER_UPDATE. */
    User self();

    /** The configured intents, so a bot can tell "empty" from "hidden" for content-gated fields. */
    GatewayIntentSet intents();

    DiscordCache cache();

    DiscordRest rest();

    DiscordEventSource events();

    default Optional<Guild> guild(final Snowflake id) {
        return this.cache().guild(id);
    }

    /**
     * @throws IllegalStateException if the guild is unknown or unavailable, saying which
     */
    default Guild requireGuild(final Snowflake id) {
        final var guild = this.cache()
                .guild(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Guild " + id + " is not in the cache. Is the bot a member of it, and has login() completed?"));
        if (!guild.isAvailable()) {
            throw new IllegalStateException("Guild " + id + " is currently unavailable (Discord outage)");
        }
        return guild;
    }

    /**
     * The one guild a single-guild bot lives in.
     *
     * @throws IllegalStateException if the bot is in zero or more than one guild
     */
    default Guild requireSingleGuild() {
        final var guilds = this.cache().guilds();
        if (guilds.size() != 1) {
            throw new IllegalStateException("Expected exactly one guild but the bot is in " + guilds.size());
        }
        return this.requireGuild(guilds.getFirst().id());
    }

    /** Closes with the configured shutdown timeout. */
    @Override
    void close();

    /** Closes, waiting up to {@code timeout} for in-flight handlers before interrupting them. */
    void close(final Duration timeout);
}
