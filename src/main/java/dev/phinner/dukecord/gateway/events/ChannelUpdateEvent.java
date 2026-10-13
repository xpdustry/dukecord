// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.model.channel.Channel;
import org.immutables.value.Value;

@Value.Immutable
public interface ChannelUpdateEvent extends ChannelEvent {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ChannelUpdateEvent of(final Channel channel) {
        return ImmutableChannelUpdateEvent.builder().channel(channel).build();
    }
}
