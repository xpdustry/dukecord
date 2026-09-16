// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import org.jspecify.annotations.Nullable;

/**
 * An immutable JSON object. Values are {@link String}, {@link Number}, {@link Boolean}, {@link Json}, an unmodifiable
 * {@link List} of those, or {@code null}. Keys that are present with a {@code null} value are distinguishable from
 * absent keys, which is what Discord's partial updates require.
 */
public final class Json {

    private static final Json EMPTY = new Json(Map.of());

    private final Map<String, @Nullable Object> fields;

    private Json(final Map<String, @Nullable Object> fields) {
        this.fields = fields;
    }

    public static Json empty() {
        return EMPTY;
    }

    /** Parses a JSON object. */
    public static Json parse(final String text) {
        try {
            return of(JsonParser.object().from(Objects.requireNonNull(text, "text")));
        } catch (final JsonParserException e) {
            throw new JsonException("Invalid JSON object", e);
        }
    }

    /** Parses a JSON array of objects. */
    public static List<Json> parseArray(final String text) {
        try {
            final List<Json> result = new ArrayList<>();
            for (final var element : JsonParser.array().from(Objects.requireNonNull(text, "text"))) {
                if (!(element instanceof Map<?, ?> map)) {
                    throw new JsonException("Expected an array of objects");
                }
                result.add(of(map));
            }
            return Collections.unmodifiableList(result);
        } catch (final JsonParserException e) {
            throw new JsonException("Invalid JSON array", e);
        }
    }

