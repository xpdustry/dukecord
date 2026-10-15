// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;
import dev.phinner.dukecord.http.DiscordHttpClient;
import dev.phinner.dukecord.http.DiscordHttpPath;
import dev.phinner.dukecord.http.RateLimiter;
import dev.phinner.dukecord.http.URIBuilder;
import dev.phinner.dukecord.util.Exceptions;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordGatewayImpl implements DiscordGateway {

    private static final Logger log = LoggerFactory.getLogger(DiscordGatewayImpl.class);

    private static final int NORMAL_CLOSURE = 1000;
    private static final int UNKNOWN_ERROR = 4000;
    private static final int UNKNOWN_OPCODE = 4001;
    private static final int DECODE_ERROR = 4002;
    private static final int NOT_AUTHENTICATED = 4003;
    private static final int AUTHENTICATION_FAILED = 4004;
    private static final int ALREADY_AUTHENTICATED = 4005;
    private static final int INVALID_SEQ = 4007;
    private static final int RATE_LIMITED = 4008;
    private static final int SESSION_TIMED_OUT = 4009;
    private static final int INVALID_SHARD = 4010;
    private static final int SHARDING_REQUIRED = 4011;
    private static final int INVALID_API_VERSION = 4012;
    private static final int INVALID_INTENTS = 4013;
    private static final int DISALLOWED_INTENTS = 4014;

    private static final int CLIENT_RECONNECT = 4900; // TODO Like JDA's

    private final DiscordHttpClient http;
    private final String token;
    private final GatewayIntentSet intents;
    private final Duration timeout;
    private final DiscordDispatchDecoder eventDecoder;
    private final RateLimiter connectionLimiter =
            RateLimiter.ofExponential(Duration.ofSeconds(5L), Duration.ofMinutes(1));

    private final Thread loop = Thread.ofPlatform().name("DukecordGatewayLoop").unstarted(this::startLoop);
    private @Nullable SessionState session = null;
    private HeartbeatState heartbeat = new HeartbeatState(Duration.ZERO, Instant.MAX, true);
    private volatile boolean closed;

    private final CompletableFuture<@Nullable Void> startFuture = new CompletableFuture<>();
    private final CompletableFuture<@Nullable Void> closeFuture = new CompletableFuture<>();

    private final ExecutorService dispatcher = Executors.newVirtualThreadPerTaskExecutor();
    private final DiscordEventDispatcher publisher = new DiscordEventDispatcher(this.dispatcher);

    public DiscordGatewayImpl(
            final DiscordHttpClient http, final String token, final GatewayIntentSet intents, final Duration timeout) {
        this(http, token, intents, timeout, DiscordDispatchDecoder.unknownOnly());
    }

    public DiscordGatewayImpl(
            final DiscordHttpClient http,
            final String token,
            final GatewayIntentSet intents,
            final Duration timeout,
            final DiscordDispatchDecoder eventDecoder) {
        this.http = http;
        this.token = token;
        this.intents = intents;
        this.timeout = timeout;
        this.eventDecoder = Objects.requireNonNull(eventDecoder);
    }

    @Override
    public void start() throws IOException {
        this.loop.start();
        var succeeded = false;
        try {
            this.startFuture.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
            succeeded = true;
        } catch (final ExecutionException e) {
            throw Exceptions.asIOException("Failed to start WebSocket", e.getCause());
        } catch (final CancellationException | TimeoutException e) {
            throw new IOException("Failed to start WebSocket", e);
        } catch (final InterruptedException e) {
            throw new InterruptedIOException();
        } finally {
            if (!succeeded) this.loop.interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        var succeeded = false;
        try {
            this.closeFuture.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
            succeeded = true;
        } catch (final ExecutionException e) {
            throw Exceptions.asIOException("Failed to close WebSocket", e.getCause());
        } catch (final CancellationException | TimeoutException e) {
            throw new IOException("Failed to close WebSocket", e);
        } catch (final InterruptedException e) {
            throw new InterruptedIOException();
        } finally {
            if (!succeeded) this.loop.interrupt();
        }
    }

    @Override
    public DiscordEventSource events() {
        return this.publisher;
    }

    private void startLoop() {
        try {
            this.loop();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.startFuture.completeExceptionally(e);
            this.closeFuture.completeExceptionally(e);
        }
    }

    private void loop() throws InterruptedException {

        while (!this.closed) {

            try {
                DiscordHttpClient.Websocket websocket = null;
                if (this.session != null) {
                    try {

                        websocket = this.http.newWebsocket(this.session.gateway, 10, TimeUnit.SECONDS);

                    } catch (final IOException e) {
                        log.warn("Failed to connect using the current session, resetting.", e);
                        this.session = null;
                    }
                }

                if (websocket == null) {
                    try {
                        final var endpoint = this.fetchGatewayEndpoint();

                        websocket = this.http.newWebsocket(endpoint, 10, TimeUnit.SECONDS);

                    } catch (final IOException e) {
                        log.warn("Failed to connect to the discord gateway", e);
                        if (this.closed) return;

                        this.connectionLimiter.sleep();
                        continue;
                    }
                }

                this.connectionLimiter.reset();

                while (!this.closed && !websocket.closed()) {
                    final var event = websocket.poll(10, TimeUnit.MILLISECONDS); // TODO Use adjustable frequency
                    switch (event) {
                        case DiscordHttpClient.Websocket.Event.TextData(var payload) -> {
                            final JsonObject json;
                            try {
                                json = JsonParser.object().from(payload);
                            } catch (final JsonParserException e) {
                                log.error("Failed to parse json payload", e);
                                continue;
                            }
                            this.onEventLoop(json, websocket);
                        }
                        case null -> {
                            this.onEventLoop(null, websocket);
                        }
                        case DiscordHttpClient.Websocket.Event.Close(var code, var reason) -> {
                            switch (code) {
                                // Recoverable
                                case SESSION_TIMED_OUT, INVALID_SEQ -> {
                                    this.session = null;
                                    log.error("Received close request with code {}: {}", code, reason);
                                }
                                case UNKNOWN_ERROR,
                                        UNKNOWN_OPCODE,
                                        DECODE_ERROR,
                                        NOT_AUTHENTICATED,
                                        ALREADY_AUTHENTICATED,
                                        RATE_LIMITED -> {
                                    log.error("Received close request with code {}: {}", code, reason);
                                }

                                // Unrecoverable
                                case AUTHENTICATION_FAILED,
                                        INVALID_SHARD,
                                        SHARDING_REQUIRED,
                                        INVALID_API_VERSION,
                                        INVALID_INTENTS,
                                        DISALLOWED_INTENTS -> {
                                    this.startFuture.completeExceptionally(new IOException(
                                            "Received unrecoverable close request with code " + code + ": " + reason));
                                    this.closed = true;
                                    // TODO Should throw to the caller?
                                    log.error("Received unrecoverable close request with code {}: {}", code, reason);
                                }

                                default -> {
                                    // TODO Treat it as fatal error too?
                                    log.error("Received unknown close request with code {}: {}", code, reason);
                                }
                            }
                        }

                        case DiscordHttpClient.Websocket.Event.Error(var e) -> {
                            log.info(e.getClass().getName());

                            log.error("An unexpected error occurred within the websocket", e);
                        }
                    }
                }

                if (!websocket.closed()) {
                    try {

                        websocket.close(
                                this.closed ? NORMAL_CLOSURE : UNKNOWN_ERROR,
                                this.closed ? "Client shutdown." : "Reconnect.");
                    } catch (final IOException e) {
                        log.warn("Failed to close the Discord gateway connection", e);
                    }
                }
            } catch (final IOException e) {
                if (!this.closed) {
                    log.error("An error occurred during the update loop of Minicord", e);

                    this.connectionLimiter.sleep();
                }
            }
        }

        this.closeFuture.complete(null);
    }

    private void onEventLoop(final @Nullable JsonObject event, final DiscordHttpClient.Websocket websocket)
            throws IOException, InterruptedException {
        if (this.heartbeat.nextBeat.isBefore(Instant.now())) {
            if (this.heartbeat.acked) {
                this.heartbeat = this.heartbeat.beat(websocket, this.session);

            } else {

                log.warn("Discord did not acknowledge the previous heartbeat; reconnecting");
                websocket.close(CLIENT_RECONNECT, "Zombie heartbeat.");
                return;
            }
        }

        if (event == null) {
            return;
        }

        final var opcode = GatewayOpcode.from(event.getInt("op"));

        switch (opcode) {
            case HELLO -> {
                final var rawInterval = event.getObject("d").getLong("heartbeat_interval");
                final var interval = Duration.ofMillis(rawInterval);
                final var jitter = ThreadLocalRandom.current().nextLong(rawInterval);
                final var nextBeat = Instant.now().plusMillis(jitter);
                this.heartbeat = new HeartbeatState(interval, nextBeat, true);

                if (this.session != null) {

                    // TODO I crave for object(key, consumer)
                    websocket.send(JsonWriter.string()
                            .object()
                            .value("op", GatewayOpcode.RESUME.code())
                            .object("d")
                            .value("token", this.token)
                            .value("session_id", this.session.id)
                            .value("seq", this.session.sequence)
                            .end()
                            .end()
                            .done());
                } else {

                    websocket.send(JsonWriter.string()
                            .object()
                            .value("op", GatewayOpcode.IDENTIFY.code())
                            .object("d")
                            .value("token", this.token)
                            .value("intents", this.intents.mask())
                            .object("properties")
                            .value("$os", System.getProperty("os.name"))
                            .value("$browser", "Minicord")
                            .value("$device", "Minicord")
                            .end()
                            .end()
                            .end()
                            .done());
                }
            }
            case HEARTBEAT -> {
                this.heartbeat = this.heartbeat.beat(websocket, this.session);
            }
            case HEARTBEAT_ACK -> {
                this.heartbeat = this.heartbeat.ack();
            }
            case DISPATCH -> {
                final var data = event.getObject("d");
                final var sequence = event.getInt("s");
                if (this.session != null) {
                    this.session = this.session.sequence(sequence);
                }
                final var type = event.getString("t");

                switch (type) {
                    case "READY" -> {
                        this.startFuture.complete(null);
                        this.session = new SessionState(
                                data.getString("session_id"),
                                sequence,
                                this.withGatewayParameters(URI.create(data.getString("resume_gateway_url"))));
                    }
                }
                final var payload = JsonWriter.string(data);
                try {
                    publisher.dispatch(this.eventDecoder.decode(type, payload));
                } catch (final DiscordEventDecodingException e) {
                    log.warn("Failed to decode dispatch event {}, publishing its raw payload", type, e);
                    publisher.dispatch(new UnknownDiscordEvent(type, payload));
                }
                log.debug("Received dispatch event: {}", type);
            }
            case RECONNECT -> {
                websocket.close(CLIENT_RECONNECT, "Reconnect request from Discord.");
                return;
            }
            case INVALID_SESSION -> {
                final var resumable = event.getBoolean("d");

                if (!resumable) this.session = null;
                websocket.close(CLIENT_RECONNECT, "Session invalidated by Discord.");
            }
            case UNKNOWN -> {
                log.trace("Received unknown opcode {}", event.getInt("op"));
            }
            default -> {
                log.debug("Unhandled gateway event: {}", event);
            }
        }
    }

    private URI fetchGatewayEndpoint() throws IOException, InterruptedException {
        final var response =
                this.http.newRequest(DiscordHttpPath.GATEWAY).execute(HttpResponse.BodyHandlers.ofString());
        try {
            return this.withGatewayParameters(
                    URI.create(JsonParser.object().from(response.getBody()).getString("url")));
        } catch (final JsonParserException e) {
            throw new IOException("Failed to parse Discord gateway response", e);
        }
    }

    private URI withGatewayParameters(final URI endpoint) {
        return new URIBuilder(endpoint)
                .addParameter("v", "10")
                .addParameter("encoding", "json")
                .build();
    }

    record SessionState(String id, int sequence, URI gateway) {
        public SessionState sequence(final int sequence) {
            return new SessionState(this.id, sequence, this.gateway);
        }
    }

    record HeartbeatState(Duration interval, Instant nextBeat, boolean acked) {

        public HeartbeatState beat(final DiscordHttpClient.Websocket websocket, final @Nullable SessionState session)
                throws IOException, InterruptedException {
            this.send(websocket, session);
            return new HeartbeatState(this.interval, Instant.now().plus(this.interval), false);
        }

        private void send(final DiscordHttpClient.Websocket websocket, final @Nullable SessionState session)
                throws IOException, InterruptedException {
            final var writer = JsonWriter.string().object().value("op", GatewayOpcode.HEARTBEAT.code());
            if (session == null) {
                writer.nul("d");
            } else {
                writer.value("d", session.sequence);
            }
            websocket.send(writer.end().done());
        }

        public HeartbeatState ack() {
            return new HeartbeatState(this.interval, this.nextBeat, true);
        }
    }
}
