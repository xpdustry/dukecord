// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A grid of up to ten images or videos. */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableMediaGallery.class)
public non-sealed interface MediaGallery extends Component {

    static MediaGallery of(final String... urls) {
        final var builder = ImmutableMediaGallery.builder();
        for (final var url : urls) {
            builder.addItems(Item.of(url));
        }
        return builder.build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 12;
    }

    List<Item> items();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableMediaGallery.Item.class)
    interface Item {

        static Item of(final String url) {
            return ImmutableMediaGallery.Item.builder()
                    .media(UnfurledMedia.of(url))
                    .build();
        }

        UnfurledMedia media();

        Optional<String> description();

        @Value.Default
        default boolean spoiler() {
            return false;
        }
    }
}
