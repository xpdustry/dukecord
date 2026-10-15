// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.DiscordEntityRepository;

public interface MemberRepository extends DiscordEntityRepository.WithGuildLookup<Member> {

    MemberRestOperations rest();
}
