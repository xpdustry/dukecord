// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.model.user.User;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableUserUpdateEvent.class)
public interface UserUpdateEvent extends DiscordGatewayEvent {

    @JsonUnwrapped
    User user();
}
