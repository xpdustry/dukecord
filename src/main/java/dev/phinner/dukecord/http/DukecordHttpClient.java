// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

public interface DukecordHttpClient {

    Websocket websocket(final URI endpoint, final long timeout, final TimeUnit unit)
            throws IOException, InterruptedException;

    interface Websocket {

        @Nullable Event poll(final long timeout, final TimeUnit unit) throws InterruptedException;

        void send(final String data) throws IOException, InterruptedException;

        void close(final int code, final String reason) throws IOException, InterruptedException;

        boolean closed();

        sealed interface Event {

            record TextData(String data) implements Event {}

            record Close(int code, String reason) implements Event {}

            record Error(Throwable throwable) implements Event {}
        }
    }
}
