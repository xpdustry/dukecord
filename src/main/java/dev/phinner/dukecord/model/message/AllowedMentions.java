// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Which mentions in a message actually ping. Specs default to {@link #none()}; pinging is the explicit choice. */
public record AllowedMentions(Set<Parse> parse, Set<Snowflake> users, Set<Snowflake> roles, boolean repliedUser) {

    public enum Parse {
        USERS("users"),
        ROLES("roles"),
        EVERYONE("everyone");

        private final String key;

        Parse(final String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public AllowedMentions {
        parse = Set.copyOf(parse);
        users = Set.copyOf(users);
        roles = Set.copyOf(roles);
    }

    /** Nothing pings. */
    public static AllowedMentions none() {
        return new AllowedMentions(Set.of(), Set.of(), Set.of(), false);
    }

    /** Discord's default: every user, role and everyone mention pings. */
    public static AllowedMentions all() {
        return new AllowedMentions(EnumSet.allOf(Parse.class), Set.of(), Set.of(), true);
    }

    /** Only these users ping. */
    public static AllowedMentions users(final Collection<Snowflake> ids) {
        return new AllowedMentions(Set.of(), Set.copyOf(ids), Set.of(), false);
    }

    /** Only these roles ping. */
    public static AllowedMentions roles(final Collection<Snowflake> ids) {
        return new AllowedMentions(Set.of(), Set.of(), Set.copyOf(ids), false);
    }

    public AllowedMentions withUsers(final Collection<Snowflake> ids) {
        final var merged = new java.util.HashSet<>(this.users);
        merged.addAll(ids);
        return new AllowedMentions(this.parse, merged, this.roles, this.repliedUser);
    }

    public AllowedMentions withRoles(final Collection<Snowflake> ids) {
        final var merged = new java.util.HashSet<>(this.roles);
        merged.addAll(ids);
        return new AllowedMentions(this.parse, this.users, merged, this.repliedUser);
    }

    public AllowedMentions withEveryone() {
        final var merged = EnumSet.noneOf(Parse.class);
        merged.addAll(this.parse);
        merged.add(Parse.EVERYONE);
        return new AllowedMentions(merged, this.users, this.roles, this.repliedUser);
    }

    public AllowedMentions withRepliedUser(final boolean ping) {
        return new AllowedMentions(this.parse, this.users, this.roles, ping);
    }

    public Json toJson() {
        final List<String> parse = new ArrayList<>();
        for (final var type : Parse.values()) {
            if (this.parse.contains(type)) {
                parse.add(type.key());
            }
        }
        var json = Json.empty().with("parse", parse).with("replied_user", this.repliedUser);
        // Discord rejects a list next to the matching parse type; the parse type already means "all of them".
        if (!this.users.isEmpty() && !this.parse.contains(Parse.USERS)) {
            json = json.with(
                    "users", this.users.stream().map(Snowflake::asString).toList());
        }
        if (!this.roles.isEmpty() && !this.parse.contains(Parse.ROLES)) {
            json = json.with(
                    "roles", this.roles.stream().map(Snowflake::asString).toList());
        }
        return json;
    }
}
