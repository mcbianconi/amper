/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Run given [testBody] respecting delays.
 * Overrides default behavior of [kotlinx.coroutines.test.runTest] that skips delays by default.
 */
fun runTestRespectingDelays(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration = 1.minutes,
    testBody: suspend TestScope.() -> Unit
) = runTest(context = context, timeout = timeout) {
    // wrap testBody into Default dispatcher, delays are respected this way
    withContext(Dispatchers.Default) {
        testBody()
    }
}
