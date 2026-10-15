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

    static ImmutableEntitySelectMenu.Builder builder(final Kind kind) {
        return ImmutableEntitySelectMenu.builder().type(kind.type);
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

        static DefaultValue user(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.builder()
                    .id(id)
                    .type(Type.USER)
                    .build();
        }

        static DefaultValue role(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.builder()
                    .id(id)
                    .type(Type.ROLE)
                    .build();
        }

        static DefaultValue channel(final Snowflake id) {
            return ImmutableEntitySelectMenu.DefaultValue.builder()
                    .id(id)
                    .type(Type.CHANNEL)
                    .build();
        }

        Snowflake id();

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
    }
}
