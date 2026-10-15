// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.Snowflake;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A select menu whose options are users, roles, both, or channels. */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableEntitySelectMenu.class)
public non-sealed interface EntitySelectMenu extends Component {

    static Builder builder(final Kind kind) {
        return ImmutableEntitySelectMenu.builder().type(kind.type);
    }

    static EntitySelectMenu of(final Kind kind, final String customId) {
        return ImmutableEntitySelectMenu.of(kind.type, customId);
    }

    @JsonIgnore
    default Kind kind() {
        return switch (this.type()) {
            case 5 -> Kind.USER;
            case 6 -> Kind.ROLE;
            case 7 -> Kind.MENTIONABLE;
            case 8 -> Kind.CHANNEL;
            default -> Kind.UNKNOWN;
        };
    }

    /** One of the {@link Kind} types. */
    @Override
    @Value.Parameter(order = 0)
    int type();

    @Value.Parameter(order = 1)
    String customId();

    Optional<String> placeholder();

    Optional<Integer> minValues();

    Optional<Integer> maxValues();

    /** Only inside a modal. */
    Optional<Boolean> required();

    @Value.Default
    default boolean disabled() {
        return false;
    }

    /** Restricts a channel select to these channel types. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Integer> channelTypes();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<DefaultValue> defaultValues();

    /** The IDs the user picked, only in a submitted modal. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Snowflake> values();

    enum Kind {
        UNKNOWN(-1),
        USER(5),
        ROLE(6),
        MENTIONABLE(7),
        CHANNEL(8);

        private final int type;

        Kind(final int type) {
            this.type = type;
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEntitySelectMenu.DefaultValue.class)
    interface DefaultValue {

        static Builder builder() {
            return ImmutableEntitySelectMenu.DefaultValue.builder();
        }

        static DefaultValue user(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.of(id, Type.USER);
        }

        static DefaultValue role(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.of(id, Type.ROLE);
        }

        static DefaultValue channel(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.of(id, Type.CHANNEL);
        }

        @Value.Parameter
        Snowflake id();

        @Value.Parameter
        Type type();

        enum Type {
            @JsonEnumDefaultValue
            UNKNOWN,
            @JsonProperty("user")
            USER,
            @JsonProperty("role")
            ROLE,
            @JsonProperty("channel")
            CHANNEL
        }

        interface Builder {
            Builder from(final DefaultValue instance);

            Builder id(final Snowflake id);

            Builder type(final DefaultValue.Type type);

            DefaultValue build();
        }
    }

    @Override
    default EntitySelectMenu withDisabled(final boolean disabled) {
        return EntitySelectMenu.builder(this.kind())
                .from(this)
                .disabled(disabled)
                .build();
    }

    interface Builder {
        Builder from(final EntitySelectMenu instance);

        Builder id(final int id);

        Builder type(final int type);

        Builder customId(final String customId);

        Builder placeholder(final String placeholder);

        Builder minValues(final int minValues);

        Builder maxValues(final int maxValues);

        Builder required(final boolean required);

        Builder disabled(final boolean disabled);

        Builder addChannelTypes(final int element);

        Builder addChannelTypes(final int... elements);

        Builder channelTypes(final Iterable<Integer> elements);

        Builder addAllChannelTypes(final Iterable<Integer> elements);

        Builder addDefaultValues(final DefaultValue element);

        Builder addDefaultValues(final DefaultValue... elements);

        Builder defaultValues(final Iterable<? extends DefaultValue> elements);

        Builder addAllDefaultValues(final Iterable<? extends DefaultValue> elements);

        Builder addValues(final Snowflake element);

        Builder addValues(final Snowflake... elements);

        Builder values(final Iterable<? extends Snowflake> elements);

        Builder addAllValues(final Iterable<? extends Snowflake> elements);

        EntitySelectMenu build();
    }
}
