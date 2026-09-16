// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.Spec;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A member edit ({@code PATCH /guilds/{id}/members/{id}}). Keys never set are absent and left unchanged. */
public final class MemberEditSpec implements Spec {

    private final Json payload;
    private final Optional<String> reason;

    private MemberEditSpec(final Json payload, final Optional<String> reason) {
        this.payload = payload;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
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
        return "MemberEditSpec" + this.payload;
    }

    public static final class Builder {

        private Json payload = Json.empty();
        private Optional<String> reason = Optional.empty();

        private Builder() {}

        public Builder nickname(final String nickname) {
            this.payload = this.payload.with("nick", Objects.requireNonNull(nickname, "nickname"));
            return this;
        }

        public Builder clearNickname() {
            this.payload = this.payload.with("nick", null);
            return this;
        }

        /** Replaces the whole role set in one request. */
        public Builder roles(final Collection<Snowflake> roleIds) {
            this.payload = this.payload.with(
                    "roles", roleIds.stream().map(Snowflake::asString).toList());
            return this;
        }

        public Builder timeoutUntil(final Instant until) {
            this.payload = this.payload.with("communication_disabled_until", until.toString());
            return this;
        }

        public Builder clearTimeout() {
            this.payload = this.payload.with("communication_disabled_until", null);
            return this;
        }

        public Builder mute(final boolean mute) {
            this.payload = this.payload.with("mute", mute);
            return this;
        }

        public Builder deaf(final boolean deaf) {
            this.payload = this.payload.with("deaf", deaf);
            return this;
        }

        /** Moves the member to a voice channel they are connected to; {@link #disconnectVoice()} kicks them out. */
        public Builder voiceChannel(final Snowflake channelId) {
            this.payload = this.payload.with("channel_id", channelId.asString());
            return this;
        }

        public Builder disconnectVoice() {
            this.payload = this.payload.with("channel_id", null);
            return this;
        }

        public Builder reason(final @Nullable String reason) {
            this.reason = Optional.ofNullable(reason);
            return this;
        }

        public MemberEditSpec build() {
            return new MemberEditSpec(this.payload, this.reason);
        }
    }
}
