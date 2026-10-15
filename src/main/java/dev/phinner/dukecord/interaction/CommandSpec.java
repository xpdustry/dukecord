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
        return builder().name(name).description(description).addOptions(options).build();
    }

    static CommandSpec user(final String name) {
        return builder().type(ApplicationCommand.Type.USER).name(name).build();
    }

    static CommandSpec message(final String name) {
        return builder().type(ApplicationCommand.Type.MESSAGE).name(name).build();
    }

    @Value.Default
    default ApplicationCommand.Type type() {
        return ApplicationCommand.Type.CHAT_INPUT;
    }

    /** Lowercase for slash commands. */
    String name();

    /** Required for slash commands, absent for the others. */
    Optional<String> description();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<CommandOption> options();

    /** Who may use the command unless a guild says otherwise. {@link PermissionSet#NONE} limits it to admins. */
    Optional<PermissionSet> defaultMemberPermissions();

    /** Where the command may be used. Every context when empty. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Set<InteractionContext> contexts();

    Optional<Boolean> nsfw();

    interface Builder {
        Builder from(CommandSpec instance);

        Builder type(ApplicationCommand.Type type);

        Builder name(String name);

        Builder description(String description);

        Builder description(Optional<String> description);

        Builder addOptions(CommandOption element);

        Builder addOptions(CommandOption... elements);

        Builder options(Iterable<? extends CommandOption> elements);

        Builder addAllOptions(Iterable<? extends CommandOption> elements);

        Builder defaultMemberPermissions(PermissionSet defaultMemberPermissions);

        Builder defaultMemberPermissions(Optional<? extends PermissionSet> defaultMemberPermissions);

        Builder addContexts(InteractionContext element);

        Builder addContexts(InteractionContext... elements);

        Builder contexts(Iterable<InteractionContext> elements);

        Builder addAllContexts(Iterable<InteractionContext> elements);

        Builder nsfw(boolean nsfw);

        Builder nsfw(Optional<Boolean> nsfw);

        CommandSpec build();
    }
}
