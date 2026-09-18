// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.model.channel.CategoryChannel;
import dev.phinner.dukecord.model.channel.DmChannel;
import dev.phinner.dukecord.model.channel.GuildTextChannel;
import dev.phinner.dukecord.model.channel.ThreadChannel;
import dev.phinner.dukecord.model.channel.UnknownChannel;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

final class ChannelPayloads {

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGuildTextCreate.class)
    interface GuildTextCreate extends ChannelCreateEvent {
        @Override
        @JsonUnwrapped
        GuildTextChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableDmCreate.class)
    interface DmCreate extends ChannelCreateEvent {
        @Override
        @JsonUnwrapped
        DmChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCategoryCreate.class)
    interface CategoryCreate extends ChannelCreateEvent {
        @Override
        @JsonUnwrapped
        CategoryChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableThreadCreate.class)
    interface ThreadCreate extends ChannelCreateEvent {
        @Override
        @JsonUnwrapped
        ThreadChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableUnknownCreate.class)
    interface UnknownCreate extends ChannelCreateEvent {
        @Override
        @JsonUnwrapped
        UnknownChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGuildTextUpdate.class)
    interface GuildTextUpdate extends ChannelUpdateEvent {
        @Override
        @JsonUnwrapped
        GuildTextChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableDmUpdate.class)
    interface DmUpdate extends ChannelUpdateEvent {
        @Override
        @JsonUnwrapped
        DmChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCategoryUpdate.class)
    interface CategoryUpdate extends ChannelUpdateEvent {
        @Override
        @JsonUnwrapped
        CategoryChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableThreadUpdate.class)
    interface ThreadUpdate extends ChannelUpdateEvent {
        @Override
        @JsonUnwrapped
        ThreadChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableUnknownUpdate.class)
    interface UnknownUpdate extends ChannelUpdateEvent {
        @Override
        @JsonUnwrapped
        UnknownChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGuildTextDelete.class)
    interface GuildTextDelete extends ChannelDeleteEvent {
        @Override
        @JsonUnwrapped
        GuildTextChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableDmDelete.class)
    interface DmDelete extends ChannelDeleteEvent {
        @Override
        @JsonUnwrapped
        DmChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCategoryDelete.class)
    interface CategoryDelete extends ChannelDeleteEvent {
        @Override
        @JsonUnwrapped
        CategoryChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableThreadDelete.class)
    interface ThreadDelete extends ChannelDeleteEvent {
        @Override
        @JsonUnwrapped
        ThreadChannel channel();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableUnknownDelete.class)
    interface UnknownDelete extends ChannelDeleteEvent {
        @Override
        @JsonUnwrapped
        UnknownChannel channel();
    }

    private ChannelPayloads() {}
}
