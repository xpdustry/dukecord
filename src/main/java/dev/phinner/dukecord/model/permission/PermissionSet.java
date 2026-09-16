// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

/**
 * An immutable set of permission bits. Stored as a {@code long}: Discord is below bit 60 today; switch to
 * {@link java.math.BigInteger} only when it passes bit 62.
 */
public record PermissionSet(long value) {

    public static final PermissionSet NONE = new PermissionSet(0L);
    public static final PermissionSet ALL;

    static {
        long all = 0L;
        for (final var permission : Permission.values()) {
            all |= permission.mask();
        }
        ALL = new PermissionSet(all);
    }

    /** Parses the string form Discord uses in JSON. */
    public static PermissionSet fromString(final String value) {
        return new PermissionSet(Long.parseUnsignedLong(Objects.requireNonNull(value, "value")));
    }

    public static PermissionSet of(final Permission... permissions) {
        return NONE.with(permissions);
    }

    public static PermissionSet of(final Collection<Permission> permissions) {
        return NONE.with(permissions.toArray(Permission[]::new));
    }

    public boolean contains(final Permission permission) {
        return (this.value & permission.mask()) != 0L;
    }

    public boolean containsAll(final PermissionSet other) {
        return (this.value & other.value) == other.value;
    }

    public boolean containsAll(final Permission... permissions) {
        return this.containsAll(of(permissions));
    }

    public boolean containsAny(final PermissionSet other) {
        return (this.value & other.value) != 0L;
    }

    public boolean isEmpty() {
        return this.value == 0L;
    }

    public PermissionSet with(final Permission... permissions) {
        long result = this.value;
        for (final var permission : permissions) {
            result |= permission.mask();
        }
        return new PermissionSet(result);
    }

    public PermissionSet with(final PermissionSet other) {
        return new PermissionSet(this.value | other.value);
    }

    public PermissionSet without(final Permission... permissions) {
        long result = this.value;
        for (final var permission : permissions) {
            result &= ~permission.mask();
        }
        return new PermissionSet(result);
    }

    public PermissionSet without(final PermissionSet other) {
        return new PermissionSet(this.value & ~other.value);
    }

    public EnumSet<Permission> asSet() {
        final var set = EnumSet.noneOf(Permission.class);
        Arrays.stream(Permission.values()).filter(this::contains).forEach(set::add);
        return set;
    }

    /** The string form Discord expects in JSON. */
    public String asString() {
        return Long.toUnsignedString(this.value);
    }

    @Override
    public String toString() {
        return this.asSet().toString();
    }
}
