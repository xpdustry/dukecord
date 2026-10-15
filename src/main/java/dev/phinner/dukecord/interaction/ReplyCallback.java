// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.message.MessageCreateSpec;

/** An interaction answered with a new message. Each interaction is acknowledged once, by any callback. */
public interface ReplyCallback extends InteractionBehavior {

    default InteractionResponse reply(final MessageCreateSpec spec) {
        this.duke().interactions().rest().reply(this.id(), this.token(), spec);
        return this.response();
    }

    default InteractionResponse reply(final String content) {
        return this.reply(MessageCreateSpec.of(content));
    }

    /** Shows a loading state. Send the message later with {@link InteractionResponse#edit}. */
    default InteractionResponse deferReply() {
        return this.deferReply(false);
    }

    default InteractionResponse deferReply(final boolean ephemeral) {
        this.duke().interactions().rest().deferReply(this.id(), this.token(), ephemeral);
        return this.response();
    }
}
