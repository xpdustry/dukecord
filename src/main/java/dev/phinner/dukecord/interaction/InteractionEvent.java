// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.phinner.dukecord.channel.MessageChannelBehavior;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;
import dev.phinner.dukecord.guild.GuildBehavior;
import dev.phinner.dukecord.guild.Member;
import dev.phinner.dukecord.guild.PermissionSet;
import dev.phinner.dukecord.user.User;
import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ImmutableUnknownInteractionEvent.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ImmutableCommandInteractionEvent.class, name = "2"),
    @JsonSubTypes.Type(value = ImmutableComponentInteractionEvent.class, name = "3"),
    @JsonSubTypes.Type(value = ImmutableAutocompleteInteractionEvent.class, name = "4"),
    @JsonSubTypes.Type(value = ImmutableModalSubmitInteractionEvent.class, name = "5")
})
public sealed interface InteractionEvent extends DiscordGatewayEvent, InteractionBehavior
        permits AutocompleteInteractionEvent,
                CommandInteractionEvent,
                ComponentInteractionEvent,
                ModalSubmitInteractionEvent,
                UnknownInteractionEvent {

    int type();

    @JsonProperty("guild_id")
    Optional<GuildBehavior> guild();

    @JsonProperty("channel_id")
    Optional<MessageChannelBehavior> channel();

    /** Only in guilds, with the member's permissions in the channel. */
    Optional<Member> member();

    /** Only outside guilds. */
    Optional<User> user();

    /** Who triggered the interaction, in or outside a guild. */
    default User invoker() {
        return this.member().map(Member::user).or(this::user).orElseThrow();
    }

    /** What the bot may do in the channel of the interaction. */
    PermissionSet appPermissions();

    /** The invoker's language. */
    Optional<String> locale();

    Optional<String> guildLocale();

    Optional<InteractionContext> context();
}
