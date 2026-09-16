// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.support.TestDukecord;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEditSpecTest {

    private static final Snowflake FIRST_ATTACHMENT = Snowflake.of("1099009338365501490");
    private static final Snowflake SECOND_ATTACHMENT = Snowflake.of("1099009338365501491");

    private static FileUpload file(final String name) {
        return FileUpload.of("data".getBytes(StandardCharsets.UTF_8), name);
    }

    @Test
    void anUntouchedBuilderYieldsAnEmptyPayload() {
        final var spec = MessageEditSpec.builder().build();
        assertThat(spec.payload()).isEqualTo(Json.empty());
        assertThat(spec.payload().isEmpty()).isTrue();
        assertThat(spec.files()).isEmpty();
        assertThat(spec.reason()).isEmpty();
    }

    @Test
    void contentIsSetOrClearedWithAPresentNull() {
        assertThat(MessageEditSpec.builder().content("edited").build().payload().string("content"))
                .isEqualTo("edited");

        final var cleared = MessageEditSpec.builder().clearContent().build();
        assertThat(cleared.payload().has("content")).isTrue();
        assertThat(cleared.payload().isNull("content")).isTrue();
        assertThat(cleared.payload().isPresent("content")).isFalse();
    }

    @Test
    void settingContentSilencesMentionsUnlessTheCallerSaysOtherwise() {
        assertThat(MessageEditSpec.builder()
                        .content("@everyone")
                        .build()
                        .payload()
                        .object("allowed_mentions"))
                .isEqualTo(AllowedMentions.none().toJson());
        assertThat(MessageEditSpec.builder()
                        .content("@everyone")
                        .allowedMentions(AllowedMentions.all())
                        .build()
                        .payload()
                        .object("allowed_mentions"))
                .isEqualTo(AllowedMentions.all().toJson());
        assertThat(MessageEditSpec.builder()
                        .embed(Embed.builder().title("x").build())
                        .build()
                        .payload()
                        .has("allowed_mentions"))
                .isFalse();
    }

    @Test
    void embedsAreReplacedNotAppended() {
        final var first = Embed.builder().title("one").build();
        final var second = Embed.builder().title("two").build();

        final var spec =
                MessageEditSpec.builder().embed(first).embeds(List.of(second)).build();
        assertThat(spec.payload().objects("embeds")).containsExactly(second.toJson());
    }

    @Test
    void clearEmbedsWritesAnEmptyArrayNotANull() {
        final var spec = MessageEditSpec.builder().clearEmbeds().build();
        assertThat(spec.payload().has("embeds")).isTrue();
        assertThat(spec.payload().isNull("embeds")).isFalse();
        assertThat(spec.payload().list("embeds")).isEmpty();
    }

    @Test
    void componentsAreReplacedAndClearedWithAnEmptyArray() {
        final var row = ActionRow.of(Button.primary("a", "A"));
        assertThat(MessageEditSpec.builder()
                        .components(List.of(row))
                        .build()
                        .payload()
                        .objects("components"))
                .containsExactly(row.toJson());

        final var cleared = MessageEditSpec.builder().clearComponents().build();
        assertThat(cleared.payload().has("components")).isTrue();
        assertThat(cleared.payload().list("components")).isEmpty();
    }

    @Test
    void attachmentsKeyIsAbsentWhenNothingTouchesAttachments() {
        assertThat(MessageEditSpec.builder().content("x").build().payload().has("attachments"))
                .isFalse();
    }

    @Test
    void keepAttachmentsListsTheIdsToKeep() {
        final var spec = MessageEditSpec.builder()
                .keepAttachments(List.of(FIRST_ATTACHMENT, SECOND_ATTACHMENT))
                .build();
        assertThat(spec.payload().objects("attachments"))
                .containsExactly(
                        Json.parse("{\"id\": \"1099009338365501490\"}"),
                        Json.parse("{\"id\": \"1099009338365501491\"}"));
    }

    @Test
    void removeAttachmentsWritesAnEmptyArray() {
        final var spec = MessageEditSpec.builder().removeAttachments().build();
        assertThat(spec.payload().has("attachments")).isTrue();
        assertThat(spec.payload().list("attachments")).isEmpty();
    }

    @Test
    void filesWithoutDescriptionsLeaveTheAttachmentsKeyAbsent() {
        final var spec = MessageEditSpec.builder().file(file("map.msav")).build();
        assertThat(spec.files()).hasSize(1);
        assertThat(spec.payload().has("attachments")).isFalse();
    }

    @Test
    void filesWithADescriptionWriteTheirPlaceholders() {
        final var spec = MessageEditSpec.builder()
                .file(file("map.msav"))
                .file(file("log.txt").withDescription("The server log"))
                .build();
        assertThat(spec.payload().objects("attachments"))
                .containsExactly(Json.parse("{\"id\": 0, \"filename\": \"map.msav\"}"), Json.parse("""
                                {"id": 1, "filename": "log.txt", "description": "The server log"}"""));
    }

    @Test
    void keptAttachmentsComeBeforeTheNewPlaceholders() {
        final var spec = MessageEditSpec.builder()
                .keepAttachments(List.of(FIRST_ATTACHMENT))
                .file(file("map.msav"))
                .build();
        assertThat(spec.payload().objects("attachments"))
                .containsExactly(
                        Json.parse("{\"id\": \"1099009338365501490\"}"),
                        Json.parse("{\"id\": 0, \"filename\": \"map.msav\"}"));
    }

    @Test
    void removeAttachmentsStillUploadsTheNewFiles() {
        final var spec = MessageEditSpec.builder()
                .removeAttachments()
                .file(file("map.msav"))
                .build();
        assertThat(spec.payload().objects("attachments"))
                .containsExactly(Json.parse("{\"id\": 0, \"filename\": \"map.msav\"}"));
    }

    @Test
    void suppressEmbedsWritesTheOnlyFlagAnEditCanChange() {
        assertThat(MessageEditSpec.builder()
                        .suppressEmbeds(true)
                        .build()
                        .payload()
                        .integer("flags"))
                .isEqualTo(MessageFlag.SUPPRESS_EMBEDS.mask());
        assertThat(MessageEditSpec.builder()
                        .suppressEmbeds(false)
                        .build()
                        .payload()
                        .integer("flags"))
                .isZero();
        assertThat(MessageEditSpec.builder().build().payload().has("flags")).isFalse();
    }

    @Test
    void reasonIsCarriedSeparatelyFromThePayload() {
        assertThat(MessageEditSpec.builder().reason("audit").build().reason()).contains("audit");
        assertThat(MessageEditSpec.builder().reason(null).build().reason()).isEmpty();
        assertThat(MessageEditSpec.builder().reason("audit").build().payload().has("reason"))
                .isFalse();
    }

    @Test
    void fromCreateCopiesOnlyTheKeysAnEditAccepts() {
        final var embed = Embed.builder().title("one").build();
        final var row = ActionRow.of(Button.primary("a", "A"));
        final var create = MessageCreateSpec.builder()
                .content("hello")
                .embed(embed)
                .component(row)
                .allowedMentions(AllowedMentions.all())
                .suppressEmbeds()
                .ephemeral()
                .tts(true)
                .nonce("abc")
                .enforceNonce(true)
                .stickers(List.of(Snowflake.of("1")))
                .reference(Snowflake.of("2"), Snowflake.of("3"), true)
                .file(file("map.msav"))
                .reason("audit")
                .build();

        final var edit = MessageEditSpec.from(create);

        assertThat(edit.payload().asMap())
                .containsOnlyKeys("content", "embeds", "components", "allowed_mentions", "flags");
        assertThat(edit.payload().string("content")).isEqualTo("hello");
        assertThat(edit.payload().objects("embeds")).containsExactly(embed.toJson());
        assertThat(edit.payload().objects("components")).containsExactly(row.toJson());
        assertThat(edit.payload().object("allowed_mentions"))
                .isEqualTo(AllowedMentions.all().toJson());
        // Only the suppress-embeds bit survives; ephemeral is a create-only flag.
        assertThat(edit.payload().integer("flags")).isEqualTo(MessageFlag.SUPPRESS_EMBEDS.mask());
        assertThat(edit.files()).isEqualTo(create.files());
        assertThat(edit.reason()).contains("audit");
    }

    @Test
    void fromCreateDropsTheFlagsKeyWhenSuppressEmbedsIsNotSet() {
        final var edit = MessageEditSpec.from(
                MessageCreateSpec.builder().content("hello").ephemeral().build());
        assertThat(edit.payload().has("flags")).isFalse();
        assertThat(edit.payload().asMap()).containsOnlyKeys("content", "allowed_mentions");
    }

    @Test
    void fromAnEmptyCreateKeepsOnlyTheSilencedMentions() {
        final var edit = MessageEditSpec.from(MessageCreateSpec.builder().build());
        assertThat(edit.payload().asMap()).containsOnlyKeys("allowed_mentions");
        assertThat(edit.payload().object("allowed_mentions"))
                .isEqualTo(AllowedMentions.none().toJson());
    }

    @Test
    void disablingComponentsOfAMessageDisablesEveryControlAndTouchesNothingElse() {
        final var duke = new TestDukecord();
        final var message = duke.putMessage(Json.parse("""
                {
                  "id": "1099009338365501234",
                  "channel_id": "41771983423143937",
                  "guild_id": "41771983423143936",
                  "author": {"id": "302096570298990593", "username": "phinner"},
                  "timestamp": "2026-09-15T10:30:00.000000+00:00",
                  "content": "Pick a page",
                  "components": [
                    {
                      "type": 1,
                      "components": [
                        {"type": 2, "style": 1, "custom_id": "page:prev", "label": "Prev", "disabled": false},
                        {"type": 2, "style": 5, "url": "https://xpdustry.com", "label": "Web", "disabled": false}
                      ]
                    },
                    {
                      "type": 1,
                      "components": [
                        {"type": 3, "custom_id": "page:goto", "disabled": false,
                         "options": [{"label": "One", "value": "1"}]}
                      ]
                    }
                  ]
                }"""));

        final var spec = MessageEditSpec.disablingComponentsOf(message);

        assertThat(spec.payload().asMap()).containsOnlyKeys("components");
        final var rows = spec.payload().objects("components");
        assertThat(rows).hasSize(2);
        for (final var row : rows) {
            for (final var child : row.objects("components")) {
                assertThat(child.bool("disabled")).describedAs("%s", child).isTrue();
            }
        }
    }

    @Test
    void disablingComponentsOfAMessageWithoutComponentsWritesAnEmptyArray() {
        final var duke = new TestDukecord();
        final var message = duke.putMessage(Json.parse("""
                {
                  "id": "1099009338365501234",
                  "channel_id": "41771983423143937",
                  "author": {"id": "302096570298990593", "username": "phinner"},
                  "timestamp": "2026-09-15T10:30:00.000000+00:00",
                  "content": "plain"
                }"""));

        assertThat(MessageEditSpec.disablingComponentsOf(message).payload())
                .isEqualTo(Json.empty().with("components", List.of()));
    }

    @Test
    void toStringShowsThePayload() {
        assertThat(MessageEditSpec.builder().content("hi").build().toString())
                .startsWith("MessageEditSpec{")
                .contains("\"content\":\"hi\"");
    }
}
