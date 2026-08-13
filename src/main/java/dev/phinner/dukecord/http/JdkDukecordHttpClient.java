// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import dev.phinner.dukecord.util.Exceptions;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;

public final class JdkDukecordHttpClient implements DukecordHttpClient {

    private final HttpClient client;

    public JdkDukecordHttpClient(final HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public Websocket websocket(final URI endpoint, final long timeout, final TimeUnit unit)
            throws IOException, InterruptedException {
        final var websocket = new JdkWebsocket();
        try {
            this.client.newWebSocketBuilder().buildAsync(endpoint, websocket).get(timeout, unit);
        } catch (final ExecutionException e) {
            throw Exceptions.asIOException("Failed to create websocket for " + endpoint, e.getCause());
        } catch (final CancellationException | TimeoutException e) {
            throw new IOException("Failed to create websocket for " + endpoint, e);
        }
        return websocket;
    }

    private static final class JdkWebsocket implements Websocket, WebSocket.Listener {

        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10L);

        private final BlockingQueue<Websocket.Event> events = new LinkedBlockingQueue<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final StringBuilder fragments = new StringBuilder();
        private final Object lock = new Object();
        private volatile @Nullable WebSocket socket;

        @Override
        public void onOpen(final WebSocket socket) {
            this.socket = socket;
            socket.request(1);
        }

        @Override
        public @Nullable CompletableFuture<?> onText(
                final WebSocket socket, final CharSequence data, final boolean last) {
            this.fragments.append(data);
            if (last) {
                this.events.add(new Event.TextData(this.fragments.toString()));
                this.fragments.setLength(0);
            }
            socket.request(1);
            return null;
        }

        @Override
        public @Nullable CompletableFuture<?> onClose(final WebSocket socket, final int code, final String reason) {
            this.terminate(code, reason);
            return null;
        }

        @Override
        public void onError(final WebSocket socket, final Throwable error) {
            if (this.terminate()) {
                this.events.add(new Websocket.Event.Error(error));
            }
        }

        @Override
        public void send(final String data) throws IOException, InterruptedException {
            if (this.closed.get()) {
                throw new EOFException("WebSocket is closed");
            }
            synchronized (this.lock) {
                try {
                    this.socket().sendText(data, true).get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                } catch (final ExecutionException e) {
                    throw Exceptions.asIOException("Failed to send WebSocket message", e.getCause());
                } catch (final CancellationException | TimeoutException e) {
                    throw new IOException("Failed to to send WebSocket message", e);
                }
            }
        }

        @Override
        public @Nullable Event poll(final long timeout, final TimeUnit unit) throws InterruptedException {
            // TODO throw illegal state when empty and closed?
            return this.events.poll(timeout, unit);
        }

        @Override
        public void close(final int code, final String reason) throws IOException, InterruptedException {
            if (this.terminate(code, reason)) {
                try {
                    this.socket().sendClose(code, reason).get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                } catch (final ExecutionException e) {
                    throw Exceptions.asIOException("Failed to close WebSocket", e.getCause());
                } catch (final CancellationException | TimeoutException e) {
                    throw new IOException("Failed to close WebSocket", e);
                }
            }
        }

        @Override
        public boolean closed() {
            return this.closed.get();
        }

        private WebSocket socket() {
            return Objects.requireNonNull(this.socket, "socket");
        }

        private boolean terminate(final int code, final String reason) {
            final var terminated = this.terminate();
            if (terminated) this.events.add(new Websocket.Event.Close(code, reason));
            return terminated;
        }

        private boolean terminate() {
            return this.closed.compareAndSet(false, true);
        }
    }
}
