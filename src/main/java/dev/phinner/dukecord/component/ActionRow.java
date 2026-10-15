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

    static Builder builder() {
        return ImmutableActionRow.builder();
    }

    static ActionRow of(final Component... components) {
        return ImmutableActionRow.of(List.of(components));
    }

    @Override
    @Value.Derived
    default int type() {
        return 1;
    }

    @Value.Parameter
    List<Component> components();

    interface Builder {
        Builder from(final ActionRow instance);

        Builder id(final int id);

        Builder addComponents(final Component element);

        Builder addComponents(final Component... elements);

        Builder components(final Iterable<? extends Component> elements);

        Builder addAllComponents(final Iterable<? extends Component> elements);

        ActionRow build();
    }
}
