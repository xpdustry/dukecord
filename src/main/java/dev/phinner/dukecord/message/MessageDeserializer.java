// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import java.io.Serial;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wraps the generated message deserializer to decode {@link Message#member()}. Discord leaves the user out of the
 * member of a message, since it is the {@code author}, so the author is copied into it first.
 */
public final class MessageDeserializer extends DelegatingDeserializer {

    public static final ValueDeserializerModifier MODIFIER = new ValueDeserializerModifier() {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ValueDeserializer<?> modifyDeserializer(
                final DeserializationConfig config,
                final BeanDescription.Supplier beanDescRef,
                final ValueDeserializer<?> deserializer) {
            return beanDescRef.getBeanClass() == ImmutableMessage.class
                    ? new MessageDeserializer(deserializer)
                    : deserializer;
        }
    };

    @Serial
    private static final long serialVersionUID = 1L;

    private MessageDeserializer(final ValueDeserializer<?> delegatee) {
        super(delegatee);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> delegatee) {
        return new MessageDeserializer(delegatee);
    }

    @Override
    public Object deserialize(final JsonParser p, final DeserializationContext ctxt) throws JacksonException {
        final var tree = ctxt.readTree(p);
        if (tree.get("member") instanceof ObjectNode member && !member.has("user") && tree.has("author")) {
            member.set("user", tree.get("author"));
        }
        try (final JsonParser buffered = tree.traverse(ctxt)) {
            buffered.nextToken();
            return this._delegatee.deserialize(buffered, ctxt);
        }
    }
}
