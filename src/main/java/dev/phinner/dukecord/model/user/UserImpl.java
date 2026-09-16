// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.user;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.Json;
import java.util.Optional;

final class UserImpl extends AbstractDiscordEntity implements User {

    private final String username;
    private final Optional<String> globalName;
    private final Optional<String> avatarHash;
    private final boolean bot;
    private final boolean system;

    UserImpl(final Dukecord duke, final Json raw) {
        super(duke, raw);
        this.username = raw.string("username");
        this.globalName = raw.optionalString("global_name");
        this.avatarHash = raw.optionalString("avatar");
        this.bot = raw.bool("bot", false);
        this.system = raw.bool("system", false);
    }

    @Override
    public String username() {
        return this.username;
    }

    @Override
    public Optional<String> globalName() {
        return this.globalName;
    }

    @Override
    public Optional<String> avatarHash() {
        return this.avatarHash;
    }

    @Override
    public boolean isBot() {
        return this.bot;
    }

    @Override
    public boolean isSystem() {
        return this.system;
    }
}
