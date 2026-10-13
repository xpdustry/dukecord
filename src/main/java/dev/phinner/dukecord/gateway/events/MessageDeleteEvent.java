// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableMessageDeleteEvent.class)
public interface MessageDeleteEvent extends MessageEvent {

    @JsonProperty("id")
    Snowflake messageId();

    Optional<Snowflake> guildId();
}
