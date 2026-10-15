// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Snowflake;
import java.io.InputStream;
import java.util.Collection;

public interface MessageRestOperations {

    Message fetch(final Snowflake channelId, final Snowflake messageId);

    Message create(final Snowflake channelId, final MessageCreateSpec spec);

    /** Only the author can change content, embeds and files; anyone allowed to manage messages can change flags. */
    Message edit(final Snowflake channelId, final Snowflake messageId, final MessageEditSpec spec);

    void delete(final Snowflake channelId, final Snowflake messageId);

    /** Opens the file of an attachment from Discord's CDN. The caller closes the stream. */
    InputStream download(final Attachment attachment);

    void delete(final Snowflake channelId, final Snowflake messageId, final String reason);

    /** Takes 2 to 100 messages, none older than 14 days. */
    void bulkDelete(final Snowflake channelId, final Collection<Snowflake> messageIds);

    void bulkDelete(final Snowflake channelId, final Collection<Snowflake> messageIds, final String reason);
}
