// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;

/** A component type this library does not model yet; round-trips untouched. */
public record UnknownComponent(Json raw) implements Component {

    @Override
    public int type() {
        return this.raw.optionalInteger("type").orElse(-1);
    }

    @Override
    public Json toJson() {
        return this.raw;
    }

    @Override
    public UnknownComponent asDisabled() {
        return this;
    }
}
