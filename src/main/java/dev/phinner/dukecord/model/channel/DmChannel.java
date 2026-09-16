// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.user.ImmutableUser;
import dev.phinner.dukecord.model.user.User;
import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableDmChannel.class)
public non-sealed interface DmChannel extends MessageChannel {

    @JsonDeserialize(contentAs = ImmutableUser.class)
    List<User> recipients();
}
