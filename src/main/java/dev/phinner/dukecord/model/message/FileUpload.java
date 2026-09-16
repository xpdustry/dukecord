// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** A file to upload with a message. The stream is opened when the request is sent. */
public final class FileUpload {

    private final String filename;
    private final Optional<String> description;
    private final Supplier<InputStream> source;

    private FileUpload(final String filename, final Optional<String> description, final Supplier<InputStream> source) {
        this.filename = Objects.requireNonNull(filename, "filename");
        this.description = Objects.requireNonNull(description, "description");
        this.source = Objects.requireNonNull(source, "source");
    }

    public static FileUpload of(final Path path) {
        return of(path, path.getFileName().toString());
    }

    public static FileUpload of(final Path path, final String filename) {
        Objects.requireNonNull(path, "path");
        return new FileUpload(filename, Optional.empty(), () -> {
            try {
                return Files.newInputStream(path);
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to open " + path, e);
            }
        });
    }

    public static FileUpload of(final byte[] bytes, final String filename) {
        final var copy = bytes.clone();
        return new FileUpload(filename, Optional.empty(), () -> new ByteArrayInputStream(copy));
    }

    public static FileUpload ofStream(final String filename, final Supplier<InputStream> source) {
        return new FileUpload(filename, Optional.empty(), source);
    }

    public FileUpload withDescription(final String description) {
        return new FileUpload(this.filename, Optional.of(description), this.source);
    }

    public String filename() {
        return this.filename;
    }

    public Optional<String> description() {
        return this.description;
    }

    /** Opens a fresh stream; the caller closes it. */
    public InputStream open() {
        return this.source.get();
    }

    @Override
    public String toString() {
        return "FileUpload[" + this.filename + "]";
    }
}
