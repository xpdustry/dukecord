// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.message.Message;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A button press or a select menu choice. */
@Value.Immutable
@JsonDeserialize(as = ImmutableComponentInteractionEvent.class)
public non-sealed interface ComponentInteractionEvent
        extends InteractionEvent, ReplyCallback, ModalCallback, UpdateCallback {

    /** The message holding the component. */
    Message message();

    ComponentInteractionData data();

    default String customId() {
        return this.data().customId();
    }
}
