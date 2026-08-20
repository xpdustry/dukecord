// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

public interface DiscordEventSource {

    <E extends DiscordEvent> DiscordEventListener.Registration register(
            final Class<E> type, final DiscordEventListener<E> subscriber);
}
