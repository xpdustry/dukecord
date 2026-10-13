// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableMessageDeleteBulkEvent.class)
public interface MessageDeleteBulkEvent extends MessageEvent {

    @JsonProperty("ids")
    List<Snowflake> messageIds();

    Optional<Snowflake> guildId();
}
