// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.Json;

final class UnknownChannelImpl extends AbstractChannel implements UnknownChannel {

    UnknownChannelImpl(final Dukecord duke, final Json raw) {
        super(duke, raw);
    }
}
