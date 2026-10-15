// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;

record InteractionResponseImpl(Dukecord duke, Snowflake applicationId, String token) implements InteractionResponse {

    @Override
    public boolean equals(final Object other) {
        return other instanceof InteractionResponseImpl that && this.token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return this.token.hashCode();
    }

    @Override
    public String toString() {
        return "InteractionResponse{applicationId=" + this.applicationId + '}';
    }
}
