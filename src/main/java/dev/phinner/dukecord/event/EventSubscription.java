// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.event;

@FunctionalInterface
public interface EventSubscription {

    void cancel();
}
