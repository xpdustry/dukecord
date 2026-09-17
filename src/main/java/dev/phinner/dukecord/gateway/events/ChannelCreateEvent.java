// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.channel.Channel;

public record ChannelCreateEvent(Channel channel) implements ChannelEvent {}
