// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.immutables.value.Value;

@Value.Immutable
public interface ChannelUpdateEvent extends ChannelEvent {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ChannelUpdateEvent of(final Channel channel) {
        return ImmutableChannelUpdateEvent.of(channel);
    }
}
