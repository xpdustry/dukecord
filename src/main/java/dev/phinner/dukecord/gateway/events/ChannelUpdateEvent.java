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
        defaultImpl = ImmutableUnknownUpdate.class)
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = ImmutableGuildTextUpdate.class,
            names = {"0", "5"}),
    @JsonSubTypes.Type(
            value = ImmutableDmUpdate.class,
            names = {"1", "3"}),
    @JsonSubTypes.Type(value = ImmutableCategoryUpdate.class, name = "4"),
    @JsonSubTypes.Type(
            value = ImmutableThreadUpdate.class,
            names = {"10", "11", "12"})
})
public interface ChannelUpdateEvent extends ChannelEvent {}
