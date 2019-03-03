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

package jp.nephy.glados.api

interface Logger {
    companion object
    
    val isTraceEnabled: Boolean
    fun trace(message: () -> Any?)
    fun trace(throwable: Throwable, message: () -> Any?)
    
    val isDebugEnabled: Boolean
    fun debug(message: () -> Any?)
    fun debug(throwable: Throwable, message: () -> Any?)
    
    val isInfoEnabled: Boolean
    fun info(message: () -> Any?)
    fun info(throwable: Throwable, message: () -> Any?)
    
    val isWarnEnabled: Boolean
    fun warn(message: () -> Any?)
    fun warn(throwable: Throwable, message: () -> Any?)
    
    val isErrorEnabled: Boolean
    fun error(message: () -> Any?)
    fun error(throwable: Throwable, message: () -> Any?)
}
