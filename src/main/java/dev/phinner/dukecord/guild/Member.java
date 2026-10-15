// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import dev.phinner.dukecord.DiscordEntity;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A user in a guild. Its ID is the user's. */
// A MemberBehavior has no full user to copy from.
@SuppressWarnings("immutables:from")
@Value.Immutable
@JsonDeserialize(as = ImmutableMember.class)
public interface Member extends DiscordEntity, MemberBehavior {

    /** Discord's member objects leave the guild out; it comes from the surrounding payload. */
    @Override
    @JsonIgnore
    @JacksonInject(value = "guild_id", useInput = OptBoolean.FALSE)
    Snowflake guildId();

    @Override
    User user();

    @Override
    @Value.Derived
    default Snowflake id() {
        return this.user().id();
    }

    /** The guild nickname. */
    Optional<String> nick();

    /** The guild avatar hash. */
    Optional<String> avatar();

    /** Without the {@code @everyone} role. */
    List<RoleBehavior> roles();

    /** Absent for guest members of voice channels. */
    Optional<Instant> joinedAt();

    /** When the member started boosting the guild. */
    Optional<Instant> premiumSince();

    /** When the timeout ends. May be in the past. */
    Optional<Instant> communicationDisabledUntil();

    @Value.Default
    default boolean deaf() {
        return false;
    }

    @Value.Default
    default boolean mute() {
        return false;
    }

    /** Whether the member has yet to pass membership screening. */
    @Value.Default
    default boolean pending() {
        return false;
    }

    @Value.Default
    default int flags() {
        return 0;
    }

    /** Only on members received with an interaction, counting channel overwrites. */
    Optional<PermissionSet> permissions();

    default String displayName() {
        return this.nick().or(() -> this.user().globalName()).orElseGet(() -> this.user()
                .username());
    }
}
