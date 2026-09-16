// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordEntityRefTest {

    private static final Snowflake ID = Snowflake.of("159985870458322944");
    private static final Snowflake OTHER_ID = Snowflake.of("266604749638606850");

    private final TestDukecord duke = new TestDukecord();

    private User user(final Snowflake id) {
        return Users.fromJson(this.duke, Json.parse("""
                        {"id": "%s", "username": "phinner", "global_name": "Phinner"}""".formatted(id.asString())));
    }

    private DiscordEntityRef<User> ref(final Snowflake id, final Optional<User> cached) {
        return DiscordEntityRef.of(User.class, id, ignored -> cached, ignored -> {
            throw new AssertionError("fetch() must not be called");
        });
    }

    @Test
    void exposesItsTargetTypeAndId() {
        final var ref = this.ref(ID, Optional.empty());

        assertThat(ref.type()).isEqualTo(User.class);
        assertThat(ref.id()).isEqualTo(ID);
    }

    @Test
    void getReadsThroughTheResolverWithTheRefId() {
        final var user = this.user(ID);
        final var seen = new ArrayList<Snowflake>();
        final var ref = DiscordEntityRef.of(
                User.class,
                ID,
                id -> {
                    seen.add(id);
                    return Optional.of(user);
                },
                id -> {
                    throw new AssertionError("fetch() must not be called");
                });

        assertThat(ref.get()).contains(user);
        assertThat(seen).containsExactly(ID);
    }

    @Test
    void getIsEmptyWhenTheResolverKnowsNothing() {
        assertThat(this.ref(ID, Optional.empty()).get()).isEmpty();
    }

    @Test
    void getReadsTheResolverAgainOnEveryCall() {
        final var user = this.user(ID);
        final var cached = new ArrayList<User>();
        final var ref = DiscordEntityRef.of(
                User.class,
                ID,
                id -> cached.isEmpty() ? Optional.<User>empty() : Optional.of(cached.getFirst()),
                id -> {
                    throw new AssertionError("fetch() must not be called");
                });

        assertThat(ref.get()).isEmpty();
        cached.add(user);
        assertThat(ref.get()).contains(user);
    }

    @Test
    void fetchGoesThroughTheFetcherWithTheRefId() {
        final var user = this.user(ID);
        final var seen = new ArrayList<Snowflake>();
        final var ref = DiscordEntityRef.of(User.class, ID, id -> Optional.empty(), id -> {
            seen.add(id);
            return user;
        });

        assertThat(ref.fetch()).isSameAs(user);
        assertThat(seen).containsExactly(ID);
    }

    @Test
    void fetchPropagatesTheFetcherFailure() {
        final var ref = DiscordEntityRef.of(User.class, ID, id -> Optional.empty(), id -> {
            throw new IllegalStateException("boom");
        });

        assertThatThrownBy(ref::fetch).isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    @Test
    void equalsByTargetTypeAndIdOnly() {
        final var one = this.ref(ID, Optional.of(this.user(ID)));
        final var other = this.ref(ID, Optional.empty());

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    }

    @Test
    void differsFromARefToAnotherId() {
        assertThat(this.ref(ID, Optional.empty())).isNotEqualTo(this.ref(OTHER_ID, Optional.empty()));
    }

    @Test
    void differsFromARefOfAnotherTypeWithTheSameId() {
        final DiscordEntityRef<Guild> guildRef = DiscordEntityRef.of(Guild.class, ID, id -> Optional.empty(), id -> {
            throw new AssertionError("fetch() must not be called");
        });

        assertThat(this.ref(ID, Optional.empty())).isNotEqualTo(guildRef);
    }

    @Test
    void differsFromUnrelatedValues() {
        assertThat(this.ref(ID, Optional.empty())).isNotEqualTo(ID).isNotEqualTo(null);
    }

    @Test
    void toStringNamesTheTargetTypeAndTheId() {
        assertThat(this.ref(ID, Optional.empty())).hasToString("Ref<User>[159985870458322944]");
    }

    @Test
    void getReadsTheLiveCacheWithoutTouchingTheRestFacade() {
        final var ref = DiscordEntityRef.of(User.class, ID, this.duke.cache()::user, this.duke.rest()::fetchUser);

        assertThat(ref.get()).isEmpty();

        final var cached = this.duke.putUser(Json.parse("""
                {"id": "159985870458322944", "username": "phinner"}"""));

        assertThat(ref.get()).contains(cached);
        assertThat(this.duke.restCalls()).isEmpty();
    }
}
