// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.immutables.value.Value;

@Value.Immutable
public interface ChannelDeleteEvent extends ChannelEvent {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ChannelDeleteEvent of(final Channel channel) {
        return ImmutableChannelDeleteEvent.builder().channel(channel).build();
    }
}
