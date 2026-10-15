// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableCategoryChannel.class)
public non-sealed interface CategoryChannel extends GuildChannel {

    /** The cached channels in this category. */
    default List<GuildChannel> channels() {
        return this.duke().channels().getAllByGuild(this.guild().id()).stream()
                .filter(channel -> channel instanceof GuildChannel guild
                        && guild.parent()
                                .map(ChannelBehavior::id)
                                .filter(this.id()::equals)
                                .isPresent())
                .map(GuildChannel.class::cast)
                .toList();
    }
}
