// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** One to three texts with a button or thumbnail beside them. */
@Value.Immutable
@JsonDeserialize(as = ImmutableSection.class)
public non-sealed interface Section extends Component {

    static Builder builder() {
        return ImmutableSection.builder();
    }

    static Section of(final Component accessory, final TextDisplay... texts) {
        return ImmutableSection.of(List.of(texts), accessory);
    }

    @Override
    @Value.Derived
    default int type() {
        return 9;
    }

    @Value.Parameter
    List<TextDisplay> components();

    /** A {@link Button} or a {@link Thumbnail}. */
    @Value.Parameter
    Component accessory();

    interface Builder {
        Builder from(final Section instance);

        Builder id(final int id);

        Builder addComponents(final TextDisplay element);

        Builder addComponents(final TextDisplay... elements);

        Builder components(final Iterable<? extends TextDisplay> elements);

        Builder addAllComponents(final Iterable<? extends TextDisplay> elements);

        Builder accessory(final Component accessory);

        Section build();
    }
}
