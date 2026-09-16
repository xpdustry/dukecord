// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageCreateSpecTest {

    private static final Snowflake CHANNEL_ID = Snowflake.of("41771983423143937");
    private static final Snowflake MESSAGE_ID = Snowflake.of("175928847299117063");

    private static FileUpload file(final String name) {
        return FileUpload.of("data".getBytes(StandardCharsets.UTF_8), name);
    }

    @Test
    void anEmptyBuilderOnlySilencesMentions() {
        final var spec = MessageCreateSpec.builder().build();
        assertThat(spec.payload())
                .isEqualTo(Json.empty()
                        .with("allowed_mentions", AllowedMentions.none().toJson()));
        assertThat(spec.files()).isEmpty();
        assertThat(spec.reason()).isEmpty();
    }

    @Test
    void allowedMentionsOverridesTheDefault() {
        final var spec = MessageCreateSpec.builder()
                .content("@everyone")
                .allowedMentions(AllowedMentions.all())
                .build();
        assertThat(spec.payload().object("allowed_mentions"))
                .isEqualTo(AllowedMentions.all().toJson());
    }

    @Test
    void contentIsSetAndReplaced() {
        final var spec =
                MessageCreateSpec.builder().content("first").content("second").build();
        assertThat(spec.payload().string("content")).isEqualTo("second");
    }

    @Test
    void embedsAppendAcrossCalls() {
        final var first = Embed.builder().title("one").build();
        final var second = Embed.builder().title("two").build();
        final var third = Embed.builder().title("three").build();

        final var spec = MessageCreateSpec.builder()
                .embed(first)
                .embeds(List.of(second, third))
                .build();

        assertThat(spec.payload().objects("embeds")).containsExactly(first.toJson(), second.toJson(), third.toJson());
    }

    @Test
    void componentsAppendAcrossCalls() {
        final var row = ActionRow.of(Button.primary("a", "A"));
        final var other = ActionRow.of(Button.danger("b", "B"));

        final var spec = MessageCreateSpec.builder()
                .component(row)
                .components(List.of(other))
                .build();

        assertThat(spec.payload().objects("components")).containsExactly(row.toJson(), other.toJson());
    }

    @Test
    void flagsAccumulateInsteadOfReplacing() {
        final var spec = MessageCreateSpec.builder()
                .ephemeral()
                .suppressNotifications()
                .suppressEmbeds()
                .componentsV2()
                .build();

        assertThat(MessageFlag.fromBits(spec.payload().integer("flags")))
                .containsExactlyInAnyOrder(
                        MessageFlag.EPHEMERAL,
                        MessageFlag.SUPPRESS_NOTIFICATIONS,
                        MessageFlag.SUPPRESS_EMBEDS,
                        MessageFlag.IS_COMPONENTS_V2);
    }

    @Test
    void explicitFlagsAreAccumulatedToo() {
        final var spec = MessageCreateSpec.builder()
                .flags(MessageFlag.SUPPRESS_EMBEDS)
                .flags(MessageFlag.EPHEMERAL, MessageFlag.URGENT)
                .build();
        assertThat(MessageFlag.fromBits(spec.payload().integer("flags")))
                .containsExactlyInAnyOrder(MessageFlag.SUPPRESS_EMBEDS, MessageFlag.EPHEMERAL, MessageFlag.URGENT);
        assertThat(MessageCreateSpec.builder().build().payload().has("flags")).isFalse();
    }

    @Test
    void referenceWritesDiscordsMessageReferenceObject() {
        final var spec = MessageCreateSpec.builder()
                .reference(MESSAGE_ID, CHANNEL_ID, false)
                .build();
        assertThat(spec.payload().object("message_reference")).isEqualTo(Json.parse("""
                        {"message_id": "175928847299117063", "channel_id": "41771983423143937",
                         "fail_if_not_exists": false}"""));
    }

    @Test
    void stickersAreWrittenAsStringIds() {
        final var spec = MessageCreateSpec.builder()
                .stickers(List.of(MESSAGE_ID, CHANNEL_ID))
                .build();
        assertThat(spec.payload().strings("sticker_ids")).containsExactly("175928847299117063", "41771983423143937");
    }

    @Test
    void scalarKeysAreWrittenOnlyWhenSet() {
        final var spec = MessageCreateSpec.builder()
                .tts(true)
                .nonce("abc")
                .enforceNonce(true)
                .reason("audit")
                .build();
        assertThat(spec.payload().bool("tts")).isTrue();
        assertThat(spec.payload().string("nonce")).isEqualTo("abc");
        assertThat(spec.payload().bool("enforce_nonce")).isTrue();
        assertThat(spec.reason()).contains("audit");

        final var bare = MessageCreateSpec.builder().build();
        assertThat(bare.payload().asMap()).containsOnlyKeys("allowed_mentions");
        assertThat(MessageCreateSpec.builder().reason(null).build().reason()).isEmpty();
    }

    @Test
    void filesAddAttachmentPlaceholdersIndexedByPosition() {
        final var spec = MessageCreateSpec.builder()
                .file(file("map.msav"))
                .file(file("log.txt").withDescription("The server log"))
                .build();

        assertThat(spec.files()).hasSize(2);
        assertThat(spec.payload().objects("attachments"))
                .containsExactly(Json.parse("{\"id\": 0, \"filename\": \"map.msav\"}"), Json.parse("""
                                {"id": 1, "filename": "log.txt", "description": "The server log"}"""));
    }

    @Test
    void noFilesMeansNoAttachmentsKey() {
        assertThat(MessageCreateSpec.builder().content("x").build().payload().has("attachments"))
                .isFalse();
    }

    @Test
    void filesAreUnmodifiable() {
        final var spec = MessageCreateSpec.builder().file(file("a.txt")).build();
        assertThatThrownBy(() -> spec.files().add(file("b.txt"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void builderCopyCarriesPayloadFilesAndReason() {
        final var original = MessageCreateSpec.builder()
                .content("hello")
                .embed(Embed.builder().title("one").build())
                .file(file("map.msav"))
                .reason("audit")
                .build();

        final var copy = MessageCreateSpec.builder(original).build();
        assertThat(copy.payload()).isEqualTo(original.payload());
        assertThat(copy.files()).isEqualTo(original.files());
        assertThat(copy.reason()).isEqualTo(original.reason());

        final var extended = MessageCreateSpec.builder(original)
                .content("goodbye")
                .embed(Embed.builder().title("two").build())
                .build();
        assertThat(extended.payload().string("content")).isEqualTo("goodbye");
        assertThat(extended.payload().objects("embeds")).hasSize(2);
        assertThat(original.payload().string("content")).isEqualTo("hello");
        assertThat(original.payload().objects("embeds")).hasSize(1);
        assertThat(original.files()).hasSize(1);
    }

    @Test
    void toStringShowsThePayload() {
        assertThat(MessageCreateSpec.builder().content("hi").build().toString())
                .startsWith("MessageCreateSpec{")
                .contains("\"content\":\"hi\"");
    }
}
