// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.store.DiscordEntityRef;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuild.class)
public interface Guild extends DiscordEntity {

    String name();

    @JsonProperty("owner_id")
    DiscordEntityRef<User> owner();

    Optional<String> description();

    Optional<String> icon();

    Set<String> features();

    Optional<Integer> memberCount();

    Optional<Integer> premiumSubscriptionCount();
}
