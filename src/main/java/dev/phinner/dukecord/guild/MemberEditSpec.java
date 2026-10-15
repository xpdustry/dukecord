// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Snowflake;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Changes to a member. Absent attributes are left as they are. An empty nick resets it; a timeout is lifted with
 * {@link MemberBehavior#removeTimeout()}.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMemberEditSpec.class)
public interface MemberEditSpec {

    static Builder builder() {
        return ImmutableMemberEditSpec.builder();
    }

    Optional<String> nick();

    /** Replaces every role of the member. */
    Optional<List<Snowflake>> roles();

    /** At most 28 days ahead. */
    Optional<Instant> communicationDisabledUntil();

    /** Only while the member is in a voice channel. */
    Optional<Boolean> mute();

    /** Only while the member is in a voice channel. */
    Optional<Boolean> deaf();

    /** Moves the member to another voice channel. */
    Optional<Snowflake> channelId();

    interface Builder {
        Builder from(final MemberEditSpec instance);

        Builder nick(final String nick);

        Builder roles(final List<Snowflake> roles);

        Builder communicationDisabledUntil(final Instant communicationDisabledUntil);

        Builder mute(final boolean mute);

        Builder deaf(final boolean deaf);

        Builder channelId(final Snowflake channelId);

        MemberEditSpec build();
    }
}
