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

    static Builder builder() {
        return ImmutableLabel.builder();
    }

    static Label of(final String label, final Component component) {
        return ImmutableLabel.of(label, component);
    }

    @Override
    @Value.Derived
    default int type() {
        return 18;
    }

    /** Not sent back in a submitted modal. */
    @Value.Default
    @Value.Parameter
    default String label() {
        return "";
    }

    Optional<String> description();

    @Value.Parameter
    Component component();

    @Override
    default Label withDisabled(final boolean disabled) {
        return Label.builder()
                .from(this)
                .component(this.component().withDisabled(disabled))
                .build();
    }

    interface Builder {
        Builder from(final Label instance);

        Builder id(final int id);

        Builder label(final String label);

        Builder description(final String description);

        Builder component(final Component component);

        Label build();
    }
}
