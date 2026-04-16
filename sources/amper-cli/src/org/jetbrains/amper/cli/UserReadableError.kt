/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.cli

/**
 * Exception that can be directly reported to the user without a stacktrace
 */
class UserReadableError(override val message: String, val exitCode: Int): RuntimeException(message)

/**
 * Prints the given error [message] to the user and immediately exits with the specified [exitCode].
 *
 * Markdown is not supported in the message (it messes up the red formatting and might not be possible if the state of
 * the program is critically broken).
 */
fun userReadableError(message: String, exitCode: Int = 1): Nothing = throw UserReadableError(message, exitCode)