    /** Deep-freezes a map into an immutable JSON object. */
    public static Json of(final Map<?, ?> map) {
        final Map<String, @Nullable Object> copy = new LinkedHashMap<>();
        for (final var entry : Objects.requireNonNull(map, "map").entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new JsonException("JSON object keys must be strings, got " + entry.getKey());
            }
            copy.put(key, freeze(entry.getValue()));
        }
        return new Json(Collections.unmodifiableMap(copy));
    }

    /** Freezes any JSON-compatible value: maps become {@link Json}, collections become unmodifiable lists. */
    public static @Nullable Object freeze(final @Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Json json -> json;
            case Map<?, ?> map -> of(map);
            case Collection<?> collection -> {
                final List<@Nullable Object> list = new ArrayList<>(collection.size());
                for (final var element : collection) {
                    list.add(freeze(element));
                }
                yield Collections.unmodifiableList(list);
            }
            case String string -> string;
            case Number number -> number;
            case Boolean bool -> bool;
            default ->
                throw new JsonException(
                        "Unsupported JSON value type " + value.getClass().getName());
        };
    }

    /** A shallow key overlay of {@code update} on {@code base}: present-and-null overwrites, absent keeps. */
    public static Json merge(final Json base, final Json update) {
        final Map<String, @Nullable Object> copy = new LinkedHashMap<>(base.fields);
        copy.putAll(update.fields);
        return new Json(Collections.unmodifiableMap(copy));
    }

    /** The underlying unmodifiable map. Values are frozen. */
    public Map<String, @Nullable Object> asMap() {
        return this.fields;
    }

    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    /** True if the key exists, even with a {@code null} value. */
    public boolean has(final String name) {
        return this.fields.containsKey(name);
    }

    /** True if the key exists and its value is {@code null}. */
    public boolean isNull(final String name) {
        return this.fields.containsKey(name) && this.fields.get(name) == null;
    }

    /** True if the key exists with a non-null value. */
    public boolean isPresent(final String name) {
        return this.fields.get(name) != null;
    }

    public @Nullable Object raw(final String name) {
        return this.fields.get(name);
    }

    public Possible<Object> possible(final String name) {
        if (!this.fields.containsKey(name)) {
            return Possible.absent();
        }
        final var value = this.fields.get(name);
        return value == null ? Possible.ofNull() : Possible.of(value);
    }

    public String string(final String name) {
        return this.optionalString(name).orElseThrow(() -> missing(name, "string"));
    }

    public Optional<String> optionalString(final String name) {
        return switch (this.fields.get(name)) {
            case null -> Optional.empty();
            case String string -> Optional.of(string);
            default -> throw invalid(name, "string");
        };
    }

    public int integer(final String name) {
        return this.optionalInteger(name).orElseThrow(() -> missing(name, "integer"));
    }

    public Optional<Integer> optionalInteger(final String name) {
        return this.optionalNumber(name).map(Number::intValue);
    }

    public long longValue(final String name) {
        return this.optionalLong(name).orElseThrow(() -> missing(name, "integer"));
    }

    public Optional<Long> optionalLong(final String name) {
        return this.optionalNumber(name).map(Number::longValue);
    }

    public Optional<Number> optionalNumber(final String name) {
        return switch (this.fields.get(name)) {
            case null -> Optional.empty();
            case Number number -> Optional.of(number);
            default -> throw invalid(name, "number");
        };
    }

    public boolean bool(final String name) {
        return this.optionalBoolean(name).orElseThrow(() -> missing(name, "boolean"));
    }

    public boolean bool(final String name, final boolean fallback) {
        return this.optionalBoolean(name).orElse(fallback);
    }

    public Optional<Boolean> optionalBoolean(final String name) {
        return switch (this.fields.get(name)) {
            case null -> Optional.empty();
            case Boolean bool -> Optional.of(bool);
            default -> throw invalid(name, "boolean");
        };
    }

    public Snowflake snowflake(final String name) {
        return this.optionalSnowflake(name).orElseThrow(() -> missing(name, "snowflake"));
    }

    public Optional<Snowflake> optionalSnowflake(final String name) {
        return switch (this.fields.get(name)) {
            case null -> Optional.empty();
            case String string -> Optional.of(parseSnowflake(name, string));
            case Number number -> Optional.of(Snowflake.of(number.longValue()));
            default -> throw invalid(name, "snowflake");
        };
    }

    /** Reads an array of snowflakes; an absent or null key yields an empty set. */
    public SequencedSet<Snowflake> snowflakes(final String name) {
        final var result = new LinkedHashSet<Snowflake>();
        for (final var element : this.list(name)) {
            result.add(
                    switch (element) {
                        case String string -> parseSnowflake(name, string);
                        case Number number -> Snowflake.of(number.longValue());
                        case null, default -> throw invalid(name, "array of snowflakes");
                    });
        }
        return Collections.unmodifiableSequencedSet(result);
    }

    public Instant instant(final String name) {
        return this.optionalInstant(name).orElseThrow(() -> missing(name, "timestamp"));
    }

    public Optional<Instant> optionalInstant(final String name) {
        try {
            return this.optionalString(name).map(Instant::parse);
        } catch (final DateTimeParseException e) {
            throw new JsonException("Field '" + name + "' is not an ISO-8601 timestamp", e);
        }
    }

    public Json object(final String name) {
        return this.optionalObject(name).orElseThrow(() -> missing(name, "object"));
    }

    public Optional<Json> optionalObject(final String name) {
        return switch (this.fields.get(name)) {
            case null -> Optional.empty();
            case Json json -> Optional.of(json);
            default -> throw invalid(name, "object");
        };
    }

    /** Reads an array; an absent or null key yields an empty list. */
    public List<@Nullable Object> list(final String name) {
        return switch (this.fields.get(name)) {
            case null -> List.of();
            case List<?> list -> {
                @SuppressWarnings("unchecked")
                final var cast = (List<@Nullable Object>) list;
                yield cast;
            }
            default -> throw invalid(name, "array");
        };
    }

    /** Reads an array of objects; an absent or null key yields an empty list. */
    public List<Json> objects(final String name) {
        final List<Json> result = new ArrayList<>();
        for (final var element : this.list(name)) {
            if (!(element instanceof Json json)) {
                throw invalid(name, "array of objects");
            }
            result.add(json);
        }
        return Collections.unmodifiableList(result);
    }

    /** Reads an array of strings; an absent or null key yields an empty list. */
    public List<String> strings(final String name) {
        final List<String> result = new ArrayList<>();
        for (final var element : this.list(name)) {
            if (!(element instanceof String string)) {
                throw invalid(name, "array of strings");
            }
            result.add(string);
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a copy with the given key set to the frozen value ({@code null} is a valid value). */
    public Json with(final String name, final @Nullable Object value) {
        final Map<String, @Nullable Object> copy = new LinkedHashMap<>(this.fields);
        copy.put(Objects.requireNonNull(name, "name"), freeze(value));
        return new Json(Collections.unmodifiableMap(copy));
    }

    /** Returns a copy without the given key. */
    public Json without(final String name) {
        if (!this.fields.containsKey(name)) {
            return this;
        }
        final Map<String, @Nullable Object> copy = new LinkedHashMap<>(this.fields);
        copy.remove(name);
        return new Json(Collections.unmodifiableMap(copy));
    }

    /** Converts back to plain maps and lists, for serialization. */
    public Map<String, @Nullable Object> toRaw() {
        final Map<String, @Nullable Object> raw = new LinkedHashMap<>();
        for (final var entry : this.fields.entrySet()) {
            raw.put(entry.getKey(), thaw(entry.getValue()));
        }
        return raw;
    }

    private static @Nullable Object thaw(final @Nullable Object value) {
        return switch (value) {
            case Json json -> json.toRaw();
            case List<?> list -> {
                final List<@Nullable Object> copy = new ArrayList<>(list.size());
                for (final var element : list) {
                    copy.add(thaw(element));
                }
                yield copy;
            }
            case null, default -> value;
        };
    }

    private static Snowflake parseSnowflake(final String name, final String value) {
        try {
            return Snowflake.of(value);
        } catch (final NumberFormatException e) {
            throw new JsonException("Field '" + name + "' is not a snowflake: " + value, e);
        }
    }

    private static JsonException missing(final String name, final String expected) {
        return new JsonException("Missing " + expected + " field '" + name + "'");
    }

    private JsonException invalid(final String name, final String expected) {
        return new JsonException("Field '" + name + "' is not a " + expected + ": " + this.fields.get(name));
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof Json json && this.fields.equals(json.fields);
    }

    @Override
    public int hashCode() {
        return this.fields.hashCode();
    }

    @Override
    public String toString() {
        return JsonWriter.string(this.toRaw());
    }
}
