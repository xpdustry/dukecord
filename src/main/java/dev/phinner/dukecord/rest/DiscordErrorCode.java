// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.rest;

/** Discord JSON error codes bots commonly test against. See the "Opcodes and Status Codes" documentation. */
public final class DiscordErrorCode {

    public static final int GENERAL_ERROR = 0;
    public static final int UNKNOWN_ACCOUNT = 10001;
    public static final int UNKNOWN_CHANNEL = 10003;
    public static final int UNKNOWN_GUILD = 10004;
    public static final int UNKNOWN_MEMBER = 10007;
    public static final int UNKNOWN_MESSAGE = 10008;
    public static final int UNKNOWN_ROLE = 10011;
    public static final int UNKNOWN_USER = 10013;
    public static final int UNKNOWN_INTERACTION = 10062;
    public static final int MAXIMUM_NUMBER_OF_GUILDS = 30001;
    public static final int MISSING_ACCESS = 50001;
    public static final int CANNOT_SEND_TO_USER = 50007;
    public static final int MISSING_PERMISSIONS = 50013;
    public static final int INVALID_FORM_BODY = 50035;
    public static final int MESSAGE_TOO_OLD_TO_BULK_DELETE = 50034;
    public static final int INTERACTION_ALREADY_ACKNOWLEDGED = 40060;
    public static final int MISSING_ROLE_HIERARCHY = 50013;

    private DiscordErrorCode() {}
}
