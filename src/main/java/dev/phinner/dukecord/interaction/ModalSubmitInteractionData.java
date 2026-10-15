// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.component.ActionRow;
import dev.phinner.dukecord.component.Component;
import dev.phinner.dukecord.component.EntitySelectMenu;
import dev.phinner.dukecord.component.Label;
import dev.phinner.dukecord.component.StringSelectMenu;
import dev.phinner.dukecord.component.TextInput;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableModalSubmitInteractionData.class)
public interface ModalSubmitInteractionData {

    String customId();

    /** The modal's components holding only their custom IDs and submitted values. */
    List<Component> components();

    @Value.Default
    default Resolved resolved() {
        return Resolved.empty();
    }

    /** What the user typed in a text input. */
    default Optional<String> value(final String customId) {
        return inputs(this.components())
                .flatMap(component ->
                        component instanceof TextInput input && input.customId().equals(customId)
                                ? input.value().stream()
                                : Stream.empty())
                .findFirst();
    }

    /** What the user picked in a select menu, IDs for entity selects. */
    default List<String> values(final String customId) {
        return inputs(this.components())
                .flatMap(component -> switch (component) {
                    case StringSelectMenu menu when menu.customId().equals(customId) -> menu.values().stream();
                    case EntitySelectMenu menu
                    when menu.customId().equals(customId) ->
                        menu.values().stream().map(Snowflake::asString);
                    default -> Stream.empty();
                })
                .toList();
    }

    private static Stream<Component> inputs(final List<Component> components) {
        return components.stream().flatMap(component -> switch (component) {
            case ActionRow row -> inputs(row.components());
            case Label label -> Stream.of(label.component());
            default -> Stream.of(component);
        });
    }
}
