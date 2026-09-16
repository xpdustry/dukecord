// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.user;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.Json;

/** Builds {@link User} snapshots from JSON. The only public way to construct one. */
public final class Users {

    /** Requires {@code id} and {@code username}; everything else is optional. */
    public static User fromJson(final Dukecord duke, final Json json) {
        return new UserImpl(duke, json);
    }

    private Users() {}
}
