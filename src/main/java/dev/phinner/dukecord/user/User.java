// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.user;

import dev.phinner.dukecord.DiscordEntity;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableUser.class)
public interface User extends DiscordEntity, UserBehavior {

    String username();

    Optional<String> globalName();

    Optional<String> avatar();

    @Value.Default
    default boolean bot() {
        return false;
    }

    @Value.Default
    default boolean system() {
        return false;
    }
}
