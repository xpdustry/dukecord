// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.user.UserBehavior;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableThreadChannel.class)
public non-sealed interface ThreadChannel extends GuildChannel, MessageChannel {

    @JsonProperty("owner_id")
    UserBehavior owner();

    @Value.Default
    default int messageCount() {
        return 0;
    }

    @Value.Default
    default int memberCount() {
        return 0;
    }

    default void join() {
        this.duke().rest().channels().joinThread(this.id());
    }

    default void leave() {
        this.duke().rest().channels().leaveThread(this.id());
    }
}
