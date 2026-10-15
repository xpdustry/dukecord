// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;

/** A set of permission bits. Bits Dukecord does not know yet are kept. */
public record PermissionSet(long value) {

    public static final PermissionSet NONE = new PermissionSet(0);

    /** Discord sends permissions as a decimal string. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PermissionSet of(final String value) {
        return new PermissionSet(Long.parseUnsignedLong(value));
    }

    public static PermissionSet of(final Permission... permissions) {
        return NONE.with(permissions);
    }

    public boolean contains(final Permission permission) {
        return (this.value & permission.mask()) != 0;
    }

    public boolean containsAll(final PermissionSet other) {
        return (this.value & other.value) == other.value;
    }

    public PermissionSet with(final Permission... permissions) {
        var value = this.value;
        for (final var permission : permissions) {
            value |= permission.mask();
        }
        return new PermissionSet(value);
    }

    public PermissionSet without(final Permission... permissions) {
        var value = this.value;
        for (final var permission : permissions) {
            value &= ~permission.mask();
        }
        return new PermissionSet(value);
    }

    public EnumSet<Permission> asEnumSet() {
        final var set = EnumSet.noneOf(Permission.class);
        for (final var permission : Permission.values()) {
            if (this.contains(permission)) {
                set.add(permission);
            }
        }
        return set;
    }

    @JsonValue
    public String asString() {
        return Long.toUnsignedString(this.value);
    }

    @Override
    public String toString() {
        return this.asEnumSet().toString();
    }
}
