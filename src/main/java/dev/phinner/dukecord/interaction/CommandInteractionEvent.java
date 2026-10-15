// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A slash, user or message command. */
@Value.Immutable
@JsonDeserialize(as = ImmutableCommandInteractionEvent.class)
public non-sealed interface CommandInteractionEvent extends InteractionEvent, ReplyCallback, ModalCallback {

    CommandInteractionData data();
}
