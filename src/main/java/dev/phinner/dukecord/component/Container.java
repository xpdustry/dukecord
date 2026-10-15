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
}
