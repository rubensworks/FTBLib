/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.feed_the_beast.ftbl.api.permissions.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IContext
{
    /**
     * @param key Context key
     * @return Context object
     * @see ContextKey
     */
    @Nullable
    <T> T get(@Nonnull ContextKey<T> key);

    /**
     * @param key Context key
     * @return true if context contains this key
     * @see ContextKey
     */
    boolean has(@Nonnull ContextKey<?> key);

    /**
     * Sets Context object
     *
     * @param key Context key
     * @param obj Context object. Can be null
     * @return itself, for easy context chaining
     * @see ContextKey
     */
    @Nonnull
    <T> IContext set(@Nonnull ContextKey<T> key, @Nullable T obj);
}