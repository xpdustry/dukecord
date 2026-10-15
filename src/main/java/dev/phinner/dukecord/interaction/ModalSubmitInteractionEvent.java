// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.message.Message;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableModalSubmitInteractionEvent.class)
public non-sealed interface ModalSubmitInteractionEvent extends InteractionEvent, ReplyCallback, UpdateCallback {

    /** Present when the modal was opened from a component, which also allows {@link #update}. */
    Optional<Message> message();

    ModalSubmitInteractionData data();

    default String customId() {
        return this.data().customId();
    }
}
