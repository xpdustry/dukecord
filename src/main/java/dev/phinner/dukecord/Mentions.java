// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

/**
 * The markup Discord renders as mentions. Messages mention nobody unless their allowed mentions say so, so a mention
 * that should ping needs both this text and a matching {@code AllowedMentions}.
 */
public final class Mentions {

    public static final String EVERYONE = "@everyone";

    public static final String HERE = "@here";

    private Mentions() {}

    public static String user(final Snowflake id) {
        return "<@" + id.asString() + ">";
    }

    public static String role(final Snowflake id) {
        return "<@&" + id.asString() + ">";
    }

    public static String channel(final Snowflake id) {
        return "<#" + id.asString() + ">";
    }

    /** A clickable slash command, where {@code path} holds the subcommands, as in {@code "map search"}. */
    public static String command(final String path, final Snowflake id) {
        return "</" + path + ":" + id.asString() + ">";
    }

    public static String emoji(final Emoji emoji) {
        return switch (emoji) {
            case UnicodeEmoji unicode -> unicode.name();
            case CustomEmoji custom ->
                (custom.animated() ? "<a:" : "<:") + custom.name().orElse("_") + ":"
                        + custom.id().asString() + ">";
        };
    }
}
