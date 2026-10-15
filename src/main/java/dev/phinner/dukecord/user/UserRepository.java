// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import dev.phinner.dukecord.DiscordEntityRepository;

public interface UserRepository extends DiscordEntityRepository.WithIdLookup<User> {

    /** The bot's user, cached from {@code READY} and kept current by {@code USER_UPDATE}. */
    User getSelf();

    UserRestOperations rest();
}
