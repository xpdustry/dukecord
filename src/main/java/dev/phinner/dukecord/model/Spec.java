// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Optional;

/**
 * A request body. Specs are immutable values built by a builder that writes a key only when its method is called, so
 * the payload carries Discord's tri-state: absent keeps, {@code null} clears, a value sets.
 */
public interface Spec {

    /** The JSON body as built. Consumers wrap it in their envelope and apply their allow-list. */
    Json payload();

    /** The audit-log reason, sent as the {@code X-Audit-Log-Reason} header. */
    Optional<String> reason();
}
