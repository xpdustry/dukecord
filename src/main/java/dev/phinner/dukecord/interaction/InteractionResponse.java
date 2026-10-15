// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.message.Message;
import dev.phinner.dukecord.message.MessageCreateSpec;
import dev.phinner.dukecord.message.MessageEditSpec;

/** The original response to an interaction, reachable through the interaction token for 15 minutes. */
public interface InteractionResponse {

    static InteractionResponse of(final Dukecord duke, final Snowflake applicationId, final String token) {
        return new InteractionResponseImpl(duke, applicationId, token);
    }

    Dukecord duke();

    Snowflake applicationId();

    String token();

    default Message fetch() {
        return this.duke().interactions().rest().fetchOriginal(this.applicationId(), this.token());
    }

    default Message edit(final MessageEditSpec spec) {
        return this.duke().interactions().rest().editOriginal(this.applicationId(), this.token(), spec);
    }

    default Message edit(final String content) {
        return this.edit(MessageEditSpec.of(content));
    }

    default void delete() {
        this.duke().interactions().rest().deleteOriginal(this.applicationId(), this.token());
    }

    default Message followUp(final MessageCreateSpec spec) {
        return this.duke().interactions().rest().createFollowUp(this.applicationId(), this.token(), spec);
    }

    default Message followUp(final String content) {
        return this.followUp(MessageCreateSpec.of(content));
    }
}
