// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

public interface DiscordHttpClient {

    RequestBuilder newRequest(final DiscordHttpPath path);

    Websocket newWebsocket(final URI endpoint, final long timeout, final TimeUnit unit)
            throws IOException, InterruptedException;

    interface RequestBuilder {

        RequestBuilder withPathBinding(final String placeholder, final String value);

        RequestBuilder withQueryParam(final String name, final String value);

        RequestBuilder withHeader(final String name, final String value);

        RequestBuilder withBodyPublisher(final HttpRequest.BodyPublisher publisher);

        <T> Response<T> execute(final HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException;
    }

    interface Response<T> {

        int getStatusCode();

        T getBody();

        Map<String, String> getHeaders();
    }

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
