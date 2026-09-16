// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentTest {

    private static final String ACTION_ROW = """
            {
              "type": 1,
              "components": [
                {"type": 2, "style": 1, "custom_id": "page:next", "label": "Next", "disabled": false},
                {"type": 2, "style": 5, "url": "https://xpdustry.com", "label": "Website", "disabled": false}
              ]
            }""";

    private static final String STRING_SELECT = """
            {
              "type": 3,
              "custom_id": "map:pick",
              "min_values": 1,
              "max_values": 3,
              "disabled": false,
              "options": [
                {"label": "Fork", "value": "fork", "default": false},
                {"label": "Glacier", "value": "glacier", "default": true, "description": "Cold",
                 "emoji": {"name": "mindustry", "id": "41771983429993937", "animated": false}}
              ],
              "placeholder": "Pick a map"
            }""";

    // ---- type codes ----

    @Test
    void typeCodesMatchDiscord() {
        assertThat(Component.TYPE_ACTION_ROW).isEqualTo(1);
        assertThat(Component.TYPE_BUTTON).isEqualTo(2);
        assertThat(Component.TYPE_STRING_SELECT).isEqualTo(3);
    }

    // ---- buttons ----

    @Test
    void buttonFactoriesPickTheRightStyle() {
        assertThat(Button.primary("a", "A").style()).isEqualTo(Button.Style.PRIMARY);
        assertThat(Button.secondary("a", "A").style()).isEqualTo(Button.Style.SECONDARY);
        assertThat(Button.success("a", "A").style()).isEqualTo(Button.Style.SUCCESS);
        assertThat(Button.danger("a", "A").style()).isEqualTo(Button.Style.DANGER);

        final var button = Button.primary("page:next", "Next");
        assertThat(button.customId()).contains("page:next");
        assertThat(button.label()).contains("Next");
        assertThat(button.url()).isEmpty();
        assertThat(button.emoji()).isEmpty();
        assertThat(button.disabled()).isFalse();
        assertThat(button.type()).isEqualTo(Component.TYPE_BUTTON);
    }

    @Test
    void linkButtonsCarryAUrlInsteadOfACustomId() {
        final var button = Button.link("https://xpdustry.com", "Website");
        assertThat(button.style()).isEqualTo(Button.Style.LINK);
        assertThat(button.url()).contains("https://xpdustry.com");
        assertThat(button.customId()).isEmpty();
    }

    @Test
    void withDisabledAndWithEmojiReturnCopies() {
        final var button = Button.primary("page:next", "Next");
        final var emoji = Emoji.unicode("➡");

        assertThat(button.withDisabled(true).disabled()).isTrue();
        assertThat(button.disabled()).isFalse();
        assertThat(button.withEmoji(emoji).emoji()).contains(emoji);
        assertThat(button.emoji()).isEmpty();
        assertThat(button.withDisabled(false)).isEqualTo(button);
    }

    @Test
    void buttonStyleCodesRoundTripAndFallBackToSecondary() {
        assertThat(Button.Style.PRIMARY.code()).isEqualTo(1);
        assertThat(Button.Style.LINK.code()).isEqualTo(5);
        assertThat(Button.Style.PREMIUM.code()).isEqualTo(6);
        for (final var style : Button.Style.values()) {
            assertThat(Button.Style.fromCode(style.code())).isEqualTo(style);
        }
        assertThat(Button.Style.fromCode(99)).isEqualTo(Button.Style.SECONDARY);
        assertThat(Button.Style.fromCode(0)).isEqualTo(Button.Style.SECONDARY);
    }

    @Test
    void buttonRoundTripsThroughJson() {
        final var json = Json.parse("""
                {"type": 2, "style": 4, "disabled": true, "custom_id": "confirm",
                 "label": "Delete", "emoji": {"name": "mindustry", "id": "41771983429993937", "animated": false}}""");
        final var component = Component.fromJson(json);
        assertThat(component).isInstanceOf(Button.class);
        final var button = (Button) component;
        assertThat(button.style()).isEqualTo(Button.Style.DANGER);
        assertThat(button.disabled()).isTrue();
        assertThat(button.emoji()).contains(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", false));
        assertThat(button.toJson()).isEqualTo(json);
    }

    @Test
    void buttonToJsonOmitsTheKeysItHasNoValueFor() {
        assertThat(Button.primary("id", "Label").toJson()).isEqualTo(Json.parse("""
                        {"type": 2, "style": 1, "disabled": false, "custom_id": "id", "label": "Label"}"""));
        assertThat(Button.link("https://xpdustry.com", "Website").toJson().has("custom_id"))
                .isFalse();
    }

    // ---- select ----

    @Test
    void stringSelectBuilderDefaultsToASingleChoice() {
        final var select = StringSelect.builder("map:pick")
                .option("Fork", "fork")
                .option(StringSelect.Option.of("Glacier", "glacier")
                        .withDescription("Cold")
                        .withDefault(true))
                .build();
        assertThat(select.customId()).isEqualTo("map:pick");
        assertThat(select.placeholder()).isEmpty();
        assertThat(select.minValues()).isEqualTo(1);
        assertThat(select.maxValues()).isEqualTo(1);
        assertThat(select.disabled()).isFalse();
        assertThat(select.type()).isEqualTo(Component.TYPE_STRING_SELECT);
        assertThat(select.options()).hasSize(2);
        assertThat(select.options().getLast().isDefault()).isTrue();
        assertThat(select.options().getLast().description()).contains("Cold");
    }

    @Test
    void stringSelectBuilderSetsPlaceholderAndRange() {
        final var select = StringSelect.builder("map:pick")
                .placeholder("Pick a map")
                .range(1, 3)
                .option("Fork", "fork")
                .build();
        assertThat(select.placeholder()).contains("Pick a map");
        assertThat(select.minValues()).isEqualTo(1);
        assertThat(select.maxValues()).isEqualTo(3);
    }

    @Test
    void stringSelectOptionCopiesAreIndependent() {
        final var option = StringSelect.Option.of("Fork", "fork");
        final var emoji = Emoji.unicode("🗺");
        assertThat(option.withEmoji(emoji).emoji()).contains(emoji);
        assertThat(option.withDescription("A map").description()).contains("A map");
        assertThat(option.withDefault(true).isDefault()).isTrue();
        assertThat(option)
                .isEqualTo(new StringSelect.Option("Fork", "fork", Optional.empty(), Optional.empty(), false));
    }

    @Test
    void stringSelectRoundTripsThroughJson() {
        final var json = Json.parse(STRING_SELECT);
        final var component = Component.fromJson(json);
        assertThat(component).isInstanceOf(StringSelect.class);
        final var select = (StringSelect) component;
        assertThat(select.customId()).isEqualTo("map:pick");
        assertThat(select.maxValues()).isEqualTo(3);
        assertThat(select.placeholder()).contains("Pick a map");
        assertThat(select.options()).hasSize(2);
        assertThat(select.options().getLast().emoji())
                .contains(Emoji.custom(Snowflake.of("41771983429993937"), "mindustry", false));
        assertThat(select.toJson()).isEqualTo(json);
    }

    @Test
    void stringSelectDefaultsMinAndMaxValuesWhenDiscordOmitsThem() {
        final var select = (StringSelect) Component.fromJson(Json.parse("""
                {"type": 3, "custom_id": "map:pick", "options": [{"label": "Fork", "value": "fork"}]}"""));
        assertThat(select.minValues()).isEqualTo(1);
        assertThat(select.maxValues()).isEqualTo(1);
        assertThat(select.disabled()).isFalse();
        assertThat(select.placeholder()).isEmpty();
    }

    // ---- action row ----

    @Test
    void actionRowRoundTripsThroughJson() {
        final var json = Json.parse(ACTION_ROW);
        final var component = Component.fromJson(json);
        assertThat(component).isInstanceOf(ActionRow.class);
        final var row = (ActionRow) component;
        assertThat(row.type()).isEqualTo(Component.TYPE_ACTION_ROW);
        assertThat(row.components()).hasSize(2);
        assertThat(row.components().getFirst()).isInstanceOf(Button.class);
        assertThat(row.toJson()).isEqualTo(json);
    }

    @Test
    void actionRowHoldsBetweenOneAndFiveComponents() {
        assertThatThrownBy(ActionRow::of)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An action row holds 1 to 5 components");
        assertThatThrownBy(() -> ActionRow.of(
                        Button.primary("1", "1"),
                        Button.primary("2", "2"),
                        Button.primary("3", "3"),
                        Button.primary("4", "4"),
                        Button.primary("5", "5"),
                        Button.primary("6", "6")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An action row holds 1 to 5 components");
        assertThat(ActionRow.of(Button.primary("1", "1")).components()).hasSize(1);
        assertThat(ActionRow.of(
                                Button.primary("1", "1"),
                                Button.primary("2", "2"),
                                Button.primary("3", "3"),
                                Button.primary("4", "4"),
                                Button.primary("5", "5"))
                        .components())
                .hasSize(5);
    }

    @Test
    void anActionRowWithInvalidChildCountFromDiscordRoundTripsAsUnknown() {
        // Rule 8: a payload Discord sends must never crash a bot, so an invalid row is preserved untouched.
        final var empty = Json.parse("{\"type\": 1, \"components\": []}");
        assertThat(Component.fromJson(empty)).isInstanceOfSatisfying(UnknownComponent.class, unknown -> {
            assertThat(unknown.raw()).isEqualTo(empty);
            assertThat(unknown.type()).isEqualTo(Component.TYPE_ACTION_ROW);
        });
        assertThat(Component.fromJson(Json.parse("{\"type\": 1}"))).isInstanceOf(UnknownComponent.class);
        // Building one in code is still a programming error.
        assertThatThrownBy(() -> ActionRow.of())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An action row holds 1 to 5 components");
    }

    @Test
    void actionRowComponentsAreUnmodifiable() {
        final var row = ActionRow.of(Button.primary("1", "1"));
        assertThatThrownBy(() -> row.components().add(Button.primary("2", "2")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- unknown ----

    @Test
    void unknownComponentTypesRoundTripUntouched() {
        final var json = Json.parse("""
                {"type": 17, "accent_color": 123, "components": [{"type": 10, "content": "hello"}]}""");
        final var component = Component.fromJson(json);
        assertThat(component).isInstanceOf(UnknownComponent.class);
        assertThat(component.type()).isEqualTo(17);
        assertThat(component.toJson()).isEqualTo(json);
        assertThat(((UnknownComponent) component).raw()).isEqualTo(json);
    }

    @Test
    void unknownComponentWithoutATypeReportsMinusOne() {
        assertThat(new UnknownComponent(Json.empty()).type()).isEqualTo(-1);
    }

    // ---- disabling ----

    @Test
    void asDisabledDisablesButtonsAndSelects() {
        assertThat(Button.primary("a", "A").asDisabled().disabled()).isTrue();
        assertThat(Button.primary("a", "A").withDisabled(true).asDisabled().disabled())
                .isTrue();
        assertThat(StringSelect.builder("s")
                        .option("a", "1")
                        .build()
                        .asDisabled()
                        .disabled())
                .isTrue();
    }

    @Test
    void asDisabledOnAnUnknownComponentReturnsItself() {
        final var unknown = new UnknownComponent(Json.parse("{\"type\": 17}"));
        assertThat(unknown.asDisabled()).isSameAs(unknown);
    }

    @Test
    void asDisabledRecursesThroughActionRows() {
        final var row = ActionRow.of(
                Button.primary("a", "A"),
                StringSelect.builder("s").option("a", "1").build(),
                new UnknownComponent(Json.parse("{\"type\": 17}")));
        final var disabled = row.asDisabled();

        assertThat(((Button) disabled.components().get(0)).disabled()).isTrue();
        assertThat(((StringSelect) disabled.components().get(1)).disabled()).isTrue();
        assertThat(disabled.components().get(2)).isEqualTo(row.components().get(2));
        assertThat(((Button) row.components().get(0)).disabled()).isFalse();
    }

    // ---- custom ids ----

    @Test
    void customIdOfReadsEveryComponentKind() {
        assertThat(Component.customIdOf(Button.primary("page:next", "Next"))).contains("page:next");
        assertThat(Component.customIdOf(Button.link("https://xpdustry.com", "Website")))
                .isEmpty();
        assertThat(Component.customIdOf(
                        StringSelect.builder("map:pick").option("a", "1").build()))
                .contains("map:pick");
        assertThat(Component.customIdOf(ActionRow.of(Button.primary("a", "A")))).isEmpty();
        assertThat(Component.customIdOf(new UnknownComponent(Json.parse("""
                        {"type": 18, "custom_id": "modal:submit"}""")))).contains("modal:submit");
        assertThat(Component.customIdOf(new UnknownComponent(Json.parse("{\"type\": 18}"))))
                .isEmpty();
    }

    @Test
    void fromJsonHandlesNestedRows() {
        final var row = (ActionRow) Component.fromJson(Json.parse(ACTION_ROW));
        assertThat(row.components().stream().map(Component::type).toList()).isEqualTo(List.of(2, 2));
    }
}
