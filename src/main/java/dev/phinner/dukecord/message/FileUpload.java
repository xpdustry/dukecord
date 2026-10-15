// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import org.immutables.value.Value;

/** A file to send with a message. Embeds and components refer to it as {@code attachment://filename}. */
@Value.Immutable
public interface FileUpload {

    static Builder builder() {
        return ImmutableFileUpload.builder();
    }

    static FileUpload of(final Path path) throws FileNotFoundException {
        return of(path.getFileName().toString(), HttpRequest.BodyPublishers.ofFile(path));
    }

    static FileUpload of(final String filename, final byte[] content) {
        return of(filename, HttpRequest.BodyPublishers.ofByteArray(content));
    }

    /** The supplier is called again for each attempt, so the upload can be retried. */
    static FileUpload of(final String filename, final Supplier<? extends InputStream> content) {
        return of(filename, HttpRequest.BodyPublishers.ofInputStream(content));
    }

    private static FileUpload of(final String filename, final HttpRequest.BodyPublisher content) {
        return ImmutableFileUpload.of(filename, content);
    }

    @Value.Parameter
    String filename();

    /** Alt text. */
    Optional<String> description();

    @Value.Default
    default boolean spoiler() {
        return false;
    }

    @Value.Auxiliary
    @Value.Parameter
    HttpRequest.BodyPublisher content();

    interface Builder {
        Builder from(final FileUpload instance);

        Builder filename(final String filename);

        Builder description(final String description);

        Builder spoiler(final boolean spoiler);

        Builder content(final HttpRequest.BodyPublisher content);

        FileUpload build();
    }
}
