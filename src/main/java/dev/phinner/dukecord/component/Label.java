// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A titled wrapper around one input in a modal. */
@Value.Immutable
@JsonDeserialize(as = ImmutableLabel.class)
public non-sealed interface Label extends Component {

    static Label of(final String label, final Component component) {
        return ImmutableLabel.builder().label(label).component(component).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 18;
    }

    /** Not sent back in a submitted modal. */
    @Value.Default
    default String label() {
        return "";
    }

    Optional<String> description();

    Component component();
}
