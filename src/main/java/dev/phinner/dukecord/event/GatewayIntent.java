// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.event;

public enum GatewayIntent {
    GUILDS(0),
    GUILD_MEMBERS(1),
    GUILD_MODERATION(2),
    GUILD_EXPRESSIONS(3),
    GUILD_INTEGRATIONS(4),
    GUILD_WEBHOOKS(5),
    GUILD_INVITES(6),
    GUILD_VOICE_STATES(7),
    GUILD_PRESENCES(8),
    GUILD_MESSAGES(9),
    GUILD_MESSAGE_REACTIONS(10),
    GUILD_MESSAGE_TYPING(11),
    MESSAGE_CONTENT(15),
    DIRECT_MESSAGES(12),
    DIRECT_MESSAGE_REACTIONS(13),
    DIRECT_MESSAGE_TYPING(14),
    GUILD_SCHEDULED_EVENTS(16),
    AUTO_MODERATION_CONFIGURATION(20),
    AUTO_MODERATION_EXECUTION(21),
    GUILD_MESSAGE_POLLS(24),
    DIRECT_MESSAGE_POLLS(25);

    private final int mask;

    GatewayIntent(final int bit) {
        this.mask = 1 << bit;
    }

    public int mask() {
        return this.mask;
    }
}
