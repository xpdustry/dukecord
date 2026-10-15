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
        return ImmutableUnfurledMedia.builder().url(url).build();
    }

    String url();

    Optional<String> proxyUrl();

    Optional<Integer> width();

    Optional<Integer> height();

    Optional<String> contentType();

    Optional<Snowflake> attachmentId();

    interface Builder {
        Builder from(UnfurledMedia instance);

        Builder url(String url);

        Builder proxyUrl(String proxyUrl);

        Builder proxyUrl(Optional<String> proxyUrl);

        Builder width(int width);

        Builder width(Optional<Integer> width);

        Builder height(int height);

        Builder height(Optional<Integer> height);

        Builder contentType(String contentType);

        Builder contentType(Optional<String> contentType);

        Builder attachmentId(Snowflake attachmentId);

        Builder attachmentId(Optional<? extends Snowflake> attachmentId);

        UnfurledMedia build();
    }
}
