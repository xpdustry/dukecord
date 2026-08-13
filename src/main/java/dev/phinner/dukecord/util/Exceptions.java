// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.util;

import java.io.IOException;
import org.jspecify.annotations.Nullable;

public final class Exceptions {

    public static IOException asIOException(final String message, final @Nullable Throwable cause) {
        return cause instanceof IOException e ? e : new IOException(message, cause);
    }

    private Exceptions() {}
}
