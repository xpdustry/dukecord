// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record ThreadMetadata(
        boolean archived,
        Duration autoArchiveDuration,
        Instant archiveTimestamp,
        boolean locked,
        Optional<Boolean> invitable,
        Optional<Instant> createTimestamp) {

    public static ThreadMetadata fromJson(final Json json) {
        return new ThreadMetadata(
                json.bool("archived", false),
                Duration.ofMinutes(json.optionalInteger("auto_archive_duration").orElse(0)),
                json.optionalInstant("archive_timestamp").orElse(Instant.EPOCH),
                json.bool("locked", false),
                json.optionalBoolean("invitable"),
                json.optionalInstant("create_timestamp"));
    }
}
