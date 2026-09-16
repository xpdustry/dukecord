// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.Spec;
import dev.phinner.dukecord.model.permission.PermissionOverwrite;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A guild channel creation ({@code POST /guilds/{id}/channels}). */
public final class ChannelCreateSpec implements Spec {

    private final Json payload;
    private final Optional<String> reason;

    private ChannelCreateSpec(final Json payload, final Optional<String> reason) {
        this.payload = payload;
        this.reason = reason;
    }

    public static Builder text(final String name) {
        return new Builder(name, ChannelType.GUILD_TEXT);
    }

    public static Builder category(final String name) {
        return new Builder(name, ChannelType.GUILD_CATEGORY);
    }

    public static Builder of(final String name, final ChannelType type) {
        return new Builder(name, type);
    }

    public ChannelType type() {
        return ChannelType.fromCode(this.payload.integer("type"));
    }

    @Override
    public Json payload() {
        return this.payload;
    }

    @Override
    public Optional<String> reason() {
        return this.reason;
    }

    @Override
    public String toString() {
        return "ChannelCreateSpec" + this.payload;
    }

    public static final class Builder {

        private Json payload;
        private Optional<String> reason = Optional.empty();

        private Builder(final String name, final ChannelType type) {
            this.payload = Json.empty()
                    .with("name", Objects.requireNonNull(name, "name"))
                    .with("type", type.code());
        }

        public Builder topic(final String topic) {
            this.payload = this.payload.with("topic", topic);
            return this;
        }

        public Builder position(final int position) {
            this.payload = this.payload.with("position", position);
            return this;
        }

        public Builder category(final Snowflake categoryId) {
            this.payload = this.payload.with("parent_id", categoryId.asString());
            return this;
        }

        public Builder nsfw(final boolean nsfw) {
            this.payload = this.payload.with("nsfw", nsfw);
            return this;
        }

        public Builder rateLimitPerUser(final Duration delay) {
            this.payload = this.payload.with("rate_limit_per_user", (int) delay.toSeconds());
            return this;
        }

        public Builder overwrites(final Collection<PermissionOverwrite> overwrites) {
            this.payload = this.payload.with(
                    "permission_overwrites",
                    overwrites.stream().map(PermissionOverwrite::toJson).toList());
            return this;
        }

        public Builder reason(final @Nullable String reason) {
            this.reason = Optional.ofNullable(reason);
            return this;
        }

        public ChannelCreateSpec build() {
            return new ChannelCreateSpec(this.payload, this.reason);
        }
    }
}
