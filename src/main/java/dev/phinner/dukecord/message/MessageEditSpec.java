// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.component.Component;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Changes to a message. Absent attributes are left as they are; an empty value clears, such as {@code content("")} or
 * {@code embeds(List.of())}. Mentions in new content are disabled unless {@link #allowedMentions()} says otherwise.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMessageEditSpec.class)
public interface MessageEditSpec {

    static Builder builder() {
        return ImmutableMessageEditSpec.builder();
    }

    static MessageEditSpec of(final String content) {
        return ImmutableMessageEditSpec.of(Optional.of(content));
    }

    @Value.Parameter
    Optional<String> content();

    Optional<List<Embed>> embeds();

    Optional<List<Component>> components();

    @Value.Default
    default AllowedMentions allowedMentions() {
        return AllowedMentions.none();
    }

    /** Replaces the flags. Only {@link MessageFlag#SUPPRESS_EMBEDS} and {@link MessageFlag#IS_COMPONENTS_V2} can change. */
    Optional<Set<MessageFlag>> flags();

    /** Added to the message. */
    @JsonIgnore
    List<FileUpload> files();

    /**
     * The existing attachments to keep; the others are removed. When absent, every existing attachment is kept, but
     * {@link #files()} are then sent without their descriptions and spoiler state.
     */
    @JsonIgnore
    Optional<List<Snowflake>> retainedAttachments();

    interface Builder {
        Builder from(final MessageEditSpec instance);

        Builder content(final String content);

        Builder embeds(final List<Embed> embeds);

        Builder components(final List<Component> components);

        Builder allowedMentions(final AllowedMentions allowedMentions);

        Builder flags(final Set<MessageFlag> flags);

        Builder addFiles(final FileUpload element);

        Builder addFiles(final FileUpload... elements);

        Builder files(final Iterable<? extends FileUpload> elements);

        Builder addAllFiles(final Iterable<? extends FileUpload> elements);

        Builder retainedAttachments(final List<Snowflake> retainedAttachments);

        MessageEditSpec build();
    }
}
