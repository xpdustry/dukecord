// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.DiscordJson;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.http.MultipartFormBodyPublisher;
import java.net.URLConnection;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds the multipart body of a request that uploads files, for REST implementations. A message with files is sent
 * as a {@code payload_json} part holding the usual JSON, followed by one {@code files[n]} part per file.
 */
public final class MessageForm {

    private MessageForm() {}

    /**
     * Writes the {@code attachments} array of {@code message}, which keeps the {@code retained} attachments and gives
     * the {@code files} their descriptions. Discord removes the existing attachments missing from it, so an edit that
     * keeps all of them must leave the array out.
     */
    public static void attachments(
            final ObjectNode message, final List<FileUpload> files, final List<Snowflake> retained) {
        final var attachments = message.putArray("attachments");
        for (final var id : retained) {
            attachments.addObject().put("id", id.asString());
        }
        for (int index = 0; index < files.size(); index++) {
            final var file = files.get(index);
            final var attachment = attachments.addObject().put("id", index).put("filename", filename(file));
            file.description().ifPresent(description -> attachment.put("description", description));
        }
    }

    /** The form of {@code payload}, which is a message or an interaction response holding one. */
    public static MultipartFormBodyPublisher of(final JsonNode payload, final List<FileUpload> files) {
        final var form = new MultipartFormBodyPublisher.Builder()
                .textPart("payload_json", DiscordJson.MAPPER.writeValueAsString(payload));
        for (int index = 0; index < files.size(); index++) {
            final var file = files.get(index);
            final var filename = filename(file);
            final var contentType = URLConnection.guessContentTypeFromName(filename);
            form.formPart(
                    "files[" + index + "]",
                    filename,
                    contentType == null ? "application/octet-stream" : contentType,
                    file.content());
        }
        return form.build();
    }

    /** Discord hides attachments whose name starts with {@code SPOILER_}. */
    private static String filename(final FileUpload file) {
        return file.spoiler() ? "SPOILER_" + file.filename() : file.filename();
    }
}
