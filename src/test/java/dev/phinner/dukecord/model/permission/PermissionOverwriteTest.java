// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionOverwriteTest {

    @Test
    void typeCodesMapBothWays() {
        assertThat(PermissionOverwrite.Type.ROLE.code()).isZero();
        assertThat(PermissionOverwrite.Type.MEMBER.code()).isEqualTo(1);
        assertThat(PermissionOverwrite.Type.fromCode(0)).isEqualTo(PermissionOverwrite.Type.ROLE);
        assertThat(PermissionOverwrite.Type.fromCode(1)).isEqualTo(PermissionOverwrite.Type.MEMBER);
        // Anything unknown falls back to a role overwrite rather than throwing.
        assertThat(PermissionOverwrite.Type.fromCode(7)).isEqualTo(PermissionOverwrite.Type.ROLE);
    }

    @Test
    void readsARoleOverwriteFromJson() {
        final var overwrite = PermissionOverwrite.fromJson(Json.parse("""
                        {"id": "41771983423143937", "type": 0, "allow": "2048", "deny": "1024"}"""));
        assertThat(overwrite.id()).isEqualTo(Snowflake.of("41771983423143937"));
        assertThat(overwrite.type()).isEqualTo(PermissionOverwrite.Type.ROLE);
        assertThat(overwrite.allow()).isEqualTo(PermissionSet.of(Permission.SEND_MESSAGES));
        assertThat(overwrite.deny()).isEqualTo(PermissionSet.of(Permission.VIEW_CHANNEL));
    }

    @Test
    void readsAMemberOverwriteFromJson() {
        final var overwrite = PermissionOverwrite.fromJson(Json.parse("""
                        {"id": "302096570298990593", "type": 1, "allow": "0", "deny": "0"}"""));
        assertThat(overwrite.type()).isEqualTo(PermissionOverwrite.Type.MEMBER);
        assertThat(overwrite.allow()).isEqualTo(PermissionSet.NONE);
        assertThat(overwrite.deny()).isEqualTo(PermissionSet.NONE);
    }

    @Test
    void roundTripsThroughJson() {
        final var json = Json.parse("""
                {"id": "41771983423143937", "type": 1, "allow": "3072", "deny": "8796093022208"}""");
        final var overwrite = PermissionOverwrite.fromJson(json);
        assertThat(overwrite.toJson()).isEqualTo(json);
        assertThat(PermissionOverwrite.fromJson(overwrite.toJson())).isEqualTo(overwrite);
    }

    @Test
    void writesIdsAndSetsAsStrings() {
        final var overwrite = new PermissionOverwrite(
                Snowflake.of("41771983423143937"),
                PermissionOverwrite.Type.ROLE,
                PermissionSet.of(Permission.VIEW_CHANNEL),
                PermissionSet.of(Permission.SEND_MESSAGES));
        assertThat(overwrite.toJson()).isEqualTo(Json.parse("""
                        {"id": "41771983423143937", "type": 0, "allow": "1024", "deny": "2048"}"""));
    }
}
