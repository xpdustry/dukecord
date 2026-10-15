// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.component.Component;
import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** A popup form. Inputs go in {@link dev.phinner.dukecord.component.Label}s. */
@Value.Immutable
@JsonSerialize(as = ImmutableModalSpec.class)
public interface ModalSpec {

    static Builder builder() {
        return ImmutableModalSpec.builder();
    }

    static ModalSpec of(final String customId, final String title, final Component... components) {
        return ImmutableModalSpec.of(customId, title, List.of(components));
    }

    @Value.Parameter
    String customId();

    @Value.Parameter
    String title();

    @Value.Parameter
    List<Component> components();

    interface Builder {
        Builder from(final ModalSpec instance);

        Builder customId(final String customId);

        Builder title(final String title);

        Builder addComponents(final Component element);

        Builder addComponents(final Component... elements);

        Builder components(final Iterable<? extends Component> elements);

        Builder addAllComponents(final Iterable<? extends Component> elements);

        ModalSpec build();
    }
}
