// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = DiscordEntityRefDeserializer.class)
public interface DiscordEntityRef<T extends DiscordEntity> {

    @JsonIgnore
    Class<T> type();

    @JsonValue
    Snowflake id();

    Optional<T> get();

    Optional<T> get(final LookupStrategy strategy);
}
