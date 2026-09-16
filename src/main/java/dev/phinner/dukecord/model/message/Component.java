// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A message component. Values, not entities. Unknown types (including the v2 layout components until they are
 * modelled) round-trip through {@link UnknownComponent} untouched.
 */
public sealed interface Component permits ActionRow, Button, StringSelect, UnknownComponent {

    int TYPE_ACTION_ROW = 1;
    int TYPE_BUTTON = 2;
    int TYPE_STRING_SELECT = 3;

    int type();

    Json toJson();

    /** A copy with every interactive control disabled. */
    Component asDisabled();

    static Component fromJson(final Json json) {
        return switch (json.integer("type")) {
            case TYPE_ACTION_ROW -> {
                final List<Component> children = new ArrayList<>();
                for (final var child : json.objects("components")) {
                    children.add(fromJson(child));
                }
                // Discord must never crash a bot: a row that violates our invariants round-trips untouched.
                yield children.isEmpty() || children.size() > 5 ? new UnknownComponent(json) : new ActionRow(children);
            }
            case TYPE_BUTTON ->
                new Button(
                        Button.Style.fromCode(json.integer("style")),
                        json.optionalString("custom_id"),
                        json.optionalString("label"),
                        json.optionalObject("emoji").map(Emoji::fromJson),
                        json.optionalString("url"),
                        json.bool("disabled", false));
            case TYPE_STRING_SELECT -> {
                final List<StringSelect.Option> options = new ArrayList<>();
                for (final var option : json.objects("options")) {
                    options.add(new StringSelect.Option(
                            option.string("label"),
                            option.string("value"),
                            option.optionalString("description"),
                            option.optionalObject("emoji").map(Emoji::fromJson),
                            option.bool("default", false)));
                }
                yield new StringSelect(
                        json.string("custom_id"),
                        json.optionalString("placeholder"),
                        json.optionalInteger("min_values").orElse(1),
                        json.optionalInteger("max_values").orElse(1),
                        options,
                        json.bool("disabled", false));
            }
            default -> new UnknownComponent(json);
        };
    }

    static Optional<String> customIdOf(final Component component) {
        return switch (component) {
            case Button button -> button.customId();
            case StringSelect select -> Optional.of(select.customId());
            case ActionRow ignored -> Optional.empty();
            case UnknownComponent unknown -> unknown.raw().optionalString("custom_id");
        };
    }
}
