// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A component type Dukecord does not know yet. Only its type and ID are kept, so it cannot be sent back. */
@Value.Immutable
@JsonDeserialize(as = ImmutableUnknownComponent.class)
public non-sealed interface UnknownComponent extends Component {}
