// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import java.util.Optional;
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
        return ImmutableActionRow.builder().addComponents(components).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 1;
    }

    List<Component> components();

    interface Builder {
        Builder from(ActionRow instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder addComponents(Component element);

        Builder addComponents(Component... elements);

        Builder components(Iterable<? extends Component> elements);

        Builder addAllComponents(Iterable<? extends Component> elements);

        ActionRow build();
    }
}
