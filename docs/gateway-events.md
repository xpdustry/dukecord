# Gateway event design

## Problem

The gateway transport previously published every Discord dispatch as mutable raw JSON. Core entities now deserialize through
a Dukecord-bound Jackson reader because their references need stores. The gateway itself must remain independent of a
particular client instance, and Discord's delete and unavailable payloads must not be presented as complete entities.

## Usage

Callers can subscribe to one concrete event or a family through the existing polymorphic dispatcher:

```java
duke.events().register(MessageCreateEvent.class, event -> consume(event.message()));
duke.events().register(GuildEvent.class, event -> audit(event.guildId()));
duke.events().register(UnknownDiscordEvent.class, event -> inspect(event.type(), event.payload()));
```

The eventual client implementation constructs its stores first, then creates a bound decoder and passes it into the gateway:

```java
var decoder = DiscordGatewayEvents.decoder(duke);
var gateway = new DiscordGatewayImpl(http, token, intents, timeout, decoder);
```

Construction must finish before `gateway.start()` publishes events.

## Shape

Public event interfaces live in `gateway.events`; Immutables generates their implementations. Family interfaces contain only
shared domain accessors. The dispatcher stores registrations under their exact requested type and walks the dispatched
event's interface hierarchy once, so concrete and family listeners each receive one call.

`DiscordDispatchDecoder` is the transport-to-domain seam. It accepts the event name and serialized `d` payload, which keeps
NanoJSON out of the public decoder contract. `DiscordGatewayEvents.decoder(duke)` only maps event names to annotated event
types and supplies the Jackson context required by entity references. Jackson and Immutables own payload conversion.

Complete payloads become existing entities. Delete events retain their documented IDs. An unavailable guild becomes
`GuildUnavailableEvent`, never an incomplete `Guild`. `MESSAGE_UPDATE` remains unknown until the model has an immutable patch
type that distinguishes an omitted field from a JSON null.

Guild, message, and user events use `@JsonUnwrapped` because Discord places the entity fields directly in `d`. Jackson cannot
unwrap the polymorphic `Channel` interface, so channel events decode through one shared deserializer that reads the flat
payload as `Channel` — reusing `Channel`'s own `type` polymorphism — and wraps the result in the requested event. The
Dukecord-bound reader context flows through the same deserialization context, so entity references inside the channel
deserialize normally.

Known decode failures throw `DiscordEventDecodingException`. The gateway catches that exception once, logs it, and publishes
the original payload as `UnknownDiscordEvent`; malformed payloads cannot terminate the websocket loop.

## Synthesis decision

Candidate B's injected decoder was the base because it keeps transport ownership separate from client-bound entity parsing.
Candidate A contributed the `gateway.events` package location and `UserUpdateEvent`. Both candidates' mutable message-patch
events were rejected. The cross-judge found and prompted the dispatcher registration fix; without it, a leaf registration
would receive sibling events and duplicate deliveries.

## Tradeoffs accepted

- The four-argument gateway constructor still emits only unknown events so transport-only use remains possible.
- Event decoding serializes NanoJSON's `d` object before Jackson reads it; this extra conversion keeps parser types private.
- Channel create, update, and delete decode through one handwritten deserializer because Jackson 3.1.4 can neither combine
  polymorphic type handling with `@JsonUnwrapped` nor resolve a polymorphic delegate for a class-level converter.
- Stores are not mutated here. Cache reduction needs write contracts and merge rules that do not exist yet.
- `UnknownDiscordEvent.payload()` is now an immutable JSON string instead of a mutable `JsonObject`.

## Alternatives considered

- Giving the gateway a `Dukecord` reference creates a construction cycle and mixes websocket lifecycle with model context.
- Duplicating entity fields directly onto event types avoids `@JsonUnwrapped`, but creates a second model that will drift from
  the canonical entities.
- Unwrapping the channel fields onto each event subtype was rejected: it duplicated the subtype table on every operation
  and multiplied five channel variants across create, update, and delete into fifteen private adapters.
- Typing `MESSAGE_UPDATE` as `Message` lies about required fields when Discord sends a partial update.

## Open questions and risks

- Should a future cache reducer run synchronously before event publication so listeners always observe committed state?
- Which immutable patch representation should model `MESSAGE_UPDATE` and other field-presence-sensitive dispatches?
- Should READY expose session details, or should those remain internal gateway state?

## Next implementation step

Add READY and a message patch only after their partial payload values have dedicated types.
