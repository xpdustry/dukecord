// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** An uploaded file shown in the layout, Discord's file component. */
@Value.Immutable
@JsonDeserialize(as = ImmutableFileDisplay.class)
public non-sealed interface FileDisplay extends Component {

    /** Takes the {@code attachment://filename} form of a file uploaded with the message. */
    static FileDisplay of(final String url) {
        return ImmutableFileDisplay.builder().file(UnfurledMedia.of(url)).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 13;
    }

    UnfurledMedia file();

    @Value.Default
    default boolean spoiler() {
        return false;
    }
}
