// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.Permission;
import dev.phinner.dukecord.model.permission.PermissionOverwrite;
import dev.phinner.dukecord.model.permission.PermissionSet;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelCreateSpecTest {

    private static final Snowflake CATEGORY_ID = Snowflake.of("41771983423143937");
    private static final Snowflake EVERYONE_ID = Snowflake.of("41771983423143936");

    @Test
    void textFactoryWritesNameAndType() {
        final var spec = ChannelCreateSpec.text("general").build();
        assertThat(spec.type()).isEqualTo(ChannelType.GUILD_TEXT);
        assertThat(spec.payload()).isEqualTo(Json.parse("{\"name\": \"general\", \"type\": 0}"));
        assertThat(spec.reason()).isEmpty();
    }

    @Test
    void categoryFactoryWritesTheCategoryType() {
        final var spec = ChannelCreateSpec.category("Staff").build();
        assertThat(spec.type()).isEqualTo(ChannelType.GUILD_CATEGORY);
        assertThat(spec.payload().integer("type")).isEqualTo(4);
    }

    @Test
    void ofAcceptsAnyChannelType() {
        final var spec = ChannelCreateSpec.of("Voice", ChannelType.GUILD_VOICE).build();
        assertThat(spec.type()).isEqualTo(ChannelType.GUILD_VOICE);
        assertThat(spec.payload().string("name")).isEqualTo("Voice");
    }

    @Test
    void optionalKeysAreAbsentUntilSet() {
        assertThat(ChannelCreateSpec.text("general").build().payload().asMap()).containsOnlyKeys("name", "type");
    }

    @Test
    void eachSetterWritesItsOwnKey() {
        final var overwrite = new PermissionOverwrite(
                EVERYONE_ID,
                PermissionOverwrite.Type.ROLE,
                PermissionSet.NONE,
                PermissionSet.of(Permission.VIEW_CHANNEL));

        final var spec = ChannelCreateSpec.text("general")
                .topic("The general channel")
                .position(3)
                .category(CATEGORY_ID)
                .nsfw(true)
                .rateLimitPerUser(Duration.ofSeconds(30))
                .overwrites(List.of(overwrite))
                .reason("audit")
                .build();

        assertThat(spec.payload().string("topic")).isEqualTo("The general channel");
        assertThat(spec.payload().integer("position")).isEqualTo(3);
        assertThat(spec.payload().string("parent_id")).isEqualTo("41771983423143937");
        assertThat(spec.payload().bool("nsfw")).isTrue();
        assertThat(spec.payload().integer("rate_limit_per_user")).isEqualTo(30);
        assertThat(spec.payload().objects("permission_overwrites")).containsExactly(overwrite.toJson());
        assertThat(spec.reason()).contains("audit");
        assertThat(spec.payload().has("reason")).isFalse();
    }

    @Test
    void rateLimitIsTruncatedToWholeSeconds() {
        final var spec = ChannelCreateSpec.text("general")
                .rateLimitPerUser(Duration.ofMillis(1500))
                .build();
        assertThat(spec.payload().integer("rate_limit_per_user")).isEqualTo(1);
        assertThat(ChannelCreateSpec.text("g")
                        .rateLimitPerUser(Duration.ZERO)
                        .build()
                        .payload()
                        .integer("rate_limit_per_user"))
                .isZero();
    }

    @Test
    void overwritesReplaceRatherThanAppend() {
        final var first = new PermissionOverwrite(
                EVERYONE_ID, PermissionOverwrite.Type.ROLE, PermissionSet.NONE, PermissionSet.NONE);
        final var second = new PermissionOverwrite(
                CATEGORY_ID, PermissionOverwrite.Type.MEMBER, PermissionSet.ALL, PermissionSet.NONE);

        final var spec = ChannelCreateSpec.text("general")
                .overwrites(List.of(first))
                .overwrites(List.of(second))
                .build();
        assertThat(spec.payload().objects("permission_overwrites")).containsExactly(second.toJson());
    }

    @Test
    void reasonIsNullable() {
        assertThat(ChannelCreateSpec.text("g").reason(null).build().reason()).isEmpty();
    }

    @Test
    void toStringShowsThePayload() {
        assertThat(ChannelCreateSpec.text("general").build().toString())
                .startsWith("ChannelCreateSpec{")
                .contains("\"name\":\"general\"");
    }
}
