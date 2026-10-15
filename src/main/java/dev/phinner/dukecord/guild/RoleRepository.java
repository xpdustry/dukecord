// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.DiscordEntityRepository;

public interface RoleRepository extends DiscordEntityRepository.WithGuildLookup<Role> {

    RoleRestOperations rest();
}
