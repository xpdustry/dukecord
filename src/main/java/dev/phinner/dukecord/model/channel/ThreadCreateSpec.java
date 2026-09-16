// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.Spec;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A thread creation. Without a starter {@link #message()} it targets text channels (standalone) or the
 * message-anchored route; with one it targets forum and media channels, whose consumer nests the message body.
 */
public final class ThreadCreateSpec implements Spec {

    private final Json payload;
    private final Optional<MessageCreateSpec> message;
    private final Optional<String> reason;

    private ThreadCreateSpec(
            final Json payload, final Optional<MessageCreateSpec> message, final Optional<String> reason) {
        this.payload = payload;
        this.message = message;
        this.reason = reason;
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    @Override
    public Json payload() {
        return this.payload;
    }

    /** The starter message for forum and media channels. */
    public Optional<MessageCreateSpec> message() {
        return this.message;
    }

    @Override
    public Optional<String> reason() {
        return this.reason;
    }

    @Override
    public String toString() {
        return "ThreadCreateSpec" + this.payload;
    }

    public static final class Builder {

        private Json payload;
        private Optional<MessageCreateSpec> message = Optional.empty();
        private Optional<String> reason = Optional.empty();

        private Builder(final String name) {
            this.payload = Json.empty().with("name", Objects.requireNonNull(name, "name"));
        }

        public Builder autoArchiveDuration(final Duration duration) {
            this.payload = this.payload.with("auto_archive_duration", (int) duration.toMinutes());
            return this;
        }

        /** Standalone threads only: a private thread instead of a public one. */
        public Builder privateThread(final boolean isPrivate) {
            this.payload = this.payload.with(
                    "type", isPrivate ? ChannelType.PRIVATE_THREAD.code() : ChannelType.PUBLIC_THREAD.code());
            return this;
        }

        /** Private threads only: whether non-moderators can invite others. */
        public Builder invitable(final boolean invitable) {
            this.payload = this.payload.with("invitable", invitable);
            return this;
        }

        public Builder rateLimitPerUser(final Duration delay) {
            this.payload = this.payload.with("rate_limit_per_user", (int) delay.toSeconds());
            return this;
        }

        /** Forum and media channels only. */
        public Builder appliedTags(final Collection<Snowflake> tagIds) {
            this.payload = this.payload.with(
                    "applied_tags", tagIds.stream().map(Snowflake::asString).toList());
            return this;
        }

        /** Forum and media channels only: the starter message. */
        public Builder message(final MessageCreateSpec message) {
            this.message = Optional.of(message);
            return this;
        }

        public Builder reason(final @Nullable String reason) {
            this.reason = Optional.ofNullable(reason);
            return this;
        }

        public ThreadCreateSpec build() {
            return new ThreadCreateSpec(this.payload, this.message, this.reason);
        }
    }
}
