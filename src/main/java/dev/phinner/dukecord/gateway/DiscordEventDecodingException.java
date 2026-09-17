// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import java.io.Serial;

public final class DiscordEventDecodingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DiscordEventDecodingException(final String type, final Throwable cause) {
        super("Failed to decode Discord gateway event " + type, cause);
    }
}
