// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.message.MessageEditSpec;

/** An interaction on a message, which can be answered by editing that message. */
public interface UpdateCallback extends InteractionBehavior {

    default InteractionResponse update(final MessageEditSpec spec) {
        this.duke().interactions().rest().update(this.id(), this.token(), spec);
        return this.response();
    }

    /** Acknowledges without a loading state. Edit the message later with {@link InteractionResponse#edit}. */
    default InteractionResponse deferUpdate() {
        this.duke().interactions().rest().deferUpdate(this.id(), this.token());
        return this.response();
    }
}
