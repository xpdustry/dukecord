// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.user.User;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public non-sealed interface DmChannel extends MessageChannel {

    List<User> recipients();
}
