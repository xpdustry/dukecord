// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public non-sealed interface CategoryChannel extends GuildChannel {

    /** cache: the channels whose parent is this category, by position. */
    default List<GuildChannel> channels() {
        return this.duke().cache().channels(this.guild().id()).stream()
                .filter(channel -> channel.isInCategory(this.id()))
                .sorted(Comparator.comparingInt(GuildChannel::position))
                .toList();
    }

    /** cache: the first child channel with that exact name. */
    default Optional<GuildChannel> channel(final String name) {
        return this.channels().stream()
                .filter(channel -> channel.name().equals(name))
                .findFirst();
    }
}
