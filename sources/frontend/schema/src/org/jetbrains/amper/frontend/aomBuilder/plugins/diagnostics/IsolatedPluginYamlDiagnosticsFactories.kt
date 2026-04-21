/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.aomBuilder.plugins.diagnostics

import org.jetbrains.amper.frontend.tree.diagnoseUnknownProperties

/**
 * All [IsolatedPluginYamlDiagnosticsFactory]s.
 */
internal val IsolatedPluginYamlDiagnosticsFactories = listOf(
    IsolatedPluginYamlDiagnosticsFactory { diagnoseUnknownProperties(it) },
    MissingPropertiesDiagnosticFactory,
    NoTasksDiagnosticFactory,
    InvalidTaskNameReferencesDiagnosticFactory,
    ConstInitDiagnosticFactory,
    ConflictingCheckNamesDiagnosticFactory,
    ConflictingCommandNamesDiagnosticFactory,
)
