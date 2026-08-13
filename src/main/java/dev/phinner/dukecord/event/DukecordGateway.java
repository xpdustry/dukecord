// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.event;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

interface DukecordGateway {

    void start(final long timeout, final TimeUnit unit) throws IOException, InterruptedException;

    void close(final long timeout, final TimeUnit unit) throws IOException, InterruptedException;

    DiscordEventPublisher events();
}
