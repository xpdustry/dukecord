// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * A failed REST request. Either Discord answered with an error status, or no answer came at all, in which case
 * {@link #status()} is {@code 0} and the cause holds the transport failure.
 */
public class DiscordRestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final int code;
    private final @Nullable String body;

    /** Reads Discord's {@code code} and {@code message} from the error body when it is JSON. */
    public static DiscordRestException of(final int status, final String body) {
        var code = 0;
        var message = "Discord answered " + status;
        try {
            final JsonNode node = DiscordJson.MAPPER.readTree(body);
            code = node.path("code").asInt(0);
            if (node.path("message").isString()) {
                message += ": " + node.path("message").asString();
            }
        } catch (final JacksonException ignored) {
            // Not JSON, for example an HTML page from a proxy; the raw body is kept.
        }
        return new DiscordRestException(status, code, message, body, null);
    }

    public DiscordRestException(final String message, final Throwable cause) {
        this(0, 0, message, null, cause);
    }

    protected DiscordRestException(
            final int status,
            final int code,
            final String message,
            final @Nullable String body,
            final @Nullable Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.body = body;
    }

    /** The HTTP status, or {@code 0} if no response was received. */
    public int status() {
        return this.status;
    }

    /** Discord's JSON error code, such as {@code 50007} for a user that cannot receive DMs, or {@code 0}. */
    public int code() {
        return this.code;
    }

    /** The raw error body, which also holds the per-field {@code errors} of a rejected form. */
    public Optional<String> body() {
        return Optional.ofNullable(this.body);
    }
}
