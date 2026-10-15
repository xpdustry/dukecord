// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** An interaction type Dukecord does not know yet. */
@Value.Immutable
@JsonDeserialize(as = ImmutableUnknownInteractionEvent.class)
public non-sealed interface UnknownInteractionEvent extends InteractionEvent {}
