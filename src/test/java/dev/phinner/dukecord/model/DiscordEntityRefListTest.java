// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;
import dev.phinner.dukecord.support.TestDukecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordEntityRefListTest {

    private static final Snowflake FIRST = Snowflake.of("100");
    private static final Snowflake SECOND = Snowflake.of("200");
    private static final Snowflake THIRD = Snowflake.of("300");

    private final TestDukecord duke = new TestDukecord();

    private User user(final Snowflake id) {
        return Users.fromJson(this.duke, Json.parse("""
                        {"id": "%s", "username": "user-%s"}""".formatted(id.asString(), id.asString())));
    }

    private DiscordEntityRefList<User> list(final Collection<Snowflake> ids, final Collection<User> cached) {
        return DiscordEntityRefList.of(
                User.class,
                ids,
                id -> cached.stream().filter(user -> user.id().equals(id)).findFirst(),
                id -> {
                    throw new AssertionError("fetch() must not be called");
                });
    }

    @Test
    void exposesItsTargetTypeIdsAndSize() {
        final var list = this.list(List.of(FIRST, SECOND), List.of());

        assertThat(list.type()).isEqualTo(User.class);
        assertThat(list.ids()).containsExactly(FIRST, SECOND);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.isEmpty()).isFalse();
        assertThat(list.contains(FIRST)).isTrue();
        assertThat(list.contains(THIRD)).isFalse();
    }

    @Test
    void isEmptyWithoutAnyId() {
        final var list = this.list(List.of(), List.of());

        assertThat(list.isEmpty()).isTrue();
        assertThat(list.size()).isZero();
        assertThat(list.ids()).isEmpty();
        assertThat(list.refs()).isEmpty();
        assertThat(list.get()).isEmpty();
        assertThat(list.contains(FIRST)).isFalse();
    }

    @Test
    void idsKeepTheirOrderAndDropDuplicates() {
        final var list = this.list(List.of(THIRD, FIRST, THIRD, SECOND), List.of());

        assertThat(list.ids()).containsExactly(THIRD, FIRST, SECOND);
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    void idsAreAnUnmodifiableSequencedSet() {
        final SequencedSet<Snowflake> ids =
                this.list(List.of(FIRST, SECOND), List.of()).ids();

        assertThat(ids.getFirst()).isEqualTo(FIRST);
        assertThat(ids.getLast()).isEqualTo(SECOND);
        assertThatThrownBy(() -> ids.add(THIRD)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ids.remove(FIRST)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void idsAreDetachedFromTheSourceCollection() {
        final var source = new ArrayList<>(List.of(FIRST));
        final var list = this.list(source, List.of());
        source.add(SECOND);

        assertThat(list.ids()).containsExactly(FIRST);
    }

    @Test
    void refsAreOneRefPerIdInOrder() {
        final var list = this.list(List.of(FIRST, SECOND), List.of());

        assertThat(list.refs()).hasSize(2).extracting(DiscordEntityRef::id).containsExactly(FIRST, SECOND);
        assertThat(list.refs().getFirst().type()).isEqualTo(User.class);
    }

    @Test
    void refsResolveThroughTheSameResolver() {
        final var first = this.user(FIRST);
        final var list = this.list(List.of(FIRST, SECOND), List.of(first));

        assertThat(list.refs().getFirst().get()).contains(first);
        assertThat(list.refs().getLast().get()).isEmpty();
    }

    @Test
    void getReturnsTheCachedSnapshotsInIdOrderAndOmitsMisses() {
        final var first = this.user(FIRST);
        final var third = this.user(THIRD);
        final var list = this.list(List.of(THIRD, SECOND, FIRST), List.of(first, third));

        assertThat(list.get()).containsExactly(third, first);
    }

    @Test
    void fetchWithoutABatchFetcherCallsTheFetcherOncePerIdInOrder() {
        final var seen = new ArrayList<Snowflake>();
        final var list =
                DiscordEntityRefList.of(User.class, List.of(SECOND, FIRST), id -> Optional.<User>empty(), id -> {
                    seen.add(id);
                    return this.user(id);
                });

        assertThat(list.fetch()).extracting(User::id).containsExactly(SECOND, FIRST);
        assertThat(seen).containsExactly(SECOND, FIRST);
    }

    @Test
    void fetchWithABatchFetcherIssuesASingleCallWithEveryId() {
        final var batches = new ArrayList<Collection<Snowflake>>();
        final var list = DiscordEntityRefList.of(
                User.class,
                List.of(FIRST, SECOND),
                id -> Optional.<User>empty(),
                id -> {
                    throw new AssertionError("the per-id fetcher must not be called");
                },
                ids -> {
                    batches.add(ids);
                    return ids.stream().map(this::user).toList();
                });

        assertThat(list.fetch()).extracting(User::id).containsExactly(FIRST, SECOND);
        assertThat(batches).hasSize(1);
        assertThat(batches.getFirst()).containsExactly(FIRST, SECOND);
    }

    @Test
    void equalsByTargetTypeAndIdsOnly() {
        final var one = this.list(List.of(FIRST, SECOND), List.of(this.user(FIRST)));
        final var other = this.list(List.of(FIRST, SECOND), List.of());

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(one).isNotEqualTo(this.list(List.of(FIRST), List.of()));
    }

    @Test
    void differsFromAListOfAnotherTypeWithTheSameIds() {
        final DiscordEntityRefList<Guild> guilds =
                DiscordEntityRefList.of(Guild.class, List.of(FIRST), id -> Optional.empty(), id -> {
                    throw new AssertionError("fetch() must not be called");
                });

        assertThat(this.list(List.of(FIRST), List.of())).isNotEqualTo(guilds);
    }

    @Test
    void toStringNamesTheTargetTypeAndTheIds() {
        assertThat(this.list(List.of(FIRST, SECOND), List.of())).hasToString("Refs<User>[100, 200]");
    }

    @Test
    void getReadsTheLiveCacheWithoutTouchingTheRestFacade() {
        final var list = DiscordEntityRefList.of(
                User.class, List.of(FIRST, SECOND), this.duke.cache()::user, this.duke.rest()::fetchUser);

        assertThat(list.get()).isEmpty();

        final var cached = this.duke.putUser(Json.parse("""
                {"id": "200", "username": "phinner"}"""));

        assertThat(list.get()).containsExactly(cached);
        assertThat(this.duke.restCalls()).isEmpty();
    }
}
