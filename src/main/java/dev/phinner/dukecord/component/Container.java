// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A box around other components, like an embed. */
@Value.Immutable
@JsonDeserialize(as = ImmutableContainer.class)
public non-sealed interface Container extends Component {

    static Builder builder() {
        return ImmutableContainer.builder();
    }

    static Container of(final Component... components) {
        return ImmutableContainer.builder().addComponents(components).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 17;
    }

    List<Component> components();

    Optional<Integer> accentColor();

    @Value.Default
    default boolean spoiler() {
        return false;
    }

    interface Builder {
        Builder from(Container instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder addComponents(Component element);

        Builder addComponents(Component... elements);

        Builder components(Iterable<? extends Component> elements);

        Builder addAllComponents(Iterable<? extends Component> elements);

        Builder accentColor(int accentColor);

        Builder accentColor(Optional<Integer> accentColor);

        Builder spoiler(boolean spoiler);

        Container build();
    }
}
