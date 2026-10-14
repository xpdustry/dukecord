// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.channel.MessageChannelDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/** The Jackson mapper for Discord payloads. */
public final class DiscordJson {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_ABSENT))
            .addModule(new SimpleModule().setDeserializerModifier(MessageChannelDeserializer.MODIFIER))
            .build();

    public static <T> T read(final Dukecord duke, final String json, final Class<T> type) {
        return MAPPER.readerFor(type)
                .with(new InjectableValues.Std().addValue(Dukecord.class, duke))
                .readValue(json);
    }

    private DiscordJson() {}
}
