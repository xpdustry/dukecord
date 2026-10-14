// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.Snowflake;
import java.io.Serial;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

/**
 * Wraps the generated deserializers of message channels to decode {@link MessageChannel#lastMessage()}, which needs
 * both the channel {@code id} and {@code last_message_id}. The object is buffered first, so field order does not matter.
 */
public final class MessageChannelDeserializer extends DelegatingDeserializer {

    public static final ValueDeserializerModifier MODIFIER = new ValueDeserializerModifier() {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ValueDeserializer<?> modifyDeserializer(
                final DeserializationConfig config,
                final BeanDescription.Supplier beanDescRef,
                final ValueDeserializer<?> deserializer) {
            final var type = beanDescRef.getBeanClass();
            return type == ImmutableDmChannel.class
                            || type == ImmutableGuildTextChannel.class
                            || type == ImmutableThreadChannel.class
                    ? new MessageChannelDeserializer(deserializer)
                    : deserializer;
        }
    };

    private MessageChannelDeserializer(final ValueDeserializer<?> delegatee) {
        super(delegatee);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> delegatee) {
        return new MessageChannelDeserializer(delegatee);
    }

    @Override
    public Object deserialize(final JsonParser p, final DeserializationContext ctxt) throws JacksonException {
        final var tree = ctxt.readTree(p);
        final MessageChannel channel;
        try (final JsonParser buffered = tree.traverse(ctxt)) {
            buffered.nextToken();
            channel = (MessageChannel) this._delegatee.deserialize(buffered, ctxt);
        }
        final var lastMessageId = tree.get("last_message_id");
        if (lastMessageId == null || lastMessageId.isNull()) {
            return channel;
        }
        final var lastMessage = channel.message(Snowflake.of(lastMessageId.asString()));
        return switch (channel) {
            case ImmutableDmChannel dm -> dm.withLastMessage(lastMessage);
            case ImmutableGuildTextChannel text -> text.withLastMessage(lastMessage);
            case ImmutableThreadChannel thread -> thread.withLastMessage(lastMessage);
            default -> throw new IllegalStateException("Unexpected message channel " + channel.getClass());
        };
    }
}
