// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.component.Component;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** The body of a new message. Mentions are disabled unless {@link #allowedMentions()} says otherwise. */
@Value.Immutable
@JsonSerialize(as = ImmutableMessageCreateSpec.class)
public interface MessageCreateSpec {

    static Builder builder() {
        return ImmutableMessageCreateSpec.builder();
    }

    static MessageCreateSpec of(final String content) {
        return ImmutableMessageCreateSpec.of(Optional.of(content), Set.of());
    }

    /** Only for interaction replies and follow-ups. */
    static MessageCreateSpec ephemeral(final String content) {
        return ImmutableMessageCreateSpec.of(Optional.of(content), Set.of(MessageFlag.EPHEMERAL));
    }

    @Value.Parameter
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Value.Parameter
    Set<MessageFlag> flags();

    interface Builder {
        Builder from(final MessageCreateSpec instance);

        Builder content(final String content);

        Builder addEmbeds(final Embed element);

        Builder addEmbeds(final Embed... elements);

        Builder embeds(final Iterable<? extends Embed> elements);

        Builder addAllEmbeds(final Iterable<? extends Embed> elements);

        Builder addComponents(final Component element);

        Builder addComponents(final Component... elements);

        Builder components(final Iterable<? extends Component> elements);

        Builder addAllComponents(final Iterable<? extends Component> elements);

        Builder addFiles(final FileUpload element);

        Builder addFiles(final FileUpload... elements);

        Builder files(final Iterable<? extends FileUpload> elements);

        Builder addAllFiles(final Iterable<? extends FileUpload> elements);

        Builder allowedMentions(final AllowedMentions allowedMentions);

        Builder messageReference(final MessageReference messageReference);

        Builder tts(final boolean tts);

        Builder addFlags(final MessageFlag element);

        Builder addFlags(final MessageFlag... elements);

        Builder flags(final Iterable<MessageFlag> elements);

        Builder addAllFlags(final Iterable<MessageFlag> elements);

        MessageCreateSpec build();
    }
}
