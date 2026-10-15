// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GatewayOpcode {
    UNKNOWN(-1),
    DISPATCH(0),
    HEARTBEAT(1),
    IDENTIFY(2),
    STATUS_UPDATE(3),
    RESUME(6),
    RECONNECT(7),
    REQUEST_GUILD_MEMBERS(8),
    INVALID_SESSION(9),
    HELLO(10),
    HEARTBEAT_ACK(11),
    REQUEST_CHANNEL_INFO(43);

    private static final Map<Integer, GatewayOpcode> REVERSED;

    private final int code;

    static {
        REVERSED =
                Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(GatewayOpcode::code, Function.identity()));
    }

    GatewayOpcode(final int code) {
        this.code = code;
    }

    public static GatewayOpcode from(final int code) {
        return REVERSED.getOrDefault(code, UNKNOWN);
    }

    /**
     * Gets the actual numeric code.
     *
     * @return The actual numeric code.
     */
    public int code() {
        return code;
    }
}
