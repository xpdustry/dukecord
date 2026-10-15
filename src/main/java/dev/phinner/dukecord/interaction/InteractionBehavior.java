// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import org.immutables.value.Value;

/** The IDs needed to answer an interaction. The token expires 15 minutes after the interaction was created. */
public interface InteractionBehavior {

    Snowflake id();

    Snowflake applicationId();

    @Value.Redacted
    String token();

    /** The client this interaction was decoded by. */
    @JsonIgnore
    @JacksonInject(useInput = OptBoolean.FALSE)
    @Value.Auxiliary
    Dukecord duke();

    /** The original response, once the interaction is acknowledged. */
    default InteractionResponse response() {
        return InteractionResponse.of(this.duke(), this.applicationId(), this.token());
    }
}
