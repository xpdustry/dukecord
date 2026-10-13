// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.Message;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableMessageCreateEvent.class)
public interface MessageCreateEvent extends MessageEvent {

    @JsonUnwrapped
    Message message();

    @Override
    @Value.Derived
    default Snowflake channelId() {
        return this.message().channel().id();
    }
}
