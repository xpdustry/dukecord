// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Snowflake;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Either a paginated listing (requires the {@code GUILD_MEMBERS} intent) or a username/nickname prefix search.
 * Discord returns at most 1000 members per call.
 */
public record MemberQuery(Optional<String> search, Optional<Snowflake> after, int limit) {

    public static final int MAX_LIMIT = 1000;

    public MemberQuery {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    public static MemberQuery list(final int limit) {
        return new MemberQuery(Optional.empty(), Optional.empty(), limit);
    }

    public static MemberQuery after(final Snowflake userId, final int limit) {
        return new MemberQuery(Optional.empty(), Optional.of(userId), limit);
    }

    public static MemberQuery search(final String prefix, final int limit) {
        return new MemberQuery(Optional.of(prefix), Optional.empty(), limit);
    }

    public boolean isSearch() {
        return this.search.isPresent();
    }

    public Map<String, String> toQueryParams() {
        final Map<String, String> params = new LinkedHashMap<>();
        this.search.ifPresent(query -> params.put("query", query));
        this.after.ifPresent(id -> params.put("after", id.asString()));
        params.put("limit", Integer.toString(this.limit));
        return params;
    }
}
