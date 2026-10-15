// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.phinner.dukecord.guild.PermissionSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/** A command to register. Registering one with an existing name and type replaces it. */
@Value.Immutable
@JsonSerialize(as = ImmutableCommandSpec.class)
public interface CommandSpec {

    static Builder builder() {
        return ImmutableCommandSpec.builder();
    }

    static CommandSpec chatInput(final String name, final String description, final CommandOption... options) {
        return ImmutableCommandSpec.of(
                ApplicationCommand.Type.CHAT_INPUT, name, Optional.of(description), List.of(options));
    }

    static CommandSpec user(final String name) {
        return ImmutableCommandSpec.of(ApplicationCommand.Type.USER, name, Optional.empty(), List.of());
    }

    static CommandSpec message(final String name) {
        return ImmutableCommandSpec.of(ApplicationCommand.Type.MESSAGE, name, Optional.empty(), List.of());
    }

    @Value.Default
    @Value.Parameter
    default ApplicationCommand.Type type() {
        return ApplicationCommand.Type.CHAT_INPUT;
    }

    /** Lowercase for slash commands. */
    @Value.Parameter
    String name();

    /** Required for slash commands, absent for the others. */
    @Value.Parameter
    Optional<String> description();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Value.Parameter
    List<CommandOption> options();

    /** Who may use the command unless a guild says otherwise. {@link PermissionSet#NONE} limits it to admins. */
    Optional<PermissionSet> defaultMemberPermissions();

    /** Where the command may be used. Every context when empty. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Set<InteractionContext> contexts();

    Optional<Boolean> nsfw();

    interface Builder {
        Builder from(final CommandSpec instance);

        Builder type(final ApplicationCommand.Type type);

        Builder name(final String name);

        Builder description(final String description);

        Builder addOptions(final CommandOption element);

        Builder addOptions(final CommandOption... elements);

        Builder options(final Iterable<? extends CommandOption> elements);

        Builder addAllOptions(final Iterable<? extends CommandOption> elements);

        Builder defaultMemberPermissions(final PermissionSet defaultMemberPermissions);

        Builder addContexts(final InteractionContext element);

        Builder addContexts(final InteractionContext... elements);

        Builder contexts(final Iterable<InteractionContext> elements);

        Builder addAllContexts(final Iterable<InteractionContext> elements);

        Builder nsfw(final boolean nsfw);

        CommandSpec build();
    }
}
