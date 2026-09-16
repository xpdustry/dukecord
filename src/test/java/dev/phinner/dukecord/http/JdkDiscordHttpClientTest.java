// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkDiscordHttpClientTest {

    private HttpServer server;
    private JdkDiscordHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.server.start();
        this.client = new JdkDiscordHttpClient(
                HttpClient.newHttpClient(),
                URI.create("http://localhost:" + this.server.getAddress().getPort() + "/api"));
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    @Test
    void returnsResponseAndSendsRequestOnce() throws Exception {
        final var requests = new AtomicInteger();
        this.server.createContext("/api/gateway", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("X-Test", "value");
            final var body = "{\"url\":\"wss://gateway.example\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        final var response =
                this.client.newRequest(DiscordHttpPath.GATEWAY).execute(HttpResponse.BodyHandlers.ofString());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("gateway.example");
        assertThat(response.getHeaders()).containsEntry("x-test", "value");
        assertThat(requests).hasValue(1);
    }

    @Test
    void rejectsNonSuccessfulStatusWithoutRetrying() {
        final var requests = new AtomicInteger();
        this.server.createContext("/api/gateway", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        assertThatThrownBy(() ->
                        this.client.newRequest(DiscordHttpPath.GATEWAY).execute(HttpResponse.BodyHandlers.discarding()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
        assertThat(requests).hasValue(1);
    }
}
