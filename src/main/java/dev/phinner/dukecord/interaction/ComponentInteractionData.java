// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableComponentInteractionData.class)
public interface ComponentInteractionData {

    String customId();

    int componentType();

    /** The picked values of a select menu, IDs for entity selects. */
    List<String> values();

    @Value.Default
    default Resolved resolved() {
        return Resolved.empty();
    }
}
