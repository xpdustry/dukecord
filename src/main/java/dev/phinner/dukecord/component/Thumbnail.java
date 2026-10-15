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
        return ImmutableThumbnail.builder().media(UnfurledMedia.of(url)).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 11;
    }

    UnfurledMedia media();

    Optional<String> description();

    @Value.Default
    default boolean spoiler() {
        return false;
    }

    interface Builder {
        Builder from(Thumbnail instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder media(UnfurledMedia media);

        Builder description(String description);

        Builder description(Optional<String> description);

        Builder spoiler(boolean spoiler);

        Thumbnail build();
    }
}
