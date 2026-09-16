// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StringSelect(
        String customId,
        Optional<String> placeholder,
        int minValues,
        int maxValues,
        List<Option> options,
        boolean disabled)
        implements Component {

    public record Option(
            String label, String value, Optional<String> description, Optional<Emoji> emoji, boolean isDefault) {

        public static Option of(final String label, final String value) {
            return new Option(label, value, Optional.empty(), Optional.empty(), false);
        }

        public Option withDescription(final String description) {
            return new Option(this.label, this.value, Optional.of(description), this.emoji, this.isDefault);
        }

        public Option withEmoji(final Emoji emoji) {
            return new Option(this.label, this.value, this.description, Optional.of(emoji), this.isDefault);
        }

        public Option withDefault(final boolean isDefault) {
            return new Option(this.label, this.value, this.description, this.emoji, isDefault);
        }

        Json toJson() {
            var json = Json.empty()
                    .with("label", this.label)
                    .with("value", this.value)
                    .with("default", this.isDefault);
            if (this.description.isPresent()) {
                json = json.with("description", this.description.get());
            }
            if (this.emoji.isPresent()) {
                json = json.with("emoji", this.emoji.get().toJson());
            }
            return json;
        }
    }

    public StringSelect {
        options = List.copyOf(options);
    }

    public static Builder builder(final String customId) {
        return new Builder(customId);
    }

    public StringSelect withDisabled(final boolean disabled) {
        return new StringSelect(
                this.customId, this.placeholder, this.minValues, this.maxValues, this.options, disabled);
    }

    @Override
    public int type() {
        return TYPE_STRING_SELECT;
    }

    @Override
    public Json toJson() {
        var json = Json.empty()
                .with("type", TYPE_STRING_SELECT)
                .with("custom_id", this.customId)
                .with("min_values", this.minValues)
                .with("max_values", this.maxValues)
                .with("disabled", this.disabled)
                .with("options", this.options.stream().map(Option::toJson).toList());
        if (this.placeholder.isPresent()) {
            json = json.with("placeholder", this.placeholder.get());
        }
        return json;
    }

    @Override
    public StringSelect asDisabled() {
        return this.withDisabled(true);
    }

    public static final class Builder {

        private final String customId;
        private Optional<String> placeholder = Optional.empty();
        private int minValues = 1;
        private int maxValues = 1;
        private final List<Option> options = new ArrayList<>();

        private Builder(final String customId) {
            this.customId = customId;
        }

        public Builder placeholder(final String placeholder) {
            this.placeholder = Optional.of(placeholder);
            return this;
        }

        public Builder range(final int min, final int max) {
            this.minValues = min;
            this.maxValues = max;
            return this;
        }

        public Builder option(final Option option) {
            this.options.add(option);
            return this;
        }

        public Builder option(final String label, final String value) {
            return this.option(Option.of(label, value));
        }

        public StringSelect build() {
            return new StringSelect(
                    this.customId, this.placeholder, this.minValues, this.maxValues, this.options, false);
        }
    }
}
