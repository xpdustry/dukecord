// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

/**
 * The message a reply points at. Three-valued by Discord's design: the field is absent when the referenced message
 * was not fetched ({@link Unknown}), present-and-null when it was deleted ({@link Deleted}), or a message.
 */
public sealed interface ReferencedMessage {

    record Unknown() implements ReferencedMessage {}

    record Deleted() implements ReferencedMessage {}

    record Present(Message message) implements ReferencedMessage {}

    ReferencedMessage UNKNOWN = new Unknown();
    ReferencedMessage DELETED = new Deleted();
}
