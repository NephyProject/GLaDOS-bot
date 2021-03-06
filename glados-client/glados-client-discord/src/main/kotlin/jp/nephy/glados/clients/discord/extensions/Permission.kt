/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("UNUSED")

package jp.nephy.glados.clients.discord.extensions

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.config
import jp.nephy.glados.clients.discord.config.config
import jp.nephy.glados.clients.discord.config.discord
import jp.nephy.glados.clients.discord.config.role
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

/**
 * Checks if this member has admin permission.
 */
val Member.hasAdminPermission: Boolean
    get() = isOwner || hasAdminRole || user.isGLaDOSOwner

/**
 * Checks if this user has admin permission.
 */
val User.hasAdminPermission: Boolean
    get() = isGLaDOSOwner

/**
 * Checks if this member has admin role.
 */
val Member.hasAdminRole: Boolean
    get() {
        val role = guild.config.role("admin") ?: return false
        return hasRole(role)
    }

/**
 * Checks if this user is GLaDOS owner.
 */
val User.isGLaDOSOwner: Boolean
    get() = idLong == GLaDOS.config.discord.ownerId
