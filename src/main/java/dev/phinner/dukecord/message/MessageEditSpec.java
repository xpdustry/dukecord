// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.component.Component;
import java.util.List;
import java.util.Optional;
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
        return builder().content(content).build();
    }

    Optional<String> content();

    Optional<List<Embed>> embeds();

    Optional<List<Component>> components();

    @Value.Default
    default AllowedMentions allowedMentions() {
        return AllowedMentions.none();
    }

    /** Only {@link MessageFlags#SUPPRESS_EMBEDS} and {@link MessageFlags#IS_COMPONENTS_V2} can change. */
    Optional<Integer> flags();

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
        Builder from(MessageEditSpec instance);

        Builder content(String content);

        Builder content(Optional<String> content);

        Builder embeds(List<Embed> embeds);

        Builder embeds(Optional<? extends List<Embed>> embeds);

        Builder components(List<Component> components);

        Builder components(Optional<? extends List<Component>> components);

        Builder allowedMentions(AllowedMentions allowedMentions);

        Builder flags(int flags);

        Builder flags(Optional<Integer> flags);

        Builder addFiles(FileUpload element);

        Builder addFiles(FileUpload... elements);

        Builder files(Iterable<? extends FileUpload> elements);

        Builder addAllFiles(Iterable<? extends FileUpload> elements);

        Builder retainedAttachments(List<Snowflake> retainedAttachments);

        Builder retainedAttachments(Optional<? extends List<Snowflake>> retainedAttachments);

        MessageEditSpec build();
    }
}
