// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadMetadataTest {

    private static final String METADATA = """
            {
              "archived": true,
              "auto_archive_duration": 4320,
              "archive_timestamp": "2026-09-15T12:00:00.123456+00:00",
              "locked": true,
              "invitable": true,
              "create_timestamp": "2026-09-01T08:30:00Z"
            }
            """;

    @Test
    void parsesEveryField() {
        final var metadata = ThreadMetadata.fromJson(Json.parse(METADATA));

        assertThat(metadata.archived()).isTrue();
        assertThat(metadata.autoArchiveDuration()).isEqualTo(Duration.ofDays(3));
        assertThat(metadata.archiveTimestamp()).isEqualTo(Instant.parse("2026-09-15T12:00:00.123456Z"));
        assertThat(metadata.locked()).isTrue();
        assertThat(metadata.invitable()).contains(true);
        assertThat(metadata.createTimestamp()).contains(Instant.parse("2026-09-01T08:30:00Z"));
    }

    @Test
    void defaultsEveryFieldOfAnEmptyObject() {
        final var metadata = ThreadMetadata.fromJson(Json.empty());

        assertThat(metadata.archived()).isFalse();
        assertThat(metadata.autoArchiveDuration()).isEqualTo(Duration.ZERO);
        assertThat(metadata.archiveTimestamp()).isEqualTo(Instant.EPOCH);
        assertThat(metadata.locked()).isFalse();
        assertThat(metadata.invitable()).isEmpty();
        assertThat(metadata.createTimestamp()).isEmpty();
    }

    @Test
    void treatsANullCreateTimestampAsAbsent() {
        final var metadata = ThreadMetadata.fromJson(Json.parse("""
                {
                  "archived": false,
                  "auto_archive_duration": 60,
                  "archive_timestamp": "2026-09-15T12:00:00Z",
                  "locked": false,
                  "create_timestamp": null
                }
                """));

        assertThat(metadata.autoArchiveDuration()).isEqualTo(Duration.ofHours(1));
        assertThat(metadata.createTimestamp()).isEmpty();
        assertThat(metadata.invitable()).isEmpty();
    }
}
