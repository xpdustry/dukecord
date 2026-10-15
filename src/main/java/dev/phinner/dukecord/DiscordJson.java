// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.channel.MessageChannelDeserializer;
import java.awt.Color;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/** The Jackson mapper for Discord payloads. */
public final class DiscordJson {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_ABSENT))
            .addModule(new SimpleModule()
                    .setDeserializerModifier(MessageChannelDeserializer.MODIFIER)
                    .addSerializer(Color.class, new ColorSerializer())
                    .addDeserializer(Color.class, new ColorDeserializer()))
            .build();

    /**
     * Reads a payload, injecting {@code duke} into behaviors. Members and roles take their guild from the payload's
     * top-level {@code guild_id}.
     */
    public static <T> T read(final Dukecord duke, final String json, final Class<T> type) {
        final var tree = MAPPER.readTree(json);
        final var guildId = tree.path("guild_id");
        return read(duke, guildId.isString() ? Snowflake.of(guildId.asString()) : null, tree, type);
    }

    /** Reads a payload whose members and roles belong to {@code guildId}, such as a fetched member. */
    public static <T> T read(final Dukecord duke, final Snowflake guildId, final String json, final Class<T> type) {
        return read(duke, guildId, MAPPER.readTree(json), type);
    }

    private static <T> T read(
            final Dukecord duke, final @Nullable Snowflake guildId, final JsonNode tree, final Class<T> type) {
        final var injectables = new InjectableValues.Std().addValue(Dukecord.class, duke);
        if (guildId != null) {
            injectables.addValue("guild_id", guildId);
        }
        return MAPPER.readerFor(type).with(injectables).readValue(tree);
    }

    private DiscordJson() {}

    /** Discord colors are RGB integers, so the alpha channel is dropped. */
    private static final class ColorSerializer extends StdScalarSerializer<Color> {

        private ColorSerializer() {
            super(Color.class);
        }

        @Override
        public void serialize(final Color value, final JsonGenerator gen, final SerializationContext ctxt) {
            gen.writeNumber(value.getRGB() & 0xFFFFFF);
        }
    }

    private static final class ColorDeserializer extends StdScalarDeserializer<Color> {

        private ColorDeserializer() {
            super(Color.class);
        }

        @Override
        public Color deserialize(final JsonParser p, final DeserializationContext ctxt) {
            return new Color(p.getIntValue());
        }
    }
}
