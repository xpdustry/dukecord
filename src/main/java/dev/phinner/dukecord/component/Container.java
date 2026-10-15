// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.awt.Color;
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
        return ImmutableContainer.of(List.of(components));
    }

    @Override
    @Value.Derived
    default int type() {
        return 17;
    }

    @Value.Parameter
    List<Component> components();

    Optional<Color> accentColor();

    @Value.Default
    default boolean spoiler() {
        return false;
    }

    @Override
    default Container withDisabled(final boolean disabled) {
        return Container.builder()
                .from(this)
                .components(this.components().stream()
                        .map(c -> c.withDisabled(disabled))
                        .toList())
                .build();
    }

    interface Builder {
        Builder from(final Container instance);

        Builder id(final int id);

        Builder addComponents(final Component element);

        Builder addComponents(final Component... elements);

        Builder components(final Iterable<? extends Component> elements);

        Builder addAllComponents(final Iterable<? extends Component> elements);

        Builder accentColor(final Color accentColor);

        Builder spoiler(final boolean spoiler);

        Container build();
    }
}
