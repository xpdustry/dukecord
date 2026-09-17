// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.message.Message;

/**
 * Messages sent through an interaction are not reachable here: ephemeral ones cannot be fetched at all, and the
 * original response needs the interaction token, so those live on the interaction handles.
 */
public interface MessageStore extends DiscordEntityStore.WithChannelLookup<Message> {}
