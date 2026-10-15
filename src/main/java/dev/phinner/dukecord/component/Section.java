// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import java.util.Optional;
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
        return ImmutableSection.builder()
                .accessory(accessory)
                .addComponents(texts)
                .build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 9;
    }

    List<TextDisplay> components();

    /** A {@link Button} or a {@link Thumbnail}. */
    Component accessory();

    interface Builder {
        Builder from(Section instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder addComponents(TextDisplay element);

        Builder addComponents(TextDisplay... elements);

        Builder components(Iterable<? extends TextDisplay> elements);

        Builder addAllComponents(Iterable<? extends TextDisplay> elements);

        Builder accessory(Component accessory);

        Section build();
    }
}
