// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadTest {

    @TempDir
    Path directory;

    private static String read(final InputStream stream) throws IOException {
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fromPathUsesTheFileNameByDefault() throws IOException {
        final var path = Files.writeString(this.directory.resolve("map.msav"), "content");
        final var upload = FileUpload.of(path);
        assertThat(upload.filename()).isEqualTo("map.msav");
        assertThat(upload.description()).isEmpty();
        assertThat(read(upload.open())).isEqualTo("content");
    }

    @Test
    void fromPathAcceptsAnOverriddenName() throws IOException {
        final var path = Files.writeString(this.directory.resolve("tmp12345"), "content");
        final var upload = FileUpload.of(path, "map.msav");
        assertThat(upload.filename()).isEqualTo("map.msav");
        assertThat(read(upload.open())).isEqualTo("content");
    }

    @Test
    void fromPathOpensAFreshStreamEveryTime() throws IOException {
        final var path = Files.writeString(this.directory.resolve("map.msav"), "content");
        final var upload = FileUpload.of(path);

        final var first = upload.open();
        final var second = upload.open();
        assertThat(first).isNotSameAs(second);
        assertThat(read(first)).isEqualTo("content");
        assertThat(read(second)).isEqualTo("content");
    }

    @Test
    void fromPathReportsAMissingFileWhenTheStreamIsOpened() {
        final var missing = this.directory.resolve("nope.msav");
        final var upload = FileUpload.of(missing);
        assertThat(upload.filename()).isEqualTo("nope.msav");
        assertThatThrownBy(upload::open)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to open");
    }

    @Test
    void fromBytesCopiesTheArray() throws IOException {
        final var bytes = "content".getBytes(StandardCharsets.UTF_8);
        final var upload = FileUpload.of(bytes, "log.txt");
        bytes[0] = 'X';

        assertThat(read(upload.open())).isEqualTo("content");
        assertThat(upload.filename()).isEqualTo("log.txt");
    }

    @Test
    void fromBytesOpensAFreshStreamEveryTime() throws IOException {
        final var upload = FileUpload.of("content".getBytes(StandardCharsets.UTF_8), "log.txt");
        assertThat(read(upload.open())).isEqualTo("content");
        assertThat(read(upload.open())).isEqualTo("content");
    }

    @Test
    void fromStreamCallsTheSupplierOncePerOpen() throws IOException {
        final var opened = new AtomicInteger();
        final var upload = FileUpload.ofStream("log.txt", () -> {
            opened.incrementAndGet();
            return InputStream.nullInputStream();
        });

        assertThat(opened).hasValue(0);
        assertThat(read(upload.open())).isEmpty();
        assertThat(read(upload.open())).isEmpty();
        assertThat(opened).hasValue(2);
    }

    @Test
    void withDescriptionReturnsACopyKeepingTheSource() throws IOException {
        final var upload = FileUpload.of("content".getBytes(StandardCharsets.UTF_8), "log.txt");
        final var described = upload.withDescription("The server log");

        assertThat(described.description()).contains("The server log");
        assertThat(described.filename()).isEqualTo("log.txt");
        assertThat(read(described.open())).isEqualTo("content");
        assertThat(upload.description()).isEmpty();
    }

    @Test
    void toStringNamesTheFile() {
        assertThat(FileUpload.of(new byte[0], "log.txt")).hasToString("FileUpload[log.txt]");
    }
}
