/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.plugins

import kotlinx.serialization.Serializable
import org.jetbrains.amper.frontend.messages.FileWithRangesBuildProblemSource
import org.jetbrains.amper.plugins.schema.model.PluginDataResponse
import org.jetbrains.amper.plugins.schema.model.PluginDataResponse.DiagnosticKind
import org.jetbrains.amper.problems.reporting.BuildProblem
import org.jetbrains.amper.problems.reporting.BuildProblemType
import org.jetbrains.amper.problems.reporting.DiagnosticId
import org.jetbrains.amper.problems.reporting.Level

@Serializable
class PluginSchemaBuildProblem(
    val diagnostic: PluginDataResponse.Diagnostic,
) : BuildProblem, DiagnosticId {
    override val source by lazy {
        FileWithRangesBuildProblemSource(diagnostic.location.path, diagnostic.location.textRange)
    }
    override val diagnosticId: DiagnosticId get() = this
    override val message get() = diagnostic.message
    override val level get() = when(diagnostic.kind) {
        DiagnosticKind.ErrorGeneric,
        DiagnosticKind.ErrorUnresolvedLikeConstruct -> Level.Error
        DiagnosticKind.WarningRedundant -> Level.WeakWarning
    }
    override val type get() = when(diagnostic.kind) {
        DiagnosticKind.ErrorGeneric -> BuildProblemType.Generic
        DiagnosticKind.ErrorUnresolvedLikeConstruct -> BuildProblemType.UnknownSymbol
        DiagnosticKind.WarningRedundant -> BuildProblemType.RedundantDeclaration
    }
}
