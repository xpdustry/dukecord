// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.message.Message;
import dev.phinner.dukecord.message.MessageCreateSpec;
import dev.phinner.dukecord.message.MessageEditSpec;
import java.util.List;

/**
 * The first group acknowledges an interaction, once, within 3 seconds. The second reaches its responses through the
 * token for 15 minutes.
 */
public interface InteractionRestOperations {

    void reply(final Snowflake interactionId, final String token, final MessageCreateSpec spec);

    void deferReply(final Snowflake interactionId, final String token, final boolean ephemeral);

    void update(final Snowflake interactionId, final String token, final MessageEditSpec spec);

    void deferUpdate(final Snowflake interactionId, final String token);

    void showModal(final Snowflake interactionId, final String token, final ModalCreateSpec modal);

    void suggest(final Snowflake interactionId, final String token, final List<CommandOption.Choice> choices);

    Message fetchOriginal(final Snowflake applicationId, final String token);

    Message editOriginal(final Snowflake applicationId, final String token, final MessageEditSpec spec);

    void deleteOriginal(final Snowflake applicationId, final String token);

    Message createFollowUp(final Snowflake applicationId, final String token, final MessageCreateSpec spec);
}
