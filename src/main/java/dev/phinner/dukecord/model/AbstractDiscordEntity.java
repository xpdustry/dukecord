// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import dev.phinner.dukecord.Dukecord;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Base class for entity snapshots: holds the client back-reference (excluded from equality) and the frozen raw JSON. */
public abstract class AbstractDiscordEntity implements DiscordEntity {

    private final Dukecord duke;
    private final Json raw;
    private final Snowflake id;

    protected AbstractDiscordEntity(final Dukecord duke, final Json raw) {
        this(duke, raw, Objects.requireNonNull(raw, "raw").snowflake("id"));
    }

    protected AbstractDiscordEntity(final Dukecord duke, final Json raw, final Snowflake id) {
        this.duke = Objects.requireNonNull(duke, "duke");
        this.raw = Objects.requireNonNull(raw, "raw");
        this.id = Objects.requireNonNull(id, "id");
    }

    @Override
    public final Dukecord duke() {
        return this.duke;
    }

    @Override
    public final Snowflake id() {
        return this.id;
    }

    @Override
    public final Json raw() {
        return this.raw;
    }

    @Override
    @SuppressWarnings("EqualsGetClass") // two entity kinds with identical raw JSON must not be equal
    public final boolean equals(final @Nullable Object other) {
        return other != null
                && other.getClass() == this.getClass()
                && ((AbstractDiscordEntity) other).raw.equals(this.raw);
    }

    @Override
    public final int hashCode() {
        return this.raw.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName().replace("Impl", "") + "[" + this.id + "]";
    }
}
