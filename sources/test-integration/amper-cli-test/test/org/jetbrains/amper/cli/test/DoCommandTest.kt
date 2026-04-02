/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.cli.test

import org.jetbrains.amper.cli.test.utils.assertStderrContains
import org.jetbrains.amper.cli.test.utils.assertStdoutContains
import org.jetbrains.amper.cli.test.utils.runSlowTest
import kotlin.test.Test

class DoCommandTest : AmperCliTestBase() {
    @Test
    fun `do command - unique name`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "my-plugin:uploadPictures",
            copyToTempDir = true,
        )
        result.assertStdoutContains("Uploading pictures...")
    }

    @Test
    fun `do command - ambiguous name`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "uploadPictures",
            copyToTempDir = true,
            expectedExitCode = 1,
            assertEmptyStdErr = false,
        )
        result.assertStderrContains("Ambiguous command name 'uploadPictures'")
        result.assertStderrContains("Please use a qualified name: my-plugin:uploadPictures, other-plugin:uploadPictures")
    }

    @Test
    fun `do command - qualified name`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "other-plugin:uploadPictures",
            copyToTempDir = true,
        )
        result.assertStdoutContains("Uploading other pictures...")
    }

    @Test
    fun `do command - multiple modules`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "my-plugin:uploadPictures",
            copyToTempDir = true,
        )
        // One for app, one for app2
        result.assertStdoutContains("Uploading pictures...")
        // Count occurrences of "Uploading pictures..."
        val occurrences = result.stdout.split("Uploading pictures...").size - 1
        kotlin.test.assertEquals(2, occurrences, "Should run for both app and app2")
    }

    @Test
    fun `do command - module selection`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "-m", "app", "my-plugin:uploadPictures",
            copyToTempDir = true,
        )
        result.assertStdoutContains("Uploading pictures...")
        val occurrences = result.stdout.split("Uploading pictures...").size - 1
        kotlin.test.assertEquals(1, occurrences, "Should only run for app")
    }
    @Test
    fun `do command - missing command`() = runSlowTest {
        val projectDir = testProject("extensibility/custom-commands")
        val result = runCli(
            projectDir = projectDir,
            "do", "nonExistent",
            copyToTempDir = true,
            expectedExitCode = 1,
            assertEmptyStdErr = false,
        )
        result.assertStderrContains("Unknown command 'nonExistent'")
    }
}
