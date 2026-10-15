// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

/** Interactions are not cached; only their routes are here. */
public interface InteractionRepository {

    InteractionRestOperations rest();
}
