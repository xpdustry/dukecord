// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import java.io.IOException;

public final class DiscordHttpException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public DiscordHttpException(final int statusCode) {
        super("Received unexpected status code " + statusCode);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
