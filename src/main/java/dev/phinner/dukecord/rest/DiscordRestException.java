// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.rest;

import dev.phinner.dukecord.model.Json;
import java.time.Duration;
import java.util.Optional;

/** A failed REST call. Unchecked, so blocking bot code is not forced to catch on every send. */
public class DiscordRestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final int code;
    private final transient Json body;
    private final transient Optional<Duration> retryAfter;

    public DiscordRestException(final int status, final int code, final String message, final Json body) {
        this(status, code, message, body, Optional.empty());
    }

    public DiscordRestException(
            final int status,
            final int code,
            final String message,
            final Json body,
            final Optional<Duration> retryAfter) {
        super("HTTP " + status + (code != 0 ? " (Discord error " + code + ")" : "") + ": " + message);
        this.status = status;
        this.code = code;
        this.body = body;
        this.retryAfter = retryAfter;
    }

    /** The HTTP status. */
    public int status() {
        return this.status;
    }

    /** Discord's JSON error code, see {@link DiscordErrorCode}; {@code 0} when the body carried none. */
    public int code() {
        return this.code;
    }

    /** The raw error body, which may include per-field validation errors. */
    public Json body() {
        return this.body;
    }

    /** Present on 429 responses. */
    public Optional<Duration> retryAfter() {
        return this.retryAfter;
    }
}
