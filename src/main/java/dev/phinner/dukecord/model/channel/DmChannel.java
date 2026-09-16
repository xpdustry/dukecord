// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.user.User;
import java.util.List;

public non-sealed interface DmChannel extends MessageChannel {

    /** The other participants; may be empty when the channel came from a message payload. */
    List<User> recipients();
}
