// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.rest;

import dev.phinner.dukecord.model.Json;

/** HTTP 404: Discord does not know the id. */
public final class DiscordNotFoundException extends DiscordRestException {

    private static final long serialVersionUID = 1L;

    public DiscordNotFoundException(final int code, final String message, final Json body) {
        super(404, code, message, body);
    }
}
