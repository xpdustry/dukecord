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

    static Builder builder() {
        return ImmutableMediaGallery.builder();
    }

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

        static Builder builder() {
            return ImmutableMediaGallery.Item.builder();
        }

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

        interface Builder {
            Builder from(Item instance);

            Builder media(UnfurledMedia media);

            Builder description(String description);

            Builder description(Optional<String> description);

            Builder spoiler(boolean spoiler);

            Item build();
        }
    }

    interface Builder {
        Builder from(MediaGallery instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder addItems(Item element);

        Builder addItems(Item... elements);

        Builder items(Iterable<? extends Item> elements);

        Builder addAllItems(Iterable<? extends Item> elements);

        MediaGallery build();
    }
}
