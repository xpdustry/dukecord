// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;
import java.util.List;
import java.util.Optional;

final class DmChannelImpl extends AbstractChannel implements DmChannel {

    private final List<User> recipients;
    private final Optional<DiscordEntityRef<Message>> lastMessage;

    DmChannelImpl(final Dukecord duke, final Json raw) {
        super(duke, raw);
        this.recipients = raw.objects("recipients").stream()
                .map(recipient -> Users.fromJson(duke, recipient))
                .toList();
        this.lastMessage = this.lastMessageRef();
    }

    @Override
    public List<User> recipients() {
        return this.recipients;
    }

    @Override
    public Optional<DiscordEntityRef<Message>> lastMessage() {
        return this.lastMessage;
    }
}
