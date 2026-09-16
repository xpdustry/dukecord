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
 * The body of a new message. Detached from any target: the same value can be sent to a channel, used as a reply,
 * or given to an interaction, whose consumer applies its own envelope and allow-list. Mentions default to
 * {@link AllowedMentions#none()}.
 */
public final class MessageCreateSpec implements Spec {

    private final Json payload;
    private final List<FileUpload> files;
    private final Optional<String> reason;

    private MessageCreateSpec(final Json payload, final List<FileUpload> files, final Optional<String> reason) {
        this.payload = payload;
        this.files = List.copyOf(files);
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled with the keys of an existing spec. */
    public static Builder builder(final MessageCreateSpec copy) {
        final var builder = new Builder();
        builder.payload = copy.payload;
        builder.files.addAll(copy.files);
        builder.reason = copy.reason;
        return builder;
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
        return "MessageCreateSpec" + this.payload;
    }

    public static final class Builder {

        private Json payload =
                Json.empty().with("allowed_mentions", AllowedMentions.none().toJson());
        private final List<FileUpload> files = new ArrayList<>();
        private Optional<String> reason = Optional.empty();

        private Builder() {}

        public Builder content(final String content) {
            this.payload = this.payload.with("content", Objects.requireNonNull(content, "content"));
            return this;
        }

        public Builder embed(final Embed embed) {
            return this.embeds(List.of(embed));
        }

        /** Appends to the embeds already added. */
        public Builder embeds(final Collection<Embed> embeds) {
            final List<Object> all = new ArrayList<>(this.payload.list("embeds"));
            embeds.forEach(embed -> all.add(embed.toJson()));
            this.payload = this.payload.with("embeds", all);
            return this;
        }

        public Builder component(final Component component) {
            return this.components(List.of(component));
        }

        /** Appends to the components already added. Top-level components are usually {@link ActionRow}s. */
        public Builder components(final Collection<Component> components) {
            final List<Object> all = new ArrayList<>(this.payload.list("components"));
            components.forEach(component -> all.add(component.toJson()));
            this.payload = this.payload.with("components", all);
            return this;
        }

        public Builder file(final FileUpload file) {
            this.files.add(Objects.requireNonNull(file, "file"));
            return this;
        }

        public Builder allowedMentions(final AllowedMentions mentions) {
            this.payload = this.payload.with("allowed_mentions", mentions.toJson());
            return this;
        }

        public Builder flags(final MessageFlag... flags) {
            return this.flag(MessageFlag.toBits(List.of(flags)));
        }

        /** Only honoured on interaction replies and follow-ups; a no-op on channel sends. */
        public Builder ephemeral() {
            return this.flag(MessageFlag.EPHEMERAL.mask());
        }

        public Builder suppressNotifications() {
            return this.flag(MessageFlag.SUPPRESS_NOTIFICATIONS.mask());
        }

        public Builder suppressEmbeds() {
            return this.flag(MessageFlag.SUPPRESS_EMBEDS.mask());
        }

        /** Marks the message as a Components V2 layout; content and embeds are then rejected by Discord. */
        public Builder componentsV2() {
            return this.flag(MessageFlag.IS_COMPONENTS_V2.mask());
        }

        private Builder flag(final int bits) {
            final int current = this.payload.optionalInteger("flags").orElse(0);
            this.payload = this.payload.with("flags", current | bits);
            return this;
        }

        public Builder tts(final boolean tts) {
            this.payload = this.payload.with("tts", tts);
            return this;
        }

        public Builder nonce(final String nonce) {
            this.payload = this.payload.with("nonce", nonce);
            return this;
        }

        public Builder enforceNonce(final boolean enforce) {
            this.payload = this.payload.with("enforce_nonce", enforce);
            return this;
        }

        /** Makes the message a reply. {@code failIfNotExists} false sends a plain message if the target is gone. */
        public Builder reference(final Snowflake messageId, final Snowflake channelId, final boolean failIfNotExists) {
            this.payload = this.payload.with(
                    "message_reference",
                    Json.empty()
                            .with("message_id", messageId.asString())
                            .with("channel_id", channelId.asString())
                            .with("fail_if_not_exists", failIfNotExists));
            return this;
        }

        public Builder stickers(final Collection<Snowflake> stickerIds) {
            this.payload = this.payload.with(
                    "sticker_ids", stickerIds.stream().map(Snowflake::asString).toList());
            return this;
        }

        public Builder reason(final @Nullable String reason) {
            this.reason = Optional.ofNullable(reason);
            return this;
        }

        public MessageCreateSpec build() {
            var payload = this.payload;
            if (!this.files.isEmpty()) {
                payload = payload.with("attachments", placeholders(this.files));
            }
            return new MessageCreateSpec(payload, this.files, this.reason);
        }
    }

    /** The {@code attachments} placeholders that name uploaded files by their multipart index. */
    static List<Json> placeholders(final List<FileUpload> files) {
        final List<Json> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            var placeholder =
                    Json.empty().with("id", i).with("filename", files.get(i).filename());
            if (files.get(i).description().isPresent()) {
                placeholder = placeholder.with(
                        "description", files.get(i).description().get());
            }
            result.add(placeholder);
        }
        return result;
    }
}
