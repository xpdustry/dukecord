// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.channel.Channel;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

public final class ChannelEventDeserializer extends ValueDeserializer<ChannelEvent> {

    private final @Nullable Class<? extends ChannelEvent> eventType;

    public ChannelEventDeserializer() {
        this(null);
    }

    private ChannelEventDeserializer(final @Nullable Class<? extends ChannelEvent> eventType) {
        this.eventType = eventType;
    }

    @Override
    public ValueDeserializer<?> createContextual(final DeserializationContext context, final BeanProperty property) {
        final JavaType type = context.getContextualType();
        if (type == null || !ChannelEvent.class.isAssignableFrom(type.getRawClass())) {
            return context.reportBadDefinition(
                    context.constructType(ChannelEvent.class),
                    "ChannelEvent deserializer requires a ChannelEvent target type");
        }
        return new ChannelEventDeserializer(type.getRawClass().asSubclass(ChannelEvent.class));
    }

    @Override
    public ChannelEvent deserialize(final JsonParser parser, final DeserializationContext context)
            throws JacksonException {
        final Channel channel = context.readValue(parser, Channel.class);
        final var type = this.eventType;
        if (type == ChannelCreateEvent.class) {
            return ImmutableChannelCreateEvent.builder().channel(channel).build();
        }
        if (type == ChannelUpdateEvent.class) {
            return ImmutableChannelUpdateEvent.builder().channel(channel).build();
        }
        if (type == ChannelDeleteEvent.class) {
            return ImmutableChannelDeleteEvent.builder().channel(channel).build();
        }
        return context.reportBadDefinition(
                context.constructType(ChannelEvent.class),
                "ChannelEvent deserializer was not contextualized with a concrete event type");
    }
}
