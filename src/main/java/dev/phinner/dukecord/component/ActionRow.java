// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Up to five buttons or a single select menu. */
@Value.Immutable
@JsonDeserialize(as = ImmutableActionRow.class)
public non-sealed interface ActionRow extends Component {

    static ActionRow of(final Component... components) {
        return ImmutableActionRow.builder().addComponents(components).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 1;
    }

    List<Component> components();
}
