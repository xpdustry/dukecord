// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmojiTest {

    @Test
    void unicodeEmojiHasNoId() {
        final var emoji = Emoji.unicode("🔥");
        assertThat(emoji.id()).isEmpty();
        assertThat(emoji.name()).isEqualTo("🔥");
        assertThat(emoji.animated()).isFalse();
        assertThat(emoji.isCustom()).isFalse();
    }

    @Test
    void customEmojiCarriesItsId() {
        final var emoji = Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", true);
        assertThat(emoji.id()).contains(Snowflake.of("41771983429993937"));
        assertThat(emoji.name()).isEqualTo("mindustry");
        assertThat(emoji.animated()).isTrue();
        assertThat(emoji.isCustom()).isTrue();
    }

    @Test
    void readsAUnicodeEmojiFromJson() {
        final var emoji = Emoji.fromJson(Json.parse("""
                {"id": null, "name": "🔥"}"""));
        assertThat(emoji).isEqualTo(Emoji.unicode("🔥"));
    }

    @Test
    void readsACustomEmojiFromJson() {
        final var emoji = Emoji.fromJson(Json.parse("""
                {"id": "41771983429993937", "name": "mindustry", "animated": true}"""));
        assertThat(emoji).isEqualTo(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", true));
    }

    @Test
    void readsAnEmojiWithoutANameAsTheEmptyString() {
        assertThat(Emoji.fromJson(Json.parse("{\"id\": \"1\"}")))
                .isEqualTo(new Emoji(Optional.of(Snowflake.of(1L)), "", false));
    }

    @Test
    void writesOnlyTheNameForUnicodeEmoji() {
        assertThat(Emoji.unicode("🔥").toJson()).isEqualTo(Json.parse("""
                {"name": "🔥"}"""));
    }

    @Test
    void writesIdAndAnimatedForCustomEmoji() {
        final var emoji = Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", false);
        assertThat(emoji.toJson()).isEqualTo(Json.parse("""
                        {"name": "mindustry", "id": "41771983429993937", "animated": false}"""));
        assertThat(Emoji.fromJson(emoji.toJson())).isEqualTo(emoji);
    }

    @Test
    void mentionIsTheCharacterOrTheAngleBracketForm() {
        assertThat(Emoji.unicode("🔥").mention()).isEqualTo("🔥");
        assertThat(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", false)
                        .mention())
                .isEqualTo("<:mindustry:41771983429993937>");
        assertThat(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", true)
                        .mention())
                .isEqualTo("<a:mindustry:41771983429993937>");
    }

    @Test
    void reactionPathIsUrlEncoded() {
        assertThat(Emoji.unicode("🔥").asReactionPath()).isEqualTo("%F0%9F%94%A5");
        assertThat(Emoji.unicode("✅").asReactionPath()).isEqualTo("%E2%9C%85");
        assertThat(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", false)
                        .asReactionPath())
                .isEqualTo("mindustry%3A41771983429993937");
    }
}
