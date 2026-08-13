// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.http;

import java.time.Duration;

public interface RateLimiter {

    void sleep() throws InterruptedException;

    void reset();

    static RateLimiter ofExponential(final Duration base, final Duration max) {
        return new RateLimiter() {

            private Duration current = base;

            @Override
            public void sleep() throws InterruptedException {
                Thread.sleep(this.current);
                this.current = this.current.multipliedBy(2);
                if (this.current.compareTo(max) >= 0) {
                    this.current = max;
                }
            }

            @Override
            public void reset() {
                this.current = base;
            }
        };
    }
}
