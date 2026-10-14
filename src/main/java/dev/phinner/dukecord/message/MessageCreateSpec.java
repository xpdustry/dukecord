// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

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

    Optional<String> content();

    @Value.Default
    default AllowedMentions allowedMentions() {
        return AllowedMentions.none();
    }

    Optional<MessageReference> messageReference();

    @Value.Default
    default boolean tts() {
        return false;
    }

    Optional<Integer> flags();
}
