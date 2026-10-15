// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A file attached to a received message. Files to send are {@link FileUpload}s. */
@Value.Immutable
@JsonDeserialize(as = ImmutableAttachment.class)
public interface Attachment {

    Snowflake id();

    String filename();

    Optional<String> title();

    Optional<String> description();

    Optional<String> contentType();

    /** In bytes. */
    long size();

    String url();

    String proxyUrl();

    /** Only for images and videos. */
    Optional<Integer> width();

    /** Only for images and videos. */
    Optional<Integer> height();

    /** Only for voice messages. */
    Optional<Double> durationSecs();

    @Value.Default
    default boolean ephemeral() {
        return false;
    }

    @Value.Default
    default int flags() {
        return 0;
    }
}
