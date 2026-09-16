// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuild.class)
public interface Guild extends DiscordEntity {

    String name();

    Snowflake ownerId();

    Optional<String> description();

    Optional<String> icon();

    Set<String> features();

    @Value.Default
    default boolean unavailable() {
        return false;
    }

    Optional<Integer> memberCount();

    Optional<Integer> premiumSubscriptionCount();
}
