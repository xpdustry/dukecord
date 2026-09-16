// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** A rich embed. A value, built with {@link #builder()}; {@link #builder(Embed)} copies an existing one. */
public record Embed(
        Optional<String> title,
        Optional<String> description,
        Optional<String> url,
        Optional<Instant> timestamp,
        OptionalInt color,
        Optional<Footer> footer,
        Optional<Media> image,
        Optional<Media> thumbnail,
        Optional<Author> author,
        List<Field> fields) {

    public static final int MAX_FIELDS = 25;

    public record Footer(String text, Optional<String> iconUrl) {}

    public record Media(String url, Optional<Integer> width, Optional<Integer> height) {
        public static Media of(final String url) {
            return new Media(url, Optional.empty(), Optional.empty());
        }
    }

    public record Author(String name, Optional<String> url, Optional<String> iconUrl) {}

    public record Field(String name, String value, boolean inline) {}

    public Embed {
        fields = List.copyOf(fields);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(final Embed copy) {
        final var builder = new Builder();
        builder.title = copy.title;
        builder.description = copy.description;
        builder.url = copy.url;
        builder.timestamp = copy.timestamp;
        builder.color = copy.color;
        builder.footer = copy.footer;
        builder.image = copy.image;
        builder.thumbnail = copy.thumbnail;
        builder.author = copy.author;
        builder.fields.addAll(copy.fields);
        return builder;
    }

    /** The value of the first field with that name. */
    public Optional<String> field(final String name) {
        return this.fields.stream()
                .filter(field -> field.name().equals(name))
                .map(Field::value)
                .findFirst();
    }

    public static Embed fromJson(final Json json) {
        final List<Field> fields = new ArrayList<>();
        for (final var field : json.objects("fields")) {
            fields.add(new Field(field.string("name"), field.string("value"), field.bool("inline", false)));
        }
        return new Embed(
                json.optionalString("title"),
                json.optionalString("description"),
                json.optionalString("url"),
                json.optionalInstant("timestamp"),
                json.optionalInteger("color").map(OptionalInt::of).orElse(OptionalInt.empty()),
                json.optionalObject("footer").map(f -> new Footer(f.string("text"), f.optionalString("icon_url"))),
                json.optionalObject("image").map(Embed::media),
                json.optionalObject("thumbnail").map(Embed::media),
                json.optionalObject("author")
                        .map(a -> new Author(a.string("name"), a.optionalString("url"), a.optionalString("icon_url"))),
                fields);
    }

    private static Media media(final Json json) {
        return new Media(json.string("url"), json.optionalInteger("width"), json.optionalInteger("height"));
    }

    public Json toJson() {
        var json = Json.empty();
        if (this.title.isPresent()) {
            json = json.with("title", this.title.get());
        }
        if (this.description.isPresent()) {
            json = json.with("description", this.description.get());
        }
        if (this.url.isPresent()) {
            json = json.with("url", this.url.get());
        }
        if (this.timestamp.isPresent()) {
            json = json.with("timestamp", this.timestamp.get().toString());
        }
        if (this.color.isPresent()) {
            json = json.with("color", this.color.getAsInt());
        }
        if (this.footer.isPresent()) {
            var footer = Json.empty().with("text", this.footer.get().text());
            if (this.footer.get().iconUrl().isPresent()) {
                footer = footer.with("icon_url", this.footer.get().iconUrl().get());
            }
            json = json.with("footer", footer);
        }
        if (this.image.isPresent()) {
            json = json.with("image", Json.empty().with("url", this.image.get().url()));
        }
        if (this.thumbnail.isPresent()) {
            json = json.with(
                    "thumbnail", Json.empty().with("url", this.thumbnail.get().url()));
        }
        if (this.author.isPresent()) {
            var author = Json.empty().with("name", this.author.get().name());
            if (this.author.get().url().isPresent()) {
                author = author.with("url", this.author.get().url().get());
            }
            if (this.author.get().iconUrl().isPresent()) {
                author = author.with("icon_url", this.author.get().iconUrl().get());
            }
            json = json.with("author", author);
        }
        if (!this.fields.isEmpty()) {
            final List<Json> fields = new ArrayList<>();
            for (final var field : this.fields) {
                fields.add(Json.empty()
                        .with("name", field.name())
                        .with("value", field.value())
                        .with("inline", field.inline()));
            }
            json = json.with("fields", fields);
        }
        return json;
    }

    public static final class Builder {

        private Optional<String> title = Optional.empty();
        private Optional<String> description = Optional.empty();
        private Optional<String> url = Optional.empty();
        private Optional<Instant> timestamp = Optional.empty();
        private OptionalInt color = OptionalInt.empty();
        private Optional<Footer> footer = Optional.empty();
        private Optional<Media> image = Optional.empty();
        private Optional<Media> thumbnail = Optional.empty();
        private Optional<Author> author = Optional.empty();
        private final List<Field> fields = new ArrayList<>();

        private Builder() {}

        public Builder title(final String title) {
            this.title = Optional.of(title);
            return this;
        }

        public Builder description(final String description) {
            this.description = Optional.of(description);
            return this;
        }

        public Builder url(final String url) {
            this.url = Optional.of(url);
            return this;
        }

        public Builder timestamp(final Instant timestamp) {
            this.timestamp = Optional.of(timestamp);
            return this;
        }

        /** RGB colour, for example {@code 0x5865F2}. */
        public Builder color(final int rgb) {
            this.color = OptionalInt.of(rgb);
            return this;
        }

        public Builder footer(final String text) {
            this.footer = Optional.of(new Footer(text, Optional.empty()));
            return this;
        }

        public Builder footer(final String text, final String iconUrl) {
            this.footer = Optional.of(new Footer(text, Optional.of(iconUrl)));
            return this;
        }

        /** An {@code attachment://filename} URL refers to a file uploaded with the same message. */
        public Builder image(final String url) {
            this.image = Optional.of(Media.of(url));
            return this;
        }

        public Builder thumbnail(final String url) {
            this.thumbnail = Optional.of(Media.of(url));
            return this;
        }

        public Builder author(final String name) {
            this.author = Optional.of(new Author(name, Optional.empty(), Optional.empty()));
            return this;
        }

        public Builder author(final String name, final String url, final String iconUrl) {
            this.author = Optional.of(new Author(name, Optional.of(url), Optional.of(iconUrl)));
            return this;
        }

        public Builder field(final String name, final String value) {
            return this.field(name, value, false);
        }

        public Builder field(final String name, final String value, final boolean inline) {
            if (this.fields.size() >= MAX_FIELDS) {
                throw new IllegalStateException("An embed holds at most " + MAX_FIELDS + " fields");
            }
            this.fields.add(
                    new Field(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"), inline));
            return this;
        }

        /** An invisible inline field, used to align inline fields into rows. */
        public Builder blankField() {
            return this.field("​", "​", true);
        }

        public Builder clearFields() {
            this.fields.clear();
            return this;
        }

        public Embed build() {
            return new Embed(
                    this.title,
                    this.description,
                    this.url,
                    this.timestamp,
                    this.color,
                    this.footer,
                    this.image,
                    this.thumbnail,
                    this.author,
                    this.fields);
        }
    }
}
