// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.rest;

import dev.phinner.dukecord.model.Json;

/** HTTP 403: missing permissions, or the target user does not accept messages from the bot. */
public final class DiscordForbiddenException extends DiscordRestException {

    private static final long serialVersionUID = 1L;

    public DiscordForbiddenException(final int code, final String message, final Json body) {
        super(403, code, message, body);
    }
}
