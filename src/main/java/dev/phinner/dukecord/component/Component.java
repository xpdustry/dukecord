// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Optional;

/**
 * A part of a message or modal layout. Unknown types decode as {@link UnknownComponent}. Messages using any layout
 * component other than {@link ActionRow} need the {@code IS_COMPONENTS_V2} flag.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ImmutableUnknownComponent.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ImmutableActionRow.class, name = "1"),
    @JsonSubTypes.Type(value = ImmutableButton.class, name = "2"),
    @JsonSubTypes.Type(value = ImmutableStringSelectMenu.class, name = "3"),
    @JsonSubTypes.Type(value = ImmutableTextInput.class, name = "4"),
    @JsonSubTypes.Type(
            value = ImmutableEntitySelectMenu.class,
            names = {"5", "6", "7", "8"}),
    @JsonSubTypes.Type(value = ImmutableSection.class, name = "9"),
    @JsonSubTypes.Type(value = ImmutableTextDisplay.class, name = "10"),
    @JsonSubTypes.Type(value = ImmutableThumbnail.class, name = "11"),
    @JsonSubTypes.Type(value = ImmutableMediaGallery.class, name = "12"),
    @JsonSubTypes.Type(value = ImmutableFileDisplay.class, name = "13"),
    @JsonSubTypes.Type(value = ImmutableSeparator.class, name = "14"),
    @JsonSubTypes.Type(value = ImmutableContainer.class, name = "17"),
    @JsonSubTypes.Type(value = ImmutableLabel.class, name = "18")
})
public sealed interface Component
        permits ActionRow,
                Button,
                Container,
                EntitySelectMenu,
                FileDisplay,
                Label,
                MediaGallery,
                Section,
                Separator,
                StringSelectMenu,
                TextDisplay,
                TextInput,
                Thumbnail,
                UnknownComponent {

    int type();

    /**
     * Disables or enables this component and the interactive components inside it, such as the buttons of an action
     * row. Other components are returned as they are.
     */
    default Component withDisabled(final boolean disabled) {
        return this;
    }

    /** Identifies the component within its message. Discord numbers the components sent without one. */
    Optional<Integer> id();
}
