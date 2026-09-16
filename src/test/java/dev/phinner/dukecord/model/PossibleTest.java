// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PossibleTest {

    @Test
    void absentIsNeitherNullNorPresent() {
        final Possible<String> absent = Possible.absent();
        assertThat(absent.isAbsent()).isTrue();
        assertThat(absent.isNull()).isFalse();
        assertThat(absent.isPresent()).isFalse();
        assertThat(absent).isInstanceOf(Possible.Absent.class);
    }

    @Test
    void nullIsNeitherAbsentNorPresent() {
        final Possible<String> nulled = Possible.ofNull();
        assertThat(nulled.isAbsent()).isFalse();
        assertThat(nulled.isNull()).isTrue();
        assertThat(nulled.isPresent()).isFalse();
        assertThat(nulled).isInstanceOf(Possible.Null.class);
    }

    @Test
    void presentCarriesItsValue() {
        final var present = Possible.of("nickname");
        assertThat(present.isAbsent()).isFalse();
        assertThat(present.isNull()).isFalse();
        assertThat(present.isPresent()).isTrue();
        assertThat(present).isEqualTo(new Possible.Present<>("nickname"));
        assertThat(((Possible.Present<String>) present).value()).isEqualTo("nickname");
    }

    @Test
    void absentAndNullAreSingletons() {
        assertThat(Possible.<String>absent()).isSameAs(Possible.<Integer>absent());
        assertThat(Possible.<String>ofNull()).isSameAs(Possible.<Integer>ofNull());
        assertThat(Possible.<String>absent()).isNotSameAs(Possible.<String>ofNull());
    }

    @Test
    void toOptionalIsEmptyForBothAbsentAndNull() {
        assertThat(Possible.of("x").toOptional()).contains("x");
        assertThat(Possible.absent().toOptional()).isEmpty();
        assertThat(Possible.ofNull().toOptional()).isEmpty();
    }

    @Test
    void mapAppliesOnlyToPresentAndPreservesTheOtherStates() {
        assertThat(Possible.of("abc").map(String::length)).isEqualTo(Possible.of(3));
        assertThat(Possible.<String>absent().map(String::length)).isEqualTo(Possible.absent());
        assertThat(Possible.<String>ofNull().map(String::length)).isEqualTo(Possible.ofNull());
    }

    @Test
    void ofOptionalMapsEmptyToAbsent() {
        assertThat(Possible.ofOptional(Optional.of("x"))).isEqualTo(Possible.of("x"));
        assertThat(Possible.ofOptional(Optional.empty())).isEqualTo(Possible.absent());
        assertThat(Possible.ofOptional(Optional.<String>empty())).isSameAs(Possible.<String>absent());
    }

    @Test
    void statesAreRecordsWithValueEquality() {
        assertThat(new Possible.Absent<String>()).isEqualTo(Possible.<String>absent());
        assertThat(new Possible.Null<String>()).isEqualTo(Possible.<String>ofNull());
        assertThat(new Possible.Present<>("a")).isEqualTo(new Possible.Present<>("a"));
        assertThat(new Possible.Present<>("a")).hasSameHashCodeAs(new Possible.Present<>("a"));
        assertThat(new Possible.Present<>("a")).isNotEqualTo(new Possible.Present<>("b"));
        assertThat(new Possible.Present<>("a")).hasToString("Present[value=a]");
    }
}
