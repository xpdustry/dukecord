// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.support;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.cache.DiscordCache;
import dev.phinner.dukecord.gateway.DiscordEventDispatcher;
import dev.phinner.dukecord.gateway.DiscordEventSource;
import dev.phinner.dukecord.gateway.GatewayIntentSet;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.channel.Channels;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Guilds;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.MemberCompleteness;
import dev.phinner.dukecord.model.guild.Members;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.model.guild.Roles;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.message.Messages;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;
import dev.phinner.dukecord.rest.DiscordRest;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * An in-memory {@link Dukecord} for tests: a map-backed cache you populate with {@code put*} helpers, and a
 * {@link DiscordRest} proxy that records every call and answers from {@link #answer(String, Function)} stubs
 * (throwing {@link UnsupportedOperationException} otherwise).
 */
public final class TestDukecord implements Dukecord {

    /** One recorded REST call: the method name and its arguments. */
    public record RestCall(String method, List<Object> args) {}

    private final Map<Snowflake, Guild> guilds = new LinkedHashMap<>();
    private final Map<Snowflake, Channel> channels = new LinkedHashMap<>();
    private final Map<Snowflake, Role> roles = new LinkedHashMap<>();
    private final Map<MemberKey, Member> members = new LinkedHashMap<>();
    private final Map<Snowflake, User> users = new LinkedHashMap<>();
    private final Map<MessageKey, Message> messages = new LinkedHashMap<>();
    private final List<RestCall> restCalls = new ArrayList<>();
    private final Map<String, Function<List<Object>, Object>> restAnswers = new LinkedHashMap<>();
    private final DiscordEventDispatcher events = new DiscordEventDispatcher(Runnable::run);
    private final DiscordRest rest;
    private User self;
    private GatewayIntentSet intents = GatewayIntentSet.UN_PRIVILEGED;

    private record MemberKey(Snowflake guildId, Snowflake userId) {}

    private record MessageKey(Snowflake channelId, Snowflake messageId) {}

    public TestDukecord() {
        this.self = Users.fromJson(this, Json.parse("{\"id\":\"1\",\"username\":\"dukecord\",\"bot\":true}"));
        this.rest = (DiscordRest) Proxy.newProxyInstance(
                DiscordRest.class.getClassLoader(), new Class<?>[] {DiscordRest.class}, this::invokeRest);
    }

    private Object invokeRest(final Object proxy, final Method method, final Object[] args) {
        final List<Object> arguments = args == null ? List.of() : List.of(args);
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "RecordingRest";
                case "hashCode" -> System.identityHashCode(proxy);
                default -> proxy == arguments.getFirst();
            };
        }
        this.restCalls.add(new RestCall(method.getName(), arguments));
        final var answer = this.restAnswers.get(method.getName());
        if (answer != null) {
            return answer.apply(arguments);
        }
        if (method.getReturnType() == void.class) {
            return null;
        }
        throw new UnsupportedOperationException("No stubbed answer for DiscordRest." + method.getName());
    }

    // ---- setup ----

    public TestDukecord self(final User self) {
        this.self = self;
        this.users.put(self.id(), self);
        return this;
    }

    public TestDukecord intents(final GatewayIntentSet intents) {
        this.intents = intents;
        return this;
    }

    /** Stubs a REST method by name; the function receives the call arguments. */
    public TestDukecord answer(final String method, final Function<List<Object>, Object> answer) {
        this.restAnswers.put(method, answer);
        return this;
    }

    public List<RestCall> restCalls() {
        return List.copyOf(this.restCalls);
    }

    public Guild putGuild(final Json json) {
        return this.putGuild(json, true, MemberCompleteness.PARTIAL);
    }

    public Guild putGuild(final Json json, final boolean available, final MemberCompleteness completeness) {
        final var guild = Guilds.fromJson(this, json, available, completeness);
        this.guilds.put(guild.id(), guild);
        return guild;
    }

    public Channel putChannel(final Json json) {
        final var channel = Channels.fromJson(this, json);
        this.channels.put(channel.id(), channel);
        return channel;
    }

    public Role putRole(final Snowflake guildId, final Json json) {
        final var role = Roles.fromJson(this, guildId, json);
        this.roles.put(role.id(), role);
        return role;
    }

    public Member putMember(final Snowflake guildId, final Json json) {
        final var member = Members.fromJson(this, guildId, json);
        this.members.put(new MemberKey(guildId, member.id()), member);
        this.users.put(member.id(), member.user());
        return member;
    }

    public User putUser(final Json json) {
        final var user = Users.fromJson(this, json);
        this.users.put(user.id(), user);
        return user;
    }

    public Message putMessage(final Json json) {
        final var message = Messages.fromJson(this, json);
        this.messages.put(new MessageKey(message.channel().id(), message.id()), message);
        return message;
    }

    // ---- Dukecord ----

    @Override
    public User self() {
        return this.self;
    }

    @Override
    public GatewayIntentSet intents() {
        return this.intents;
    }

    @Override
    public DiscordCache cache() {
        return new DiscordCache() {
            @Override
            public Optional<Guild> guild(final Snowflake id) {
                return Optional.ofNullable(TestDukecord.this.guilds.get(id));
            }

            @Override
            public List<Guild> guilds() {
                return List.copyOf(TestDukecord.this.guilds.values());
            }

            @Override
            public Optional<Channel> channel(final Snowflake id) {
                return Optional.ofNullable(TestDukecord.this.channels.get(id));
            }

            @Override
            public Optional<Role> role(final Snowflake id) {
                return Optional.ofNullable(TestDukecord.this.roles.get(id));
            }

            @Override
            public Optional<Member> member(final Snowflake guildId, final Snowflake userId) {
                return Optional.ofNullable(TestDukecord.this.members.get(new MemberKey(guildId, userId)));
            }

            @Override
            public Optional<User> user(final Snowflake id) {
                return Optional.ofNullable(TestDukecord.this.users.get(id));
            }

            @Override
            public Optional<Message> message(final Snowflake channelId, final Snowflake messageId) {
                return Optional.ofNullable(TestDukecord.this.messages.get(new MessageKey(channelId, messageId)));
            }

            @Override
            public List<GuildChannel> channels(final Snowflake guildId) {
                return TestDukecord.this.channels.values().stream()
                        .filter(GuildChannel.class::isInstance)
                        .map(GuildChannel.class::cast)
                        .filter(channel -> channel.guild().id().equals(guildId))
                        .toList();
            }

            @Override
            public List<Role> roles(final Snowflake guildId) {
                return TestDukecord.this.roles.values().stream()
                        .filter(role -> role.guild().id().equals(guildId))
                        .toList();
            }

            @Override
            public List<Member> members(final Snowflake guildId) {
                return TestDukecord.this.members.entrySet().stream()
                        .filter(entry -> entry.getKey().guildId().equals(guildId))
                        .map(Map.Entry::getValue)
                        .toList();
            }
        };
    }

    @Override
    public DiscordRest rest() {
        return this.rest;
    }

    @Override
    public DiscordEventSource events() {
        return this.events;
    }

    @Override
    public void close() {}

    @Override
    public void close(final Duration timeout) {}
}
