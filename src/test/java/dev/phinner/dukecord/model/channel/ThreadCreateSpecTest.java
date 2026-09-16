// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadCreateSpecTest {

    private static final Snowflake TAG = Snowflake.of("41771983423143937");

    @Test
    void onlyTheNameIsWrittenByDefault() {
        final var spec = ThreadCreateSpec.builder("Bug report").build();
        assertThat(spec.payload()).isEqualTo(Json.parse("{\"name\": \"Bug report\"}"));
        assertThat(spec.message()).isEmpty();
        assertThat(spec.reason()).isEmpty();
    }

    @Test
    void autoArchiveDurationIsWrittenInMinutes() {
        final var spec = ThreadCreateSpec.builder("Bug report")
                .autoArchiveDuration(Duration.ofDays(1))
                .build();
        assertThat(spec.payload().integer("auto_archive_duration")).isEqualTo(1440);
        assertThat(ThreadCreateSpec.builder("t")
                        .autoArchiveDuration(Duration.ofMinutes(60))
                        .build()
                        .payload()
                        .integer("auto_archive_duration"))
                .isEqualTo(60);
    }

    @Test
    void privateThreadSwitchesTheChannelType() {
        assertThat(ThreadCreateSpec.builder("t")
                        .privateThread(true)
                        .build()
                        .payload()
                        .integer("type"))
                .isEqualTo(ChannelType.PRIVATE_THREAD.code());
        assertThat(ThreadCreateSpec.builder("t")
                        .privateThread(false)
                        .build()
                        .payload()
                        .integer("type"))
                .isEqualTo(ChannelType.PUBLIC_THREAD.code());
        assertThat(ThreadCreateSpec.builder("t").build().payload().has("type")).isFalse();
    }

    @Test
    void invitableAndRateLimitWriteTheirOwnKeys() {
        final var spec = ThreadCreateSpec.builder("t")
                .invitable(true)
                .rateLimitPerUser(Duration.ofSeconds(10))
                .build();
        assertThat(spec.payload().bool("invitable")).isTrue();
        assertThat(spec.payload().integer("rate_limit_per_user")).isEqualTo(10);
    }

    @Test
    void appliedTagsAreWrittenAsStringIds() {
        final var spec = ThreadCreateSpec.builder("t").appliedTags(List.of(TAG)).build();
        assertThat(spec.payload().strings("applied_tags")).containsExactly("41771983423143937");
        assertThat(ThreadCreateSpec.builder("t").build().payload().has("applied_tags"))
                .isFalse();
    }

    @Test
    void theStarterMessageLivesBesideThePayloadNotInsideIt() {
        final var message = MessageCreateSpec.builder().content("First post").build();
        final var spec = ThreadCreateSpec.builder("Bug report").message(message).build();

        assertThat(spec.message()).contains(message);
        assertThat(spec.payload().has("message")).isFalse();
        assertThat(spec.payload().asMap()).containsOnlyKeys("name");
    }

    @Test
    void reasonIsCarriedOutsideThePayload() {
        assertThat(ThreadCreateSpec.builder("t").reason("audit").build().reason())
                .contains("audit");
        assertThat(ThreadCreateSpec.builder("t").reason(null).build().reason()).isEmpty();
    }

    @Test
    void keysAccumulateAcrossCalls() {
        final var spec = ThreadCreateSpec.builder("Bug report")
                .autoArchiveDuration(Duration.ofMinutes(60))
                .privateThread(true)
                .invitable(false)
                .rateLimitPerUser(Duration.ofSeconds(5))
                .appliedTags(List.of(TAG))
                .build();
        assertThat(spec.payload().asMap())
                .containsOnlyKeys(
                        "name", "auto_archive_duration", "type", "invitable", "rate_limit_per_user", "applied_tags");
    }

    @Test
    void toStringShowsThePayload() {
        assertThat(ThreadCreateSpec.builder("Bug report").build().toString())
                .startsWith("ThreadCreateSpec{")
                .contains("\"name\":\"Bug report\"");
    }
}
