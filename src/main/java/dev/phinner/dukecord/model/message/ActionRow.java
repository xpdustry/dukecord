// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Json;
import java.util.List;

/** A row of up to five buttons, or one select. */
public record ActionRow(List<Component> components) implements Component {

    public ActionRow {
        components = List.copyOf(components);
        if (components.isEmpty() || components.size() > 5) {
            throw new IllegalArgumentException("An action row holds 1 to 5 components");
        }
    }

    public static ActionRow of(final Component... components) {
        return new ActionRow(List.of(components));
    }

    @Override
    public int type() {
        return TYPE_ACTION_ROW;
    }

    @Override
    public Json toJson() {
        return Json.empty()
                .with("type", TYPE_ACTION_ROW)
                .with(
                        "components",
                        this.components.stream().map(Component::toJson).toList());
    }

    @Override
    public ActionRow asDisabled() {
        return new ActionRow(this.components.stream().map(Component::asDisabled).toList());
    }
}
