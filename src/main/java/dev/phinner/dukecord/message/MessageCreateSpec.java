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
        return builder().content(content).build();
    }

    /** Only for interaction replies and follow-ups. */
    static MessageCreateSpec ephemeral(final String content) {
        return builder().content(content).addFlags(MessageFlag.EPHEMERAL).build();
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Set<MessageFlag> flags();

    interface Builder {
        Builder from(MessageCreateSpec instance);

        Builder content(String content);

        Builder content(Optional<String> content);

        Builder addEmbeds(Embed element);

        Builder addEmbeds(Embed... elements);

        Builder embeds(Iterable<? extends Embed> elements);

        Builder addAllEmbeds(Iterable<? extends Embed> elements);

        Builder addComponents(Component element);

        Builder addComponents(Component... elements);

        Builder components(Iterable<? extends Component> elements);

        Builder addAllComponents(Iterable<? extends Component> elements);

        Builder addFiles(FileUpload element);

        Builder addFiles(FileUpload... elements);

        Builder files(Iterable<? extends FileUpload> elements);

        Builder addAllFiles(Iterable<? extends FileUpload> elements);

        Builder allowedMentions(AllowedMentions allowedMentions);

        Builder messageReference(MessageReference messageReference);

        Builder messageReference(Optional<? extends MessageReference> messageReference);

        Builder tts(boolean tts);

        Builder addFlags(MessageFlag element);

        Builder addFlags(MessageFlag... elements);

        Builder flags(Iterable<MessageFlag> elements);

        Builder addAllFlags(Iterable<MessageFlag> elements);

        MessageCreateSpec build();
    }
}
