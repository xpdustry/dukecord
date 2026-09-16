// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.message.Message;
import java.time.Duration;
import java.util.Optional;

final class GuildTextChannelImpl extends AbstractGuildChannel implements GuildTextChannel {

    private final Optional<String> topic;
    private final boolean nsfw;
    private final Duration rateLimitPerUser;
    private final Optional<DiscordEntityRef<Message>> lastMessage;

    GuildTextChannelImpl(final Dukecord duke, final Json raw) {
        super(duke, raw);
        this.topic = raw.optionalString("topic");
        this.nsfw = raw.bool("nsfw", false);
        this.rateLimitPerUser =
                Duration.ofSeconds(raw.optionalInteger("rate_limit_per_user").orElse(0));
        this.lastMessage = this.lastMessageRef();
    }

    @Override
    public Optional<String> topic() {
        return this.topic;
    }

    @Override
    public boolean isNsfw() {
        return this.nsfw;
    }

    @Override
    public Duration rateLimitPerUser() {
        return this.rateLimitPerUser;
    }

    @Override
    public Optional<DiscordEntityRef<Message>> lastMessage() {
        return this.lastMessage;
    }
}
