// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableUserUpdateEvent.class)
public interface UserUpdateEvent extends DiscordGatewayEvent {

    @JsonUnwrapped
    User user();
}
