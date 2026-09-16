# Discord API feature set from real bots

This is a build brief for Dukecord. It records the Discord functions used by three codebases that matter here, not a list of everything Discord offers.

The sources were checked locally on 2026-08-15 through BTCA:

- [xpdustry/imperium at `6f4d195`](https://github.com/xpdustry/imperium/tree/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb)
- [xpdustry/nucleus at `8531849`](https://github.com/xpdustry/nucleus/tree/85318495a754f69c21f5eaa4b99272b4648986c7)
- [phinner/mad at `0af4e98`](https://github.com/phinner/mad/tree/0af4e9826cca31f447a662ab09cb8038312efeac)

## What to build first

### Login, lifecycle, and gateway events

All three bots need a gateway client which starts predictably and can be shut down cleanly. Imperium waits for ready before its application starts, configures message, member, reaction, direct-message, and expression intents, and caches all guild members. Nucleus does the same in Javacord. MAD uses a hosted gateway client and registers commands when it receives `READY`.

Provide:

- `DiscordClient.login(token, options)` that blocks until ready or throws a useful exception.
- `close()` with a bounded, graceful gateway shutdown.
- intent configuration, including Message Content and Guild Members.
- typed, cancellable subscriptions for message create, channel create, thread revealed, slash command, button/select, and modal interactions.
- callbacks dispatched off the gateway thread.
- a small cache for the current user, guilds, members, roles, and channels. Cached lookup and REST fetch must have different method names.

Evidence: [Imperium connection and thread subscriptions](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt#L55-L100), [Nucleus connection](https://github.com/xpdustry/nucleus/blob/85318495a754f69c21f5eaa4b99272b4648986c7/nucleus-discord/src/main/java/fr/xpdustry/nucleus/discord/service/SimpleDiscordService.java#L43-L67), and [MAD ready-time registration](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Discord/DiscordHostedService.cs#L20-L73).

### Guild administration and topology

These are single or few-guild operations bots. They resolve configured IDs from cache, inspect members and roles, create text channels under a category, and make thread channels. Imperium also has to join existing, newly created, and revealed threads to receive their messages. That awkward Discord rule should be hidden behind an opt-in `joinThreads` client option or a clear helper.

Provide:

- guild, member, role, category, text-channel, private-channel, and thread entities.
- cache lookup by snowflake plus explicit REST retrieval where Discord permits it.
- member role list and add/remove role operations.
- create a text channel within a category.
- create and join thread channels. Deliver thread message events once joined.
- open a direct-message channel with a user.

Evidence: [Imperium role synchronization](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt#L104-L169), [live-chat channel creation](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/bridge/MindustryBridgeListener.kt#L63-L92), and [thread joining](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/service/DiscordService.kt#L68-L85).

### Messages, files, embeds, and mention safety

Sending messages is the highest-frequency operation. Imperium bridges game chat into Discord, explicitly suppresses allowed mentions, edits messages, disables expired components, reads attachments, and sends map files, previews, and text exports. MAD sends Component V2 message layouts with mentions disabled.

Provide:

- blocking `sendMessage`, `edit`, `delete`, and `fetchMessage` methods that return typed messages.
- message creation and edit specs with content, embeds, components, attachments, and an explicit `AllowedMentions` value. Safe defaults should allow no mentions.
- embed fields, title, description, color, author/footer, image, and thumbnail.
- file upload from bytes, `Path`, and an `InputStream` supplier. Attachment metadata and a blocking download stream are required for map submission.
- preserve or replace existing attachments deliberately when editing. Imperium needs to re-upload attachments while changing embeds, so this cannot be an accidental side effect.
- paginated REST message retrieval and bulk deletion of up to 100 messages. Discord rejects bulk deletion of messages older than 14 days, so expose that limit in validation or a precise error.

Evidence: [safe bridge sends](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/bridge/MindustryBridgeListener.kt#L48-L75), [interactive message editing and retrieval](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSearchCommand.kt#L94-L171), [Imperium file/attachment uses](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSubmitCommand.kt#L56-L118), and [MAD pagination and bulk deletion](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Delete/MessageDeletionService.cs#L18-L99).

### Application commands and interactions

Slash commands are core, not a later convenience. Nucleus and Imperium both built command dispatchers on top of their Discord libraries. MAD registers global commands in production and guild commands while debugging. It then runs slash commands, subcommands, buttons, and component custom-ID handlers with permission checks and themed error responses.

Provide a small command module, separate from the core HTTP and gateway client:

- application-command definitions with slash commands, subcommands, string, integer, boolean, user, channel, and attachment options.
- bulk registration for global and guild commands. A guild-only mode makes development bearable.
- interaction objects with user, guild/channel, resolved option values, application permissions, and custom ID.
- `reply`, ephemeral `reply`, `deferReply`, `editOriginal`, `deferEdit`, `updateMessage`, and follow-up message operations. The module must know whether the interaction has already been acknowledged.
- button and string-select components, including disabled state, defaults, ranges, and custom IDs.
- custom-ID routing plus a way for applications to own per-message state and expiry. On expiry, editing a message to disable its controls should be easy.
- modal definitions and modal-submit events. Imperium has modal dispatch support, so leaving this out would strand an existing use case.
- permission predicates based on the invoking user and the bot's channel permissions. Do not make this a giant authorization framework.

Evidence: [Imperium's stateful map search](https://github.com/xpdustry/imperium/blob/6f4d195bd4889e9b3e4a1c50f276d1a056aa78cb/imperium-backend/src/main/kotlin/com/xpdustry/imperium/backend/commands/MapSearchCommand.kt#L48-L159), [Nucleus command registration and dispatch](https://github.com/xpdustry/nucleus/blob/85318495a754f69c21f5eaa4b99272b4648986c7/nucleus-discord/src/main/java/fr/xpdustry/nucleus/discord/interaction/SimpleInteractionManager.java#L47-L185), [MAD slash/button modules](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Delete/AutoDeleteInteractionModule.cs#L12-L192), and [MAD's error response logic](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Discord/MadInteractionHandlers.cs#L82-L136).

### Component V2 messages

MAD makes real use of Discord's Components V2 layout: text displays, sections, thumbnails, separators, action rows, link buttons, ephemeral error messages, and the `IsComponentsV2` flag. This should be supported in message and interaction-reply specs. It is not a substitute for embeds because Imperium uses embeds heavily.

Evidence: [MAD help layout](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Help/HelpInteractionModule.cs#L12-L67) and [Component V2 log send](https://github.com/phinner/mad/blob/0af4e9826cca31f447a662ab09cb8038312efeac/Mad.Application/Log/LogNotifier.cs#L80-L95).

## Delivery order

1. REST client, models, errors, message specs, file uploads, and webhook-less message CRUD.
2. Gateway login, cache, lifecycle, and typed message/channel/thread events.
3. Guild/member/role/channel/DM/thread operations.
4. Slash commands, acknowledgement lifecycle, buttons, selects, and Component V2 specs.
5. Attachments/downloads, paginated retrieval, bulk delete, modals, and command registration helpers.

The first four stages cover the day-to-day work in all three bots. The fifth covers real Imperium and MAD workloads, but can land after basic bots are usable.

## Deliberately out of scope for now

There is no evidence in these repositories that Dukecord needs voice, audio playback, stage channels, polls, scheduled events, automod configuration, or a general DI/command framework. Webhooks appear only incidentally in message filtering, so a dedicated webhook client can wait unless another bot requires it.

## API shape to protect

The shared complaint encoded in Imperium and Nucleus is asynchronous library plumbing leaking into ordinary bot code. Dukecord should keep the public Java path blocking and typed:

```java
try (DiscordClient discord = DiscordClient.login(token, options)) {
    TextChannel channel = discord.requireGuild(guildId).requireTextChannel(channelId);
    Message message = channel.send(MessageCreate.builder()
        .content("Server started")
        .allowedMentions(AllowedMentions.none())
        .build());

    discord.on(ButtonInteraction.class, event -> {
        if (event.customId().equals("maps:next")) {
            event.deferEdit().editOriginal(updatedPage);
        }
    });
}
```

Internally async I/O is fine. It should not force `CompletableFuture`, Reactor, RxJava, or library-specific action objects into bot handlers.
