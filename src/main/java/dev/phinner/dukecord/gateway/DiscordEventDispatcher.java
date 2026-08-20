// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DiscordEventDispatcher implements DiscordEventSource {

    private static final Logger log = LoggerFactory.getLogger(DiscordEventDispatcher.class);

    private final Map<Class<? extends DiscordEvent>, List<Class<? extends DiscordEvent>>> superclasses =
            new HashMap<>();
    private final Map<Class<? extends DiscordEvent>, List<DiscordEventListener<? extends DiscordEvent>>> subscribers =
            new HashMap<>();
    private final Object lock = new Object();
    private final Executor dispatcher;

    public DiscordEventDispatcher(final Executor dispatcher) {
        this.dispatcher = dispatcher;
    }

    @SuppressWarnings("unchecked")
    public <E extends DiscordEvent> void dispatch(final E event) {
        synchronized (this.lock) {
            for (final var superclass : this.superclasses(event.getClass())) {
                for (final var subscriber : this.subscribers.getOrDefault(superclass, List.of())) {
                    this.dispatcher.execute(() -> {
                        try {
                            ((DiscordEventListener<E>) subscriber).onDiscordEvent(event);
                        } catch (final Exception e) {
                            log.error(
                                    "An unhandled exception occurred while dispatching {}",
                                    event.getClass().getSimpleName(),
                                    e);
                        }
                    });
                }
            }
        }
    }

    @Override
    public <E extends DiscordEvent> DiscordEventListener.Registration register(
            final Class<E> type, final DiscordEventListener<E> subscriber) {
        synchronized (this.lock) {
            for (final var superclass : this.superclasses(type)) {
                this.subscribers
                        .computeIfAbsent(superclass, _ -> new ArrayList<>())
                        .add(subscriber);
            }
        }
        return () -> {
            synchronized (this.lock) {
                for (final var superclass : this.superclasses(type)) {
                    final var subscribers = this.subscribers.get(superclass);
                    if (subscribers != null) {
                        subscribers.remove(subscriber);
                    }
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends DiscordEvent>> superclasses(final Class<? extends DiscordEvent> type) {
        return this.superclasses.computeIfAbsent(type, _ -> {
            final List<Class<? extends DiscordEvent>> types = new ArrayList<>();
            types.add(type);
            for (int i = 0; i < types.size(); i++) {
                final var next = types.get(i);
                final var superclass = next.getSuperclass();
                if (superclass != null && DiscordEvent.class.isAssignableFrom(superclass)) {
                    types.add((Class<? extends DiscordEvent>) superclass);
                }
                for (final var interface0 : next.getInterfaces()) {
                    if (DiscordEvent.class.isAssignableFrom(interface0) && !types.contains(interface0)) {
                        types.add((Class<? extends DiscordEvent>) interface0);
                    }
                }
            }
            return types;
        });
    }
}
