// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.model.channel.Channel;
import org.immutables.value.Value;

@Value.Immutable
public interface ChannelCreateEvent extends ChannelEvent {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ChannelCreateEvent of(final Channel channel) {
        return ImmutableChannelCreateEvent.builder().channel(channel).build();
    }
}
