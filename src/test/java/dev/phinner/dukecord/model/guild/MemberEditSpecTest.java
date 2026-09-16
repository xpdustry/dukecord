// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberEditSpecTest {

    private static final Snowflake ROLE = Snowflake.of("41771983423143937");
    private static final Snowflake OTHER_ROLE = Snowflake.of("41771983423143938");
    private static final Snowflake VOICE_CHANNEL = Snowflake.of("175928847299117063");

    @Test
    void anUntouchedBuilderYieldsAnEmptyPayload() {
        final var spec = MemberEditSpec.builder().build();
        assertThat(spec.payload()).isEqualTo(Json.empty());
        assertThat(spec.reason()).isEmpty();
    }

    @Test
    void nicknameIsSetOrClearedWithAPresentNull() {
        assertThat(MemberEditSpec.builder()
                        .nickname("phinner")
                        .build()
                        .payload()
                        .string("nick"))
                .isEqualTo("phinner");

        final var cleared = MemberEditSpec.builder().clearNickname().build();
        assertThat(cleared.payload().has("nick")).isTrue();
        assertThat(cleared.payload().isNull("nick")).isTrue();
    }

    @Test
    void rolesReplaceTheWholeSetAsStringIds() {
        final var spec =
                MemberEditSpec.builder().roles(List.of(ROLE, OTHER_ROLE)).build();
        assertThat(spec.payload().strings("roles")).containsExactly("41771983423143937", "41771983423143938");
        assertThat(MemberEditSpec.builder().roles(List.of()).build().payload().list("roles"))
                .isEmpty();
    }

    @Test
    void timeoutIsAnIsoTimestampOrAPresentNull() {
        final var until = Instant.parse("2026-09-15T10:30:00Z");
        assertThat(MemberEditSpec.builder()
                        .timeoutUntil(until)
                        .build()
                        .payload()
                        .instant("communication_disabled_until"))
                .isEqualTo(until);

        final var cleared = MemberEditSpec.builder().clearTimeout().build();
        assertThat(cleared.payload().has("communication_disabled_until")).isTrue();
        assertThat(cleared.payload().isNull("communication_disabled_until")).isTrue();
    }

    @Test
    void muteAndDeafAreWrittenEvenWhenFalse() {
        final var spec = MemberEditSpec.builder().mute(false).deaf(true).build();
        assertThat(spec.payload().bool("mute")).isFalse();
        assertThat(spec.payload().bool("deaf")).isTrue();
        assertThat(MemberEditSpec.builder().build().payload().has("mute")).isFalse();
    }

    @Test
    void voiceChannelIsSetOrClearedToDisconnect() {
        assertThat(MemberEditSpec.builder()
                        .voiceChannel(VOICE_CHANNEL)
                        .build()
                        .payload()
                        .string("channel_id"))
                .isEqualTo("175928847299117063");

        final var disconnected = MemberEditSpec.builder().disconnectVoice().build();
        assertThat(disconnected.payload().has("channel_id")).isTrue();
        assertThat(disconnected.payload().isNull("channel_id")).isTrue();
    }

    @Test
    void reasonIsCarriedOutsideThePayload() {
        final var spec =
                MemberEditSpec.builder().nickname("phinner").reason("audit").build();
        assertThat(spec.reason()).contains("audit");
        assertThat(spec.payload().asMap()).containsOnlyKeys("nick");
        assertThat(MemberEditSpec.builder().reason(null).build().reason()).isEmpty();
    }

    @Test
    void keysAccumulateAcrossCalls() {
        final var spec = MemberEditSpec.builder()
                .nickname("phinner")
                .roles(List.of(ROLE))
                .mute(true)
                .deaf(false)
                .timeoutUntil(Instant.parse("2026-09-15T10:30:00Z"))
                .voiceChannel(VOICE_CHANNEL)
                .build();
        assertThat(spec.payload().asMap())
                .containsOnlyKeys("nick", "roles", "mute", "deaf", "communication_disabled_until", "channel_id");
    }

    @Test
    void toStringShowsThePayload() {
        assertThat(MemberEditSpec.builder().nickname("phinner").build().toString())
                .startsWith("MemberEditSpec{")
                .contains("\"nick\":\"phinner\"");
    }
}
