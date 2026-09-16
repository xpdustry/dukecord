// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.user;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.channel.DmChannel;
import java.util.Optional;

public interface User extends DiscordEntity {

    String username();

    Optional<String> globalName();

    Optional<String> avatarHash();

    /** Absent in the payload means false. */
    boolean isBot();

    boolean isSystem();

    default String displayName() {
        return this.globalName().orElseGet(this::username);
    }

    default String mention() {
        return "<@" + this.id() + ">";
    }

    /** REST: opens (or returns the existing) direct-message channel with this user. */
    default DmChannel openDm() {
        return this.duke().rest().openDm(this.id());
    }
}
