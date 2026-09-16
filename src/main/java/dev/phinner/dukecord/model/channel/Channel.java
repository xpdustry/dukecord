// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.phinner.dukecord.model.DiscordEntity;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ImmutableUnknownChannel.class)
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = ImmutableGuildTextChannel.class,
            names = {"0", "5"}),
    @JsonSubTypes.Type(
            value = ImmutableDmChannel.class,
            names = {"1", "3"}),
    @JsonSubTypes.Type(value = ImmutableCategoryChannel.class, name = "4"),
    @JsonSubTypes.Type(
            value = ImmutableThreadChannel.class,
            names = {"10", "11", "12"})
})
public sealed interface Channel extends DiscordEntity permits GuildChannel, MessageChannel, UnknownChannel {

    int type();
}
