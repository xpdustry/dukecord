// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.StringJoiner;

public final class GatewayIntentSet {

    public static final GatewayIntentSet ALL = new GatewayIntentSet().with(GatewayIntent.values());

    public static final GatewayIntentSet PRIVILEGED = new GatewayIntentSet()
            .with(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);

    public static final GatewayIntentSet UN_PRIVILEGED =
            new GatewayIntentSet().with(ALL).without(PRIVILEGED);

    private final EnumSet<GatewayIntent> set = EnumSet.noneOf(GatewayIntent.class);

    public GatewayIntentSet with(final GatewayIntent... intents) {
        return this.with(Arrays.asList(intents));
    }

    public GatewayIntentSet with(final GatewayIntentSet intents) {
        return this.with(intents.set);
    }

    public GatewayIntentSet with(final Collection<GatewayIntent> intents) {
        final var set = new GatewayIntentSet();
        set.set.addAll(this.set);
        set.set.addAll(intents);
        return set;
    }

    public GatewayIntentSet without(final GatewayIntent... intents) {
        return this.without(Arrays.asList(intents));
    }

    public GatewayIntentSet without(final GatewayIntentSet intents) {
        return this.without(intents.set);
    }

    public GatewayIntentSet without(final Collection<GatewayIntent> intents) {
        final var set = new GatewayIntentSet();
        set.set.addAll(this.set);
        set.set.removeAll(intents);
        return set;
    }

    public int mask() {
        return this.set.stream().mapToInt(GatewayIntent::mask).reduce(0, (a, b) -> a | b);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GatewayIntentSet.class.getSimpleName() + "[", "]")
                .add("set=" + this.set)
                .toString();
    }
}
