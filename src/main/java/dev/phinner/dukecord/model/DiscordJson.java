// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import dev.phinner.dukecord.Dukecord;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** The Jackson mapper for Discord payloads. */
public final class DiscordJson {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public static <T> T read(final Dukecord duke, final String json, final Class<T> type) {
        return MAPPER.readerFor(type).withAttribute(Dukecord.class, duke).readValue(json);
    }

    private DiscordJson() {}
}
