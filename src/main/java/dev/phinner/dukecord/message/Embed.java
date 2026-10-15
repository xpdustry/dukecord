// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import java.awt.Color;
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

    static Builder builder() {
        return ImmutableEmbed.builder();
    }

    Optional<String> title();

    Optional<String> description();

    Optional<String> url();

    Optional<Instant> timestamp();

    Optional<Color> color();

    Optional<Author> author();

    List<Field> fields();

    Optional<Media> image();

    Optional<Media> thumbnail();

    Optional<Footer> footer();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Author.class)
    interface Author {

        static Builder builder() {
            return ImmutableEmbed.Author.builder();
        }

        static Author of(final String name) {
            return ImmutableEmbed.Author.of(name);
        }

        @Value.Parameter
        String name();

        Optional<String> url();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();

        interface Builder {
            Builder from(final Author instance);

            Builder name(final String name);

            Builder url(final String url);

            Builder iconUrl(final String iconUrl);

            Builder proxyIconUrl(final String proxyIconUrl);

            Author build();
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Field.class)
    interface Field {

        static Builder builder() {
            return ImmutableEmbed.Field.builder();
        }

        static Field of(final String name, final String value) {
            return of(name, value, false);
        }

        static Field of(final String name, final String value, final boolean inline) {
            return ImmutableEmbed.Field.of(name, value, inline);
        }

        @Value.Parameter
        String name();

        @Value.Parameter
        String value();

        @Value.Default
        @Value.Parameter
        default boolean inline() {
            return false;
        }

        interface Builder {
            Builder from(final Field instance);

            Builder name(final String name);

            Builder value(final String value);

            Builder inline(final boolean inline);

            Field build();
        }
    }

    /** An image or thumbnail. */
    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Media.class)
    interface Media {

        static Builder builder() {
            return ImmutableEmbed.Media.builder();
        }

        /** Takes a web URL or {@code attachment://filename}. */
        static Media of(final String url) {
            return ImmutableEmbed.Media.of(url);
        }

        @Value.Parameter
        String url();

        Optional<String> proxyUrl();

        Optional<Integer> width();

        Optional<Integer> height();

        interface Builder {
            Builder from(final Media instance);

            Builder url(final String url);

            Builder proxyUrl(final String proxyUrl);

            Builder width(final int width);

            Builder height(final int height);

            Media build();
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEmbed.Footer.class)
    interface Footer {

        static Builder builder() {
            return ImmutableEmbed.Footer.builder();
        }

        static Footer of(final String text) {
            return ImmutableEmbed.Footer.of(text);
        }

        @Value.Parameter
        String text();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();

        interface Builder {
            Builder from(final Footer instance);

            Builder text(final String text);

            Builder iconUrl(final String iconUrl);

            Builder proxyIconUrl(final String proxyIconUrl);

            Footer build();
        }
    }

    interface Builder {
        Builder from(final Embed instance);

        Builder title(final String title);

        Builder description(final String description);

        Builder url(final String url);

        Builder timestamp(final Instant timestamp);

        Builder color(final Color color);

        Builder author(final Author author);

        Builder addFields(final Field element);

        Builder addFields(final Field... elements);

        Builder fields(final Iterable<? extends Field> elements);

        Builder addAllFields(final Iterable<? extends Field> elements);

        Builder image(final Media image);

        Builder thumbnail(final Media thumbnail);

        Builder footer(final Footer footer);

        Embed build();
    }
}
