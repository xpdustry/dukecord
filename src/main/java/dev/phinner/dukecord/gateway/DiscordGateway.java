// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import java.io.IOException;

interface DiscordGateway extends AutoCloseable {

    void start() throws IOException;

    @Override
    void close() throws IOException;

    DiscordEventSource events();
}
