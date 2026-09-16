// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.net.URI;
import java.util.Optional;

/** A file attached to a message. A value, not an entity: download it through {@code duke.rest().download(...)}. */
public record Attachment(
        Snowflake id,
        String filename,
        Optional<String> description,
        Optional<String> contentType,
        long size,
        URI url,
        URI proxyUrl,
        Optional<Integer> width,
        Optional<Integer> height,
        boolean ephemeral) {

    public static Attachment fromJson(final Json json) {
        return new Attachment(
                json.snowflake("id"),
                json.string("filename"),
                json.optionalString("description"),
                json.optionalString("content_type"),
                json.longValue("size"),
                URI.create(json.string("url")),
                URI.create(json.string("proxy_url")),
                json.optionalInteger("width"),
                json.optionalInteger("height"),
                json.bool("ephemeral", false));
    }
}
