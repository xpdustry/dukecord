// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** A thread to start, from a message or on its own. */
@Value.Immutable
@JsonSerialize(as = ImmutableThreadCreateSpec.class)
public interface ThreadCreateSpec {

    static Builder builder() {
        return ImmutableThreadCreateSpec.builder();
    }

    static ThreadCreateSpec of(final String name) {
        return ImmutableThreadCreateSpec.of(name);
    }

    /** 1 to 100 characters. */
    @Value.Parameter
    String name();

    /** How long without messages before the thread is hidden from the channel list. */
    Optional<ArchiveDuration> autoArchiveDuration();

    /** Seconds a user waits between messages, 0 to 21600. */
    Optional<Integer> rateLimitPerUser();

    /** Only for threads started on their own, which are public by default. */
    Optional<Type> type();

    /** Whether members can invite others, only for private threads. */
    Optional<Boolean> invitable();

    enum ArchiveDuration {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        HOUR(60),
        DAY(1440),
        THREE_DAYS(4320),
        WEEK(10080);

        private final int minutes;

        ArchiveDuration(final int minutes) {
            this.minutes = minutes;
        }

        @JsonValue
        public int minutes() {
            return this.minutes;
        }
    }

    enum Type {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        PUBLIC(11),
        PRIVATE(12);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        @JsonValue
        public int value() {
            return this.value;
        }
    }

    interface Builder {
        Builder from(final ThreadCreateSpec instance);

        Builder name(final String name);

        Builder autoArchiveDuration(final ArchiveDuration autoArchiveDuration);

        Builder rateLimitPerUser(final int rateLimitPerUser);

        Builder type(final Type type);

        Builder invitable(final boolean invitable);

        ThreadCreateSpec build();
    }
}
