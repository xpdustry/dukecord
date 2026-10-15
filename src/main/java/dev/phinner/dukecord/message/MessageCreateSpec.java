// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.component.Component;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** The body of a new message. Mentions are disabled unless {@link #allowedMentions()} says otherwise. */
@Value.Immutable
@JsonSerialize(as = ImmutableMessageCreateSpec.class)
public interface MessageCreateSpec {

    static ImmutableMessageCreateSpec.Builder builder() {
        return ImmutableMessageCreateSpec.builder();
    }

    static MessageCreateSpec of(final String content) {
        return builder().content(content).build();
    }

    /** Only for interaction replies and follow-ups. */
    static MessageCreateSpec ephemeral(final String content) {
        return builder().content(content).flags(MessageFlags.EPHEMERAL).build();
    }

    Optional<String> content();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Embed> embeds();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Component> components();

    @JsonIgnore
    List<FileUpload> files();

    @Value.Default
    default AllowedMentions allowedMentions() {
        return AllowedMentions.none();
    }

    Optional<MessageReference> messageReference();

    @Value.Default
    default boolean tts() {
        return false;
    }

    /** See {@link MessageFlags}. */
    Optional<Integer> flags();
}
