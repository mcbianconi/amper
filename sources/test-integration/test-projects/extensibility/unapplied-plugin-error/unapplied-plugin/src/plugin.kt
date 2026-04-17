/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.example
import org.jetbrains.amper.plugins.TaskAction

@TaskAction
fun myAction() {}

@TaskAction
fun myAction(param: Int) {}
