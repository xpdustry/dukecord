// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.Spec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The body of a message edit. A key that was never set is absent and Discord keeps the current value; the
 * {@code clear*} methods write {@code null}. The {@code attachments} key is written only by {@link Builder#keepAttachments},
 * {@link Builder#removeAttachments} or a file upload: absent keeps every existing attachment.
 */
public final class MessageEditSpec implements Spec {

    private final Json payload;
    private final List<FileUpload> files;
    private final Optional<String> reason;

    private MessageEditSpec(final Json payload, final List<FileUpload> files, final Optional<String> reason) {
        this.payload = payload;
        this.files = List.copyOf(files);
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Copies the keys an edit accepts (content, embeds, components, allowed mentions, suppress-embeds flag, files). */
    public static MessageEditSpec from(final MessageCreateSpec create) {
        final var builder = new Builder();
        var payload = Json.empty();
        for (final var key : List.of("content", "embeds", "components", "allowed_mentions")) {
            if (create.payload().has(key)) {
                payload = payload.with(key, create.payload().raw(key));
            }
        }
        final int flags = create.payload().optionalInteger("flags").orElse(0) & MessageFlag.SUPPRESS_EMBEDS.mask();
        if (flags != 0) {
            payload = payload.with("flags", flags);
        }
        builder.payload = payload;
        builder.files.addAll(create.files());
        builder.reason = create.reason();
        return builder.build();
    }

    /** The teardown edit for an expired interactive message: every component disabled, nothing else touched. */
    public static MessageEditSpec disablingComponentsOf(final Message message) {
        return builder()
                .components(
                        message.components().stream().map(Component::asDisabled).toList())
                .build();
    }

    @Override
    public Json payload() {
        return this.payload;
    }

    public List<FileUpload> files() {
        return this.files;
    }

    @Override
    public Optional<String> reason() {
        return this.reason;
    }

    @Override
    public String toString() {
        return "MessageEditSpec" + this.payload;
    }

    public static final class Builder {

        private Json payload = Json.empty();
        private final List<FileUpload> files = new ArrayList<>();
        private @Nullable List<Snowflake> keptAttachments;
        private Optional<String> reason = Optional.empty();

        private Builder() {}

        public Builder content(final String content) {
            this.payload = this.payload.with("content", Objects.requireNonNull(content, "content"));
            return this;
        }

        public Builder clearContent() {
            this.payload = this.payload.with("content", null);
            return this;
        }

        /** Replaces the embeds. */
        public Builder embeds(final Collection<Embed> embeds) {
            this.payload = this.payload.with(
                    "embeds", embeds.stream().map(Embed::toJson).toList());
            return this;
        }

        public Builder embed(final Embed embed) {
            return this.embeds(List.of(embed));
        }

        public Builder clearEmbeds() {
            this.payload = this.payload.with("embeds", List.of());
            return this;
        }

        /** Replaces the components. */
        public Builder components(final Collection<Component> components) {
            this.payload = this.payload.with(
                    "components", components.stream().map(Component::toJson).toList());
            return this;
        }

        public Builder clearComponents() {
            this.payload = this.payload.with("components", List.of());
            return this;
        }

        public Builder file(final FileUpload file) {
            this.files.add(Objects.requireNonNull(file, "file"));
            return this;
        }

        /** Keeps exactly these existing attachments (plus any new uploads); every other one is removed. */
        public Builder keepAttachments(final Collection<Snowflake> attachmentIds) {
            this.keptAttachments = List.copyOf(attachmentIds);
            return this;
        }

        /** Removes every existing attachment (new uploads are still added). */
        public Builder removeAttachments() {
            this.keptAttachments = List.of();
            return this;
        }

        public Builder allowedMentions(final AllowedMentions mentions) {
            this.payload = this.payload.with("allowed_mentions", mentions.toJson());
            return this;
        }

        /** The only flag an edit can change. */
        public Builder suppressEmbeds(final boolean suppress) {
            this.payload = this.payload.with("flags", suppress ? MessageFlag.SUPPRESS_EMBEDS.mask() : 0);
            return this;
        }

        public Builder reason(final @Nullable String reason) {
            this.reason = Optional.ofNullable(reason);
            return this;
        }

        public MessageEditSpec build() {
            var payload = this.payload;
            if (this.keptAttachments != null) {
                // A keep-list: exactly these ids survive, plus the new uploads.
                final List<Json> attachments = new ArrayList<>();
                for (final var id : this.keptAttachments) {
                    attachments.add(Json.empty().with("id", id.asString()));
                }
                attachments.addAll(MessageCreateSpec.placeholders(this.files));
                payload = payload.with("attachments", attachments);
            } else if (this.files.stream().anyMatch(file -> file.description().isPresent())) {
                // No keep-list: Discord keeps the existing attachments when the key is absent. Placeholders are
                // only needed to carry descriptions, and sending them alone would drop the existing ones.
                payload = payload.with("attachments", MessageCreateSpec.placeholders(this.files));
            }
            if (!payload.has("allowed_mentions") && payload.has("content")) {
                // Discord re-parses mentions with default allowances on any content edit that omits the key.
                payload =
                        payload.with("allowed_mentions", AllowedMentions.none().toJson());
            }
            return new MessageEditSpec(payload, this.files, this.reason);
        }
    }
}
