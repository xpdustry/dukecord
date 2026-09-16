// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import java.util.Optional;
import java.util.Set;

final class GuildImpl extends AbstractDiscordEntity implements Guild {

    private final String name;
    private final DiscordEntityRef<Member> owner;
    private final Optional<String> description;
    private final Optional<String> iconHash;
    private final Set<String> features;
    private final boolean available;
    private final Optional<Integer> memberCount;
    private final Optional<Integer> boostCount;
    private final MemberCompleteness memberCompleteness;

    GuildImpl(
            final Dukecord duke, final Json raw, final boolean available, final MemberCompleteness memberCompleteness) {
        super(duke, raw);
        this.name = raw.string("name");
        this.owner = Members.ref(duke, this.id(), raw.snowflake("owner_id"));
        this.description = raw.optionalString("description");
        this.iconHash = raw.optionalString("icon");
        this.features = Set.copyOf(raw.strings("features"));
        this.available = available;
        this.memberCount = raw.optionalInteger("member_count");
        this.boostCount = raw.optionalInteger("premium_subscription_count");
        this.memberCompleteness = memberCompleteness;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DiscordEntityRef<Member> owner() {
        return this.owner;
    }

    @Override
    public Optional<String> description() {
        return this.description;
    }

    @Override
    public Optional<String> iconHash() {
        return this.iconHash;
    }

    @Override
    public Set<String> features() {
        return this.features;
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public Optional<Integer> memberCount() {
        return this.memberCount;
    }

    @Override
    public Optional<Integer> boostCount() {
        return this.boostCount;
    }

    @Override
    public MemberCompleteness memberCompleteness() {
        return this.memberCompleteness;
    }
}
