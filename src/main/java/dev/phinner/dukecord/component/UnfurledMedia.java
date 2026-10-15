// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A media URL. Only {@link #url()} is sent; Discord fills in the rest. */
@Value.Immutable
@JsonDeserialize(as = ImmutableUnfurledMedia.class)
public interface UnfurledMedia {

    static Builder builder() {
        return ImmutableUnfurledMedia.builder();
    }

    /** Takes a web URL or {@code attachment://filename}. */
    static UnfurledMedia of(final String url) {
        return ImmutableUnfurledMedia.of(url);
    }

    @Value.Parameter
    String url();

    Optional<String> proxyUrl();

    Optional<Integer> width();

    Optional<Integer> height();

    Optional<String> contentType();

    Optional<Snowflake> attachmentId();

    interface Builder {
        Builder from(final UnfurledMedia instance);

        Builder url(final String url);

        Builder proxyUrl(final String proxyUrl);

        Builder width(final int width);

        Builder height(final int height);

        Builder contentType(final String contentType);

        Builder attachmentId(final Snowflake attachmentId);

        UnfurledMedia build();
    }
}
