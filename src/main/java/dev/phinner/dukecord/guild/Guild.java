// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.DiscordEntity;
import dev.phinner.dukecord.user.UserBehavior;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuild.class)
public interface Guild extends DiscordEntity, GuildBehavior {

    String name();

    @JsonProperty("owner_id")
    UserBehavior owner();

    Optional<String> description();

    Optional<String> icon();

    Set<String> features();

    Optional<Integer> memberCount();

    Optional<Integer> premiumSubscriptionCount();
}
