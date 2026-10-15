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

    static ModalSpec of(final String customId, final String title, final Component... components) {
        return ImmutableModalSpec.builder()
                .customId(customId)
                .title(title)
                .addComponents(components)
                .build();
    }

    String customId();

    String title();

    List<Component> components();
}
