// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableThumbnail.class)
public non-sealed interface Thumbnail extends Component {

    static Builder builder() {
        return ImmutableThumbnail.builder();
    }

    static Thumbnail of(final String url) {
        return ImmutableThumbnail.of(UnfurledMedia.of(url));
    }

    @Override
    @Value.Derived
    default int type() {
        return 11;
    }

    @Value.Parameter
    UnfurledMedia media();

    Optional<String> description();

    @Value.Default
    default boolean spoiler() {
        return false;
    }

    interface Builder {
        Builder from(final Thumbnail instance);

        Builder id(final int id);

        Builder media(final UnfurledMedia media);

        Builder description(final String description);

        Builder spoiler(final boolean spoiler);

        Thumbnail build();
    }
}
