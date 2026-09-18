// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ImmutableUnknownDelete.class)
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = ImmutableGuildTextDelete.class,
            names = {"0", "5"}),
    @JsonSubTypes.Type(
            value = ImmutableDmDelete.class,
            names = {"1", "3"}),
    @JsonSubTypes.Type(value = ImmutableCategoryDelete.class, name = "4"),
    @JsonSubTypes.Type(
            value = ImmutableThreadDelete.class,
            names = {"10", "11", "12"})
})
public interface ChannelDeleteEvent extends ChannelEvent {}
