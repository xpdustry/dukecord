// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.JsonException;
import dev.phinner.dukecord.model.Snowflake;
import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentTest {

    private static final String FULL = """
            {
              "id": "1900000000000000001",
              "filename": "screenshot.png",
              "description": "the crash",
              "content_type": "image/png",
              "size": 1048576,
              "url": "https://cdn.discordapp.com/attachments/120/130/screenshot.png",
              "proxy_url": "https://media.discordapp.net/attachments/120/130/screenshot.png",
              "width": 1920,
              "height": 1080,
              "ephemeral": true
            }
            """;

    private static final String MINIMAL = """
            {
              "id": "1900000000000000002",
              "filename": "map.msav",
              "size": 2048,
              "url": "https://cdn.discordapp.com/attachments/120/130/map.msav",
              "proxy_url": "https://media.discordapp.net/attachments/120/130/map.msav"
            }
            """;

    @Test
    void parsesEveryField() {
        final var attachment = Attachment.fromJson(Json.parse(FULL));

        assertThat(attachment.id()).isEqualTo(Snowflake.of("1900000000000000001"));
        assertThat(attachment.filename()).isEqualTo("screenshot.png");
        assertThat(attachment.description()).contains("the crash");
        assertThat(attachment.contentType()).contains("image/png");
        assertThat(attachment.size()).isEqualTo(1_048_576L);
        assertThat(attachment.url())
                .isEqualTo(URI.create("https://cdn.discordapp.com/attachments/120/130/screenshot.png"));
        assertThat(attachment.proxyUrl())
                .isEqualTo(URI.create("https://media.discordapp.net/attachments/120/130/screenshot.png"));
        assertThat(attachment.width()).contains(1920);
        assertThat(attachment.height()).contains(1080);
        assertThat(attachment.ephemeral()).isTrue();
    }

    @Test
    void leavesTheOptionalFieldsEmpty() {
        final var attachment = Attachment.fromJson(Json.parse(MINIMAL));

        assertThat(attachment.description()).isEmpty();
        assertThat(attachment.contentType()).isEmpty();
        assertThat(attachment.width()).isEmpty();
        assertThat(attachment.height()).isEmpty();
        assertThat(attachment.ephemeral()).isFalse();
    }

    @Test
    void rejectsAPayloadWithoutTheRequiredFields() {
        final var json = Json.parse(MINIMAL).without("proxy_url");

        assertThatThrownBy(() -> Attachment.fromJson(json))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("proxy_url");
    }

    @Test
    void comparesByValue() {
        assertThat(Attachment.fromJson(Json.parse(FULL))).isEqualTo(Attachment.fromJson(Json.parse(FULL)));
        assertThat(Attachment.fromJson(Json.parse(FULL))).isNotEqualTo(Attachment.fromJson(Json.parse(MINIMAL)));
    }
}
