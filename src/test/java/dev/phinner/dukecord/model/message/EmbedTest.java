// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbedTest {

    private static final String FULL = """
            {
              "title": "Server started",
              "description": "The map is **Fork**.",
              "url": "https://xpdustry.com",
              "timestamp": "2026-09-15T10:30:00Z",
              "color": 5793266,
              "footer": {"text": "Imperium", "icon_url": "https://cdn.example/footer.png"},
              "image": {"url": "attachment://map.png"},
              "thumbnail": {"url": "https://cdn.example/thumb.png"},
              "author": {"name": "phinner", "url": "https://phinner.dev", "icon_url": "https://cdn.example/a.png"},
              "fields": [
                {"name": "Players", "value": "12", "inline": true},
                {"name": "Wave", "value": "34", "inline": false}
              ]
            }""";

    @Test
    void builderSetsOnlyWhatIsAsked() {
        final var embed = Embed.builder()
                .title("Server started")
                .description("The map is **Fork**.")
                .url("https://xpdustry.com")
                .timestamp(Instant.parse("2026-09-15T10:30:00Z"))
                .color(0x5865F2)
                .footer("Imperium", "https://cdn.example/footer.png")
                .image("attachment://map.png")
                .thumbnail("https://cdn.example/thumb.png")
                .author("phinner", "https://phinner.dev", "https://cdn.example/a.png")
                .field("Players", "12", true)
                .field("Wave", "34")
                .build();

        assertThat(embed.title()).contains("Server started");
        assertThat(embed.description()).contains("The map is **Fork**.");
        assertThat(embed.url()).contains("https://xpdustry.com");
        assertThat(embed.timestamp()).contains(Instant.parse("2026-09-15T10:30:00Z"));
        assertThat(embed.color()).hasValue(0x5865F2);
        assertThat(embed.footer())
                .contains(new Embed.Footer("Imperium", Optional.of("https://cdn.example/footer.png")));
        assertThat(embed.image()).contains(Embed.Media.of("attachment://map.png"));
        assertThat(embed.thumbnail()).contains(Embed.Media.of("https://cdn.example/thumb.png"));
        assertThat(embed.author())
                .contains(new Embed.Author(
                        "phinner", Optional.of("https://phinner.dev"), Optional.of("https://cdn.example/a.png")));
        assertThat(embed.fields())
                .containsExactly(new Embed.Field("Players", "12", true), new Embed.Field("Wave", "34", false));
    }

    @Test
    void anEmptyBuilderProducesAnEmptyEmbed() {
        final var embed = Embed.builder().build();
        assertThat(embed.title()).isEmpty();
        assertThat(embed.color()).isEmpty();
        assertThat(embed.fields()).isEmpty();
        assertThat(embed.toJson()).isEqualTo(Json.empty());
    }

    @Test
    void singleArgumentFooterAndAuthorLeaveTheUrlsOut() {
        final var embed = Embed.builder().footer("Imperium").author("phinner").build();
        assertThat(embed.footer()).contains(new Embed.Footer("Imperium", Optional.empty()));
        assertThat(embed.author()).contains(new Embed.Author("phinner", Optional.empty(), Optional.empty()));
        assertThat(embed.toJson()).isEqualTo(Json.parse("""
                        {"footer": {"text": "Imperium"}, "author": {"name": "phinner"}}"""));
    }

    @Test
    void builderCopyStartsFromAnExistingEmbed() {
        final var original = Embed.fromJson(Json.parse(FULL));
        final var copy = Embed.builder(original).build();
        assertThat(copy).isEqualTo(original);

        final var changed = Embed.builder(original).title("Server stopped").build();
        assertThat(changed.title()).contains("Server stopped");
        assertThat(changed.fields()).isEqualTo(original.fields());
        assertThat(original.title()).contains("Server started");
    }

    @Test
    void builderCopyDoesNotAliasTheFieldList() {
        final var original = Embed.builder().field("a", "1").build();
        final var copy = Embed.builder(original).field("b", "2").build();
        assertThat(original.fields()).hasSize(1);
        assertThat(copy.fields()).hasSize(2);
    }

    @Test
    void fieldByNameReturnsTheFirstMatch() {
        final var embed = Embed.builder()
                .field("Players", "12")
                .field("Wave", "34")
                .field("Players", "99")
                .build();
        assertThat(embed.field("Players")).contains("12");
        assertThat(embed.field("Wave")).contains("34");
        assertThat(embed.field("Missing")).isEmpty();
    }

    @Test
    void fieldsAreCappedAtTheDiscordMaximum() {
        assertThat(Embed.MAX_FIELDS).isEqualTo(25);
        final var builder = Embed.builder();
        for (int i = 0; i < Embed.MAX_FIELDS; i++) {
            builder.field("name" + i, "value" + i);
        }
        assertThat(builder.build().fields()).hasSize(Embed.MAX_FIELDS);
        assertThatThrownBy(() -> builder.field("one", "too many"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An embed holds at most 25 fields");

        builder.clearFields();
        assertThat(builder.build().fields()).isEmpty();
        assertThat(builder.field("again", "ok").build().fields()).hasSize(1);
    }

    @Test
    void blankFieldIsAnInvisibleInlineField() {
        final var embed = Embed.builder().blankField().build();
        assertThat(embed.fields()).hasSize(1);
        assertThat(embed.fields().getFirst().inline()).isTrue();
        assertThat(embed.fields().getFirst().name()).isEqualTo("​");
        assertThat(embed.fields().getFirst().value()).isEqualTo("​");
    }

    @Test
    void embedIsAValueWithAnUnmodifiableFieldList() {
        final var embed = Embed.builder().field("a", "1").build();
        assertThat(embed).isEqualTo(Embed.builder().field("a", "1").build());
        assertThat(embed).hasSameHashCodeAs(Embed.builder().field("a", "1").build());
        assertThatThrownBy(() -> embed.fields().add(new Embed.Field("b", "2", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsEveryNestedPartFromJson() {
        final var embed = Embed.fromJson(Json.parse(FULL));
        assertThat(embed.title()).contains("Server started");
        assertThat(embed.color()).hasValue(5793266);
        assertThat(embed.timestamp()).contains(Instant.parse("2026-09-15T10:30:00Z"));
        assertThat(embed.footer().orElseThrow().iconUrl()).contains("https://cdn.example/footer.png");
        assertThat(embed.image().orElseThrow().url()).isEqualTo("attachment://map.png");
        assertThat(embed.thumbnail().orElseThrow().url()).isEqualTo("https://cdn.example/thumb.png");
        assertThat(embed.author().orElseThrow().name()).isEqualTo("phinner");
        assertThat(embed.fields()).hasSize(2);
        assertThat(embed.fields().getFirst().inline()).isTrue();
        assertThat(embed.fields().getLast().inline()).isFalse();
    }

    @Test
    void readsMediaDimensionsWhenDiscordSendsThem() {
        final var embed = Embed.fromJson(Json.parse("""
                {"image": {"url": "https://cdn.example/i.png", "width": 800, "height": 600}}"""));
        assertThat(embed.image().orElseThrow().width()).contains(800);
        assertThat(embed.image().orElseThrow().height()).contains(600);
    }

    @Test
    void roundTripsThroughJson() {
        final var json = Json.parse(FULL);
        final var embed = Embed.fromJson(json);
        assertThat(embed.toJson()).isEqualTo(json);
        assertThat(Embed.fromJson(embed.toJson())).isEqualTo(embed);
    }

    @Test
    void toJsonSendsOnlyTheMediaUrlBackToDiscord() {
        // Discord computes width and height itself, so a received embed does not round-trip them.
        final var embed = Embed.fromJson(Json.parse("""
                {"image": {"url": "https://cdn.example/i.png", "width": 800, "height": 600}}"""));
        assertThat(embed.toJson()).isEqualTo(Json.parse("{\"image\": {\"url\": \"https://cdn.example/i.png\"}}"));
        assertThat(Embed.fromJson(embed.toJson()).image().orElseThrow().width()).isEmpty();
    }

    @Test
    void toJsonOmitsEveryKeyThatWasNeverSet() {
        final var embed = Embed.builder().description("only this").build();
        assertThat(embed.toJson()).isEqualTo(Json.parse("{\"description\": \"only this\"}"));
        assertThat(embed.toJson().asMap()).containsOnlyKeys("description");
    }
}
