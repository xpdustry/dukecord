// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** The user is typing an option that has autocomplete. */
@Value.Immutable
@JsonDeserialize(as = ImmutableAutocompleteInteractionEvent.class)
public non-sealed interface AutocompleteInteractionEvent extends InteractionEvent {

    /** Holds the options typed so far, which are not validated yet. */
    CommandInteractionData data();

    default Optional<CommandInteractionData.Option> focusedOption() {
        return this.data().leafOptions().stream()
                .filter(CommandInteractionData.Option::focused)
                .findFirst();
    }

    /** Takes up to 25 choices. */
    default void suggest(final List<CommandOption.Choice> choices) {
        this.duke().interactions().rest().suggest(this.id(), this.token(), choices);
    }
}
