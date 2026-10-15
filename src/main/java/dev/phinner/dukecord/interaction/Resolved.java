// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.guild.Role;
import dev.phinner.dukecord.message.Attachment;
import dev.phinner.dukecord.user.User;
import java.util.Map;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * The objects behind the IDs in options and select values. Discord also sends partial members, channels and messages,
 * which are left out; use their IDs instead.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableResolved.class)
public interface Resolved {

    static Resolved empty() {
        return ImmutableResolved.builder().build();
    }

    Map<Snowflake, User> users();

    Map<Snowflake, Role> roles();

    Map<Snowflake, Attachment> attachments();
}
