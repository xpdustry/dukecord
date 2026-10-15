// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Markdown text. */
@Value.Immutable
@JsonDeserialize(as = ImmutableTextDisplay.class)
public non-sealed interface TextDisplay extends Component {

    static Builder builder() {
        return ImmutableTextDisplay.builder();
    }

    static TextDisplay of(final String content) {
        return ImmutableTextDisplay.of(content);
    }

    @Override
    @Value.Derived
    default int type() {
        return 10;
    }

    /** Not sent back in a submitted modal. */
    @Value.Default
    @Value.Parameter
    default String content() {
        return "";
    }

    interface Builder {
        Builder from(final TextDisplay instance);

        Builder id(final int id);

        Builder content(final String content);

        TextDisplay build();
    }
}
