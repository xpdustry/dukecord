// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A rich embed. Received embeds may also carry the proxy URLs Discord adds. */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableEmbed.class)
public interface Embed {

    static ImmutableEmbed.Builder builder() {
        return ImmutableEmbed.builder();
    }

    Optional<String> title();

    Optional<String> description();

    Optional<String> url();

    Optional<Instant> timestamp();

    Optional<Integer> color();

    Optional<Author> author();

    List<Field> fields();

    Optional<Media> image();

    Optional<Media> thumbnail();

    Optional<Footer> footer();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Author.class)
    interface Author {

        static Author of(final String name) {
            return ImmutableEmbed.Author.builder().name(name).build();
        }

        String name();

        Optional<String> url();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Field.class)
    interface Field {

        static Field of(final String name, final String value) {
            return of(name, value, false);
        }

        static Field of(final String name, final String value, final boolean inline) {
            return ImmutableEmbed.Field.builder()
                    .name(name)
                    .value(value)
                    .inline(inline)
                    .build();
        }

        String name();

        String value();

        @Value.Default
        default boolean inline() {
            return false;
        }
    }

    /** An image or thumbnail. */
    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Media.class)
    interface Media {

        /** Takes a web URL or {@code attachment://filename}. */
        static Media of(final String url) {
            return ImmutableEmbed.Media.builder().url(url).build();
        }

        String url();

        Optional<String> proxyUrl();

        Optional<Integer> width();

        Optional<Integer> height();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Footer.class)
    interface Footer {

        static Footer of(final String text) {
            return ImmutableEmbed.Footer.builder().text(text).build();
        }

        String text();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();
    }
}
