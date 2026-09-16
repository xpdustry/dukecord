// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;

/** A channel permission overwrite for a role or a member. */
public record PermissionOverwrite(Snowflake id, Type type, PermissionSet allow, PermissionSet deny) {

    public enum Type {
        ROLE(0),
        MEMBER(1);

        private final int code;

        Type(final int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }

        public static Type fromCode(final int code) {
            return code == 1 ? MEMBER : ROLE;
        }
    }

    public static PermissionOverwrite fromJson(final Json json) {
        return new PermissionOverwrite(
                json.snowflake("id"),
                Type.fromCode(json.integer("type")),
                PermissionSet.fromString(json.string("allow")),
                PermissionSet.fromString(json.string("deny")));
    }

    public Json toJson() {
        return Json.empty()
                .with("id", this.id.asString())
                .with("type", this.type.code())
                .with("allow", this.allow.asString())
                .with("deny", this.deny.asString());
    }
}
