// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Snowflake;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Pagination for message history. Discord returns at most 100 messages per call, newest first. */
public record MessageQuery(
        Optional<Snowflake> before, Optional<Snowflake> after, Optional<Snowflake> around, int limit) {

    public static final int MAX_LIMIT = 100;

    public MessageQuery {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    public static MessageQuery latest(final int limit) {
        return new MessageQuery(Optional.empty(), Optional.empty(), Optional.empty(), limit);
    }

    public static MessageQuery before(final Snowflake messageId, final int limit) {
        return new MessageQuery(Optional.of(messageId), Optional.empty(), Optional.empty(), limit);
    }

    public static MessageQuery after(final Snowflake messageId, final int limit) {
        return new MessageQuery(Optional.empty(), Optional.of(messageId), Optional.empty(), limit);
    }

    public static MessageQuery around(final Snowflake messageId, final int limit) {
        return new MessageQuery(Optional.empty(), Optional.empty(), Optional.of(messageId), limit);
    }

    public Map<String, String> toQueryParams() {
        final Map<String, String> params = new LinkedHashMap<>();
        this.before.ifPresent(id -> params.put("before", id.asString()));
        this.after.ifPresent(id -> params.put("after", id.asString()));
        this.around.ifPresent(id -> params.put("around", id.asString()));
        params.put("limit", Integer.toString(this.limit));
        return params;
    }
}
