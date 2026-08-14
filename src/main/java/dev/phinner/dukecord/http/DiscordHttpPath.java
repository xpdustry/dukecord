// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DiscordHttpPath {

    public static final DiscordHttpPath GATEWAY = new DiscordHttpPath(Method.GET, "gateway");

    private final Method method;
    private final List<PathSegment> path = new ArrayList<>();

    public DiscordHttpPath(final Method method, final String path) {
        this.method = Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        final var normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        final var segments = normalizedPath.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            var name = segments[i];
            if (name.isBlank()) {
                throw new IllegalArgumentException("Segment " + i + " of path " + path + " is blank");
            }
            var placeholder = false;
            if (name.startsWith("{")) {
                if (!name.endsWith("}")) {
                    throw new IllegalArgumentException("Invalid placeholder at segment " + i + " of path " + path);
                }
                placeholder = true;
                name = name.substring(1, name.length() - 1);
                if (name.isBlank()) {
                    throw new IllegalArgumentException(
                            "Placeholder at segment " + i + " of path " + path + " is blank");
                }
            }
            this.path.add(new PathSegment(name, placeholder));
        }
    }

    Method method() {
        return this.method;
    }

    List<PathSegment> segments() {
        return List.copyOf(this.path);
    }

    record PathSegment(String name, boolean placeholder) {}
}
