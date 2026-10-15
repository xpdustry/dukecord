// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
        return ImmutableMediaGallery.of(Stream.of(urls).map(Item::of).toList());
    }

    @Override
    @Value.Derived
    default int type() {
        return 12;
    }

    @Value.Parameter
    List<Item> items();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableMediaGallery.Item.class)
    interface Item {

        static Builder builder() {
            return ImmutableMediaGallery.Item.builder();
        }

        static Item of(final String url) {
            return ImmutableMediaGallery.Item.of(UnfurledMedia.of(url));
        }

        @Value.Parameter
        UnfurledMedia media();

        Optional<String> description();

        @Value.Default
        default boolean spoiler() {
            return false;
        }

        interface Builder {
            Builder from(final Item instance);

            Builder media(final UnfurledMedia media);

            Builder description(final String description);

            Builder spoiler(final boolean spoiler);

            Item build();
        }
    }

    interface Builder {
        Builder from(final MediaGallery instance);

        Builder id(final int id);

        Builder addItems(final Item element);

        Builder addItems(final Item... elements);

        Builder items(final Iterable<? extends Item> elements);

        Builder addAllItems(final Iterable<? extends Item> elements);

        MediaGallery build();
    }
}
