// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.event;

public interface DiscordEventPublisher {

    <E extends DiscordEvent> EventSubscription subscribe(final Class<E> type, final EventSubscriber<E> subscriber);
}
