// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.user.User;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

public final class DiscordEntityRefDeserializer extends StdScalarDeserializer<DiscordEntityRef<?>> {

    private final @Nullable Class<? extends DiscordEntity> entityType;

    public DiscordEntityRefDeserializer() {
        this(null);
    }

    private DiscordEntityRefDeserializer(final @Nullable Class<? extends DiscordEntity> entityType) {
        super(DiscordEntityRef.class);
        this.entityType = entityType;
    }

    @Override
    public ValueDeserializer<?> createContextual(final DeserializationContext context, final BeanProperty property) {
        final JavaType refType = context.getContextualType();
        if (refType == null || refType.containedTypeCount() != 1) {
            return context.reportBadDefinition(
                    context.constructType(DiscordEntityRef.class), "DiscordEntityRef must declare its entity type");
        }

        final Class<?> rawEntityType = refType.containedType(0).getRawClass();
        if (!DiscordEntity.class.isAssignableFrom(rawEntityType)) {
            return context.reportBadDefinition(refType, "DiscordEntityRef type must implement DiscordEntity");
        }

        return new DiscordEntityRefDeserializer(rawEntityType.asSubclass(DiscordEntity.class));
    }

    @Override
    public DiscordEntityRef<?> deserialize(final JsonParser parser, final DeserializationContext context)
            throws JacksonException {
        final String value = parser.getValueAsString();
        if (value == null) {
            return context.reportInputMismatch(DiscordEntityRef.class, "Discord entity IDs must be JSON strings");
        }
        if (this.entityType == null) {
            return context.reportBadDefinition(
                    context.constructType(DiscordEntityRef.class),
                    "DiscordEntityRef cannot be deserialized without a concrete entity type");
        }

        final Object contextDuke = context.getAttribute(Dukecord.class);
        if (!(contextDuke instanceof Dukecord duke)) {
            return context.reportInputMismatch(
                    DiscordEntityRef.class, "DiscordEntityRef requires a Dukecord-bound ObjectReader");
        }
        return create(this.entityType, Snowflake.of(value), duke);
    }

    private static DiscordEntityRef<?> create(
            final Class<? extends DiscordEntity> type, final Snowflake id, final Dukecord duke) {
        if (type == Guild.class) {
            return createTyped(Guild.class, id, duke.guilds());
        }
        if (type == Channel.class) {
            return createTyped(Channel.class, id, duke.channels());
        }
        if (type == User.class) {
            return createTyped(User.class, id, duke.users());
        }
        throw new IllegalArgumentException("No store for Discord entity type " + type.getName());
    }

    private static <T extends DiscordEntity> DiscordEntityRef<T> createTyped(
            final Class<T> type, final Snowflake id, final DiscordEntityStore.WithIdLookup<T> store) {
        return new DiscordEntityRefImpl<>(type, id, store);
    }
}
