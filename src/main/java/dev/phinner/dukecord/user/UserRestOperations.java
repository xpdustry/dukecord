// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import dev.phinner.dukecord.Snowflake;

public interface UserRestOperations {

    User fetchSelf();

    User fetch(final Snowflake userId);
}
