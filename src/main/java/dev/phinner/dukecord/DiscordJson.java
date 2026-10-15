// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.channel.MessageChannelDeserializer;
import dev.phinner.dukecord.message.MessageDeserializer;
import java.awt.Color;
import java.io.Serial;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.type.CollectionType;

/** The Jackson mapper for Discord payloads. */
public final class DiscordJson {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_ABSENT))
            // The set* calls replace the module's mappings, so they come before the add* calls
            .addModule(new SimpleModule()
                    .setSerializers(new BitFlagSerializers())
                    .setDeserializers(new BitFlagDeserializers())
                    .setDeserializerModifier(MessageChannelDeserializer.MODIFIER)
                    .addSerializer(Color.class, new ColorSerializer())
                    .addDeserializer(Color.class, new ColorDeserializer()))
            .addModule(new SimpleModule().setDeserializerModifier(MessageDeserializer.MODIFIER))
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

    private static boolean isBitFlagSet(final CollectionType type) {
        return Set.class.isAssignableFrom(type.getRawClass())
                && type.getContentType().isEnumType()
                && BitFlag.class.isAssignableFrom(type.getContentType().getRawClass());
    }

    private static final class BitFlagSerializers extends SimpleSerializers {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public @Nullable ValueSerializer<?> findCollectionSerializer(
                final SerializationConfig config,
                final CollectionType type,
                final BeanDescription.Supplier beanDescRef,
                final JsonFormat.Value formatOverrides,
                final @Nullable TypeSerializer elementTypeSerializer,
                final @Nullable ValueSerializer<Object> elementValueSerializer) {
            return isBitFlagSet(type)
                    ? new BitFlagSetSerializer()
                    : super.findCollectionSerializer(
                            config, type, beanDescRef, formatOverrides, elementTypeSerializer, elementValueSerializer);
        }
    }

    private static final class BitFlagDeserializers extends SimpleDeserializers {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public @Nullable ValueDeserializer<?> findCollectionDeserializer(
                final CollectionType type,
                final DeserializationConfig config,
                final BeanDescription.Supplier beanDescRef,
                final @Nullable TypeDeserializer elementTypeDeserializer,
                final @Nullable ValueDeserializer<?> elementDeserializer) {
            return isBitFlagSet(type)
                    ? new BitFlagSetDeserializer(type.getContentType().getRawClass())
                    : super.findCollectionDeserializer(
                            type, config, beanDescRef, elementTypeDeserializer, elementDeserializer);
        }
    }

    /** Writes the bits of the flags, leaving {@code UNKNOWN} out. */
    private static final class BitFlagSetSerializer extends StdSerializer<Set<?>> {

        private BitFlagSetSerializer() {
            super(Set.class);
        }

        @Override
        public void serialize(final Set<?> value, final JsonGenerator gen, final SerializationContext ctxt) {
            var bits = 0L;
            for (final var flag : value) {
                bits |= ((BitFlag) flag).mask();
            }
            gen.writeNumber(bits);
        }

        @Override
        public boolean isEmpty(final SerializationContext ctxt, final Set<?> value) {
            return value.isEmpty();
        }
    }

    /** Reads the bits into an enum set, adding {@code UNKNOWN} for the bits no flag has. */
    private static final class BitFlagSetDeserializer extends StdDeserializer<Set<?>> {

        private final Class<?> flagType;

        private BitFlagSetDeserializer(final Class<?> flagType) {
            super(Set.class);
            this.flagType = flagType;
        }

        @Override
        public Set<?> deserialize(final JsonParser p, final DeserializationContext ctxt) {
            // Permissions and other 64-bit fields arrive as strings
            final var bits = p.currentToken() == JsonToken.VALUE_STRING
                    ? Long.parseUnsignedLong(p.getString())
                    : p.getLongValue();
            final var set = this.empty();
            var unknown = bits;
            for (final var constant : this.flagType.getEnumConstants()) {
                final var mask = ((BitFlag) constant).mask();
                if (mask != 0 && (bits & mask) != 0) {
                    set.add(constant);
                    unknown &= ~mask;
                }
            }
            if (unknown != 0) {
                for (final var constant : this.flagType.getEnumConstants()) {
                    if (((BitFlag) constant).mask() == 0) {
                        set.add(constant);
                    }
                }
            }
            return set;
        }

        @Override
        public Set<?> getNullValue(final DeserializationContext ctxt) {
            return this.empty();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Set<Object> empty() {
            return EnumSet.noneOf((Class) this.flagType);
        }
    }
}
