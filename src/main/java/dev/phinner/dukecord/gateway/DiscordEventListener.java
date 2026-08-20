// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

@FunctionalInterface
public interface DiscordEventListener<E> {

    void onDiscordEvent(final E event);

    interface Registration {

        void unregister();
    }
}
