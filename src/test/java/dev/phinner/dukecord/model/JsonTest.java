// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonTest {

    private static final String GUILD = """
            {
              "id": "175928847299117063",
              "name": "Xpdustry",
              "description": null,
              "member_count": 42,
              "large": true,
              "features": ["COMMUNITY", "NEWS"],
              "owner": {"id": "302096570298990593", "username": "phinner"},
              "roles": [
                {"id": "41771983423143936", "name": "@everyone"},
                {"id": "41771983423143937", "name": "moderator"}
              ]
            }""";

    // ---- parsing ----

    @Test
    void parsesAnObject() {
        final var json = Json.parse(GUILD);
        assertThat(json.string("name")).isEqualTo("Xpdustry");
        assertThat(json.integer("member_count")).isEqualTo(42);
        assertThat(json.bool("large")).isTrue();
        assertThat(json.strings("features")).containsExactly("COMMUNITY", "NEWS");
        assertThat(json.object("owner").string("username")).isEqualTo("phinner");
        assertThat(json.objects("roles")).hasSize(2);
    }

    @Test
    void parseRejectsMalformedJson() {
        assertThatThrownBy(() -> Json.parse("{\"a\":"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Invalid JSON object")
                .cause()
                .isNotNull();
    }

    @Test
    void parseRejectsAnArray() {
        assertThatThrownBy(() -> Json.parse("[]"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Invalid JSON object");
    }

    @Test
    void parsesAnArrayOfObjects() {
        final var array = Json.parseArray("""
                [{"id": "1", "name": "a"}, {"id": "2", "name": "b"}]""");
        assertThat(array).hasSize(2);
        assertThat(array.get(0).string("name")).isEqualTo("a");
        assertThat(array.get(1).snowflake("id")).isEqualTo(Snowflake.of(2L));
        assertThatThrownBy(() -> array.add(Json.empty())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void parseArrayRejectsNonObjectElements() {
        assertThatThrownBy(() -> Json.parseArray("[1, 2]"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Expected an array of objects");
    }

    @Test
    void parseArrayRejectsMalformedJson() {
        assertThatThrownBy(() -> Json.parseArray("nope"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Invalid JSON array");
    }

    // ---- freezing ----

    @Test
    void ofDeepFreezesMapsAndLists() {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("inner", new LinkedHashMap<>(Map.of("deep", "value")));
        nested.put("list", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("k", "v")))));
        final var json = Json.of(nested);

        assertThat(json.object("inner")).isInstanceOf(Json.class);
        assertThat(json.object("inner").string("deep")).isEqualTo("value");
        assertThat(json.list("list").getFirst()).isInstanceOf(Json.class);

        // The source map is copied, not aliased.
        nested.put("added", "late");
        assertThat(json.has("added")).isFalse();
    }

    @Test
    void frozenCollectionsRejectMutation() {
        final var json = Json.parse("{\"a\": [1, 2], \"b\": {\"c\": 1}}");
        assertThatThrownBy(() -> json.asMap().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> json.list("a").add(3)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> json.object("b").asMap().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> json.objects("a")).isInstanceOf(JsonException.class);
    }

    @Test
    void ofRejectsNonStringKeys() {
        final Map<Object, Object> map = new HashMap<>();
        map.put(1, "one");
        assertThatThrownBy(() -> Json.of(map))
                .isInstanceOf(JsonException.class)
                .hasMessage("JSON object keys must be strings, got 1");
    }

    @Test
    void freezeRejectsUnsupportedValues() {
        assertThatThrownBy(() -> Json.freeze(Instant.EPOCH))
                .isInstanceOf(JsonException.class)
                .hasMessage("Unsupported JSON value type java.time.Instant");
        assertThat(Json.freeze(null)).isNull();
        assertThat(Json.freeze(Json.empty())).isEqualTo(Json.empty());
    }

    // ---- tri-state ----

    @Test
    void distinguishesAbsentFromNullFromPresent() {
        final var json = Json.parse("{\"present\": \"x\", \"nulled\": null}");

        assertThat(json.has("present")).isTrue();
        assertThat(json.has("nulled")).isTrue();
        assertThat(json.has("absent")).isFalse();

        assertThat(json.isNull("present")).isFalse();
        assertThat(json.isNull("nulled")).isTrue();
        assertThat(json.isNull("absent")).isFalse();

        assertThat(json.isPresent("present")).isTrue();
        assertThat(json.isPresent("nulled")).isFalse();
        assertThat(json.isPresent("absent")).isFalse();
    }

    @Test
    void possibleMirrorsTheThreeStates() {
        final var json = Json.parse("{\"present\": \"x\", \"nulled\": null}");
        assertThat(json.possible("present")).isEqualTo(Possible.of("x"));
        assertThat(json.possible("nulled")).isEqualTo(Possible.ofNull());
        assertThat(json.possible("absent")).isEqualTo(Possible.absent());
    }

    @Test
    void emptyIsEmptyAndShared() {
        assertThat(Json.empty().isEmpty()).isTrue();
        assertThat(Json.empty()).isSameAs(Json.empty());
        assertThat(Json.parse("{\"a\":1}").isEmpty()).isFalse();
    }

    @Test
    void rawReturnsTheFrozenValue() {
        final var json = Json.parse("{\"a\": 1, \"b\": null}");
        assertThat(json.raw("a")).isEqualTo(1);
        assertThat(json.raw("b")).isNull();
        assertThat(json.raw("missing")).isNull();
    }

    // ---- typed accessors ----

    @Test
    void readsStrings() {
        final var json = Json.parse("{\"name\": \"phinner\", \"n\": 1, \"nulled\": null}");
        assertThat(json.string("name")).isEqualTo("phinner");
        assertThat(json.optionalString("name")).contains("phinner");
        assertThat(json.optionalString("missing")).isEmpty();
        assertThat(json.optionalString("nulled")).isEmpty();
        assertThatThrownBy(() -> json.string("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing string field 'missing'");
        assertThatThrownBy(() -> json.string("n"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'n' is not a string: 1");
    }

    @Test
    void readsNumbers() {
        final var json = Json.parse("{\"count\": 42, \"big\": 9007199254740993, \"text\": \"no\"}");
        assertThat(json.integer("count")).isEqualTo(42);
        assertThat(json.optionalInteger("count")).contains(42);
        assertThat(json.optionalInteger("missing")).isEmpty();
        assertThat(json.longValue("big")).isEqualTo(9007199254740993L);
        assertThat(json.optionalLong("big")).contains(9007199254740993L);
        assertThat(json.optionalNumber("missing")).isEmpty();
        assertThatThrownBy(() -> json.integer("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing integer field 'missing'");
        assertThatThrownBy(() -> json.longValue("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing integer field 'missing'");
        assertThatThrownBy(() -> json.integer("text"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'text' is not a number: no");
    }

    @Test
    void readsBooleans() {
        final var json = Json.parse("{\"bot\": true, \"text\": \"no\"}");
        assertThat(json.bool("bot")).isTrue();
        assertThat(json.bool("missing", true)).isTrue();
        assertThat(json.bool("bot", false)).isTrue();
        assertThat(json.optionalBoolean("missing")).isEmpty();
        assertThatThrownBy(() -> json.bool("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing boolean field 'missing'");
        assertThatThrownBy(() -> json.bool("text"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'text' is not a boolean: no");
    }

    @Test
    void readsSnowflakesFromStringsAndNumbers() {
        final var json = Json.parse("""
                {"id": "175928847299117063", "numeric": 175928847299117063, "bad": "abc", "wrong": true}""");
        assertThat(json.snowflake("id")).isEqualTo(Snowflake.of(175928847299117063L));
        assertThat(json.snowflake("numeric")).isEqualTo(Snowflake.of(175928847299117063L));
        assertThat(json.optionalSnowflake("missing")).isEmpty();
        assertThatThrownBy(() -> json.snowflake("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing snowflake field 'missing'");
        assertThatThrownBy(() -> json.snowflake("bad"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'bad' is not a snowflake: abc");
        assertThatThrownBy(() -> json.snowflake("wrong"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'wrong' is not a snowflake: true");
    }

    @Test
    void readsSnowflakeArrays() {
        final var json = Json.parse("""
                {"roles": ["41771983423143937", 41771983423143938, "41771983423143937"], "bad": [true]}""");
        assertThat(json.snowflakes("roles"))
                .containsExactly(Snowflake.of(41771983423143937L), Snowflake.of(41771983423143938L));
        assertThat(json.snowflakes("missing")).isEmpty();
        assertThatThrownBy(() -> json.snowflakes("roles").add(Snowflake.of(1L)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> json.snowflakes("bad"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Field 'bad' is not a array of snowflakes");
    }

    @Test
    void readsInstants() {
        final var json = Json.parse("""
                {"joined_at": "2016-04-30T11:18:25.796Z", "bad": "yesterday", "wrong": 1}""");
        assertThat(json.instant("joined_at")).isEqualTo(Instant.parse("2016-04-30T11:18:25.796Z"));
        assertThat(json.optionalInstant("missing")).isEmpty();
        assertThatThrownBy(() -> json.instant("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing timestamp field 'missing'");
        assertThatThrownBy(() -> json.instant("bad"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'bad' is not an ISO-8601 timestamp");
        assertThatThrownBy(() -> json.instant("wrong"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'wrong' is not a string: 1");
    }

    @Test
    void readsObjects() {
        final var json = Json.parse("""
                {"author": {"id": "1"}, "authors": [{"id": "1"}], "bad": [1], "wrong": 5}""");
        assertThat(json.object("author").string("id")).isEqualTo("1");
        assertThat(json.optionalObject("missing")).isEmpty();
        assertThat(json.objects("authors")).hasSize(1);
        assertThat(json.objects("missing")).isEmpty();
        assertThatThrownBy(() -> json.object("missing"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Missing object field 'missing'");
        assertThatThrownBy(() -> json.object("wrong"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'wrong' is not a object: 5");
        assertThatThrownBy(() -> json.objects("bad"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Field 'bad' is not a array of objects");
        assertThatThrownBy(() -> json.objects("authors").add(Json.empty()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsListsAndStringArrays() {
        final var json = Json.parse("""
                {"features": ["A", "B"], "mixed": ["A", 1], "wrong": 5, "nulled": null}""");
        assertThat(json.strings("features")).containsExactly("A", "B");
        assertThat(json.strings("missing")).isEmpty();
        assertThat(json.strings("nulled")).isEmpty();
        assertThat(json.list("features")).containsExactly("A", "B");
        assertThat(json.list("missing")).isEmpty();
        assertThatThrownBy(() -> json.strings("mixed"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Field 'mixed' is not a array of strings");
        assertThatThrownBy(() -> json.list("wrong"))
                .isInstanceOf(JsonException.class)
                .hasMessage("Field 'wrong' is not a array: 5");
        assertThatThrownBy(() -> json.strings("features").add("C")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void snowflakeArraysKeepDiscordsOrderAndDropDuplicates() {
        final var json = Json.parse("""
                {"roles": ["3", "1", "2", "1"]}""");
        assertThat(json.snowflakes("roles")).containsExactly(Snowflake.of(3L), Snowflake.of(1L), Snowflake.of(2L));
    }

    @Test
    void arraysWithNullElementsFailTypedReadsInsteadOfReturningNulls() {
        final var json = Json.parse("""
                {"objects": [null], "strings": [null]}""");
        assertThat(json.list("objects")).containsExactly((Object) null);
        assertThatThrownBy(() -> Json.parse("{\"roles\": [null]}").snowflakes("roles"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Field 'roles' is not a array of snowflakes");
        assertThatThrownBy(() -> json.objects("objects")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> json.strings("strings")).isInstanceOf(JsonException.class);
    }

    // ---- copies ----

    @Test
    void withAddsOrReplacesAKeyWithoutTouchingTheOriginal() {
        final var json = Json.parse("{\"a\": 1}");
        final var updated = json.with("b", Map.of("c", 2)).with("a", 9);

        assertThat(json.asMap()).containsExactly(Map.entry("a", 1));
        assertThat(updated.integer("a")).isEqualTo(9);
        assertThat(updated.object("b").integer("c")).isEqualTo(2);
    }

    @Test
    void withNullWritesAPresentNull() {
        final var json = Json.empty().with("nick", null);
        assertThat(json.has("nick")).isTrue();
        assertThat(json.isNull("nick")).isTrue();
        assertThat(json.isPresent("nick")).isFalse();
    }

    @Test
    void withoutRemovesAKeyAndIsIdentityWhenAbsent() {
        final var json = Json.parse("{\"a\": 1, \"b\": 2}");
        assertThat(json.without("a").has("a")).isFalse();
        assertThat(json.without("a").integer("b")).isEqualTo(2);
        assertThat(json.without("missing")).isSameAs(json);
    }

    @Test
    void mergeOverwritesWithPresentNullsAndKeepsAbsentKeys() {
        final var base = Json.parse("""
                {"id": "1", "nick": "old", "roles": ["1", "2"], "kept": true}""");
        final var update = Json.parse("""
                {"nick": null, "roles": ["3"], "added": 5}""");
        final var merged = Json.merge(base, update);

        assertThat(merged.isNull("nick")).isTrue();
        assertThat(merged.string("id")).isEqualTo("1");
        assertThat(merged.bool("kept")).isTrue();
        assertThat(merged.integer("added")).isEqualTo(5);
        // Arrays are replaced wholesale, never element-merged.
        assertThat(merged.strings("roles")).containsExactly("3");
        assertThat(base.string("nick")).isEqualTo("old");
    }

    // ---- serialization ----

    @Test
    void toRawAndToStringRoundTrip() {
        final var json = Json.parse(GUILD);
        final var raw = json.toRaw();

        assertThat(raw.get("owner")).isInstanceOf(Map.class).isNotInstanceOf(Json.class);
        assertThat(raw.get("roles")).isInstanceOf(List.class);
        assertThat(raw.get("roles"))
                .asInstanceOf(InstanceOfAssertFactories.list(Object.class))
                .first()
                .isInstanceOf(Map.class);
        assertThat(raw).containsEntry("description", null);

        assertThat(Json.of(raw)).isEqualTo(json);
        assertThat(Json.parse(json.toString())).isEqualTo(json);
    }

    @Test
    void toRawReturnsADetachedMutableCopy() {
        final var json = Json.parse("{\"a\": {\"b\": 1}}");
        final var raw = json.toRaw();
        raw.put("c", 2);
        assertThat(json.has("c")).isFalse();
    }

    @Test
    void equalsAndHashCodeAreStructural() {
        final var a = Json.parse("{\"x\": 1, \"y\": {\"z\": [1, 2]}}");
        final var b = Json.parse("{\"y\": {\"z\": [1, 2]}, \"x\": 1}");
        final var c = Json.parse("{\"x\": 1, \"y\": {\"z\": [2, 1]}}");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(Json.empty());
        assertThat(Json.empty()).isEqualTo(Json.of(Map.of()));
    }

    @Test
    void jsonExceptionKeepsMessageAndCause() {
        final var cause = new IllegalStateException("boom");
        assertThat(new JsonException("message")).hasMessage("message").hasNoCause();
        assertThat(new JsonException("message", cause)).hasMessage("message").hasCause(cause);
    }
}
