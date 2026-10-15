// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

/**
 * The Immutables style of every value type in Dukecord. It sits on the root package, and Immutables applies it to
 * subpackages that have no style of their own.
 *
 * <p>The generated {@code Immutable*} classes are package-private. A value type users can build declares a nested
 * {@code Builder} interface, which the generated builder implements, and a static {@code builder()} returning it.
 *
 * <p>Immutables picks a single style per type without merging, so a style on a type or a subpackage replaces this one
 * entirely. Add a variant as another meta-annotation here rather than a bare {@link Value.Style}. Types nested in a
 * {@link Value.Enclosing} type always take the enclosing type's style.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true,
        forceJacksonPropertyNames = false)
public @interface DukecordStyle {}
