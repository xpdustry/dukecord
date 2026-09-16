// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.gateway.DiscordEvent;
import dev.phinner.dukecord.gateway.DiscordGatewayImpl;
import dev.phinner.dukecord.gateway.GatewayIntent;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.http.JdkDiscordHttpClient;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {

    private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

    static void main() throws Exception {
        final var http = new JdkDiscordHttpClient(HttpClient.newHttpClient());
        final var token = Files.readString(Path.of("token.txt"))
                .trim();
        try (final var gateway = new DiscordGatewayImpl(
                http,
                token,
                GatewayIntentSet.UN_PRIVILEGED.with(
                        GatewayIntent.GUILDS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES),
                Duration.ofSeconds(5L))) {
            gateway.start();
            gateway.events().register(DiscordEvent.class, event -> {
                log.error("Today I received {}", event);
            });
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}
