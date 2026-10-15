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
            return ImmutableEmbed.Author.builder().name(name).build();
        }

        String name();

        Optional<String> url();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();

        interface Builder {
            Builder from(Author instance);

            Builder name(String name);

            Builder url(String url);

            Builder url(Optional<String> url);

            Builder iconUrl(String iconUrl);

            Builder iconUrl(Optional<String> iconUrl);

            Builder proxyIconUrl(String proxyIconUrl);

            Builder proxyIconUrl(Optional<String> proxyIconUrl);

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

        interface Builder {
            Builder from(Field instance);

            Builder name(String name);

            Builder value(String value);

            Builder inline(boolean inline);

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
            return ImmutableEmbed.Media.builder().url(url).build();
        }

        String url();

        Optional<String> proxyUrl();

        Optional<Integer> width();

        Optional<Integer> height();

        interface Builder {
            Builder from(Media instance);

            Builder url(String url);

            Builder proxyUrl(String proxyUrl);

            Builder proxyUrl(Optional<String> proxyUrl);

            Builder width(int width);

            Builder width(Optional<Integer> width);

            Builder height(int height);

            Builder height(Optional<Integer> height);

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
            return ImmutableEmbed.Footer.builder().text(text).build();
        }

        String text();

        Optional<String> iconUrl();

        Optional<String> proxyIconUrl();

        interface Builder {
            Builder from(Footer instance);

            Builder text(String text);

            Builder iconUrl(String iconUrl);

            Builder iconUrl(Optional<String> iconUrl);

            Builder proxyIconUrl(String proxyIconUrl);

            Builder proxyIconUrl(Optional<String> proxyIconUrl);

            Footer build();
        }
    }

    interface Builder {
        Builder from(Embed instance);

        Builder title(String title);

        Builder title(Optional<String> title);

        Builder description(String description);

        Builder description(Optional<String> description);

        Builder url(String url);

        Builder url(Optional<String> url);

        Builder timestamp(Instant timestamp);

        Builder timestamp(Optional<? extends Instant> timestamp);

        Builder color(Color color);

        Builder color(Optional<? extends Color> color);

        Builder author(Author author);

        Builder author(Optional<? extends Author> author);

        Builder addFields(Field element);

        Builder addFields(Field... elements);

        Builder fields(Iterable<? extends Field> elements);

        Builder addAllFields(Iterable<? extends Field> elements);

        Builder image(Media image);

        Builder image(Optional<? extends Media> image);

        Builder thumbnail(Media thumbnail);

        Builder thumbnail(Optional<? extends Media> thumbnail);

        Builder footer(Footer footer);

        Builder footer(Optional<? extends Footer> footer);

        Embed build();
    }
}
