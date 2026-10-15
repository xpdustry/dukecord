// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

/** An interaction that can be answered with a modal. */
public interface ModalCallback extends InteractionBehavior {

    default void showModal(final ModalCreateSpec modal) {
        this.duke().interactions().rest().showModal(this.id(), this.token(), modal);
    }
}
